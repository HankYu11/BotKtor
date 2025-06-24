package com.hank

import com.hank.db.GameEntity
import com.hank.GameUpdateManager
import com.hank.model.domain.Player
import com.hank.model.domain.Result
import com.hank.model.domain.RoundWithResults
import com.hank.model.request.CreateGameRequest
import com.hank.model.request.CreateRoundRequest
import com.hank.model.response.GameDetails
import com.hank.model.response.GameWithPlayers
import com.hank.model.response.RoundDetails
import com.hank.repository.GameRepository
import com.hank.repository.PlayerRepository
import com.hank.repository.ResultRepository
import com.hank.repository.RoundRepository
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sse.SSE
import io.ktor.server.sse.heartbeat
import io.ktor.server.sse.sse
import io.ktor.sse.ServerSentEvent
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json()
    }
    install(SSE)

    val gameRepository = GameRepository()
    val playerRepository = PlayerRepository()
    val roundRepository = RoundRepository()
    val resultRepository = ResultRepository()

    // Helper function to fetch full game details.
    suspend fun getGameDetails(gameId: Int): GameDetails? {
        val game = gameRepository.findById(gameId) ?: return null
        val players = playerRepository.findByGameId(gameId)
        val rounds = roundRepository.findByGameId(gameId)
        val roundIds = rounds.map { it.id }
        val allResultsForGame: List<Result> = if (roundIds.isNotEmpty()) {
            resultRepository.findByRoundIds(roundIds)
        } else {
            emptyList()
        }
        val resultsByRoundId: Map<Int, List<Result>> = allResultsForGame.groupBy { it.roundId }
        val roundWithResultsList: List<RoundWithResults> = rounds.map { round ->
            RoundWithResults(
                roundId = round.id,
                bet = round.bet,
                results = resultsByRoundId[round.id] ?: emptyList()
            )
        }
        return GameDetails(
            game = game,
            players = players,
            roundWithResults = roundWithResultsList,
        )
    }

    routing {
        route("/game") {
            post("/create") {
                val request = call.receive<CreateGameRequest>()

                if (request.playerNames.size != 4) {
                    call.respond(HttpStatusCode.BadRequest, "Exactly four player names are required.")
                    return@post
                }

                // Create a new game
                val game = gameRepository.create()

                // Create players for the game
                val players = request.playerNames.mapNotNull { playerName ->
                    playerRepository.create(name = playerName, balance = 0, gameId = game.id)
                }

                if (players.size == 4) {
                    call.respond(HttpStatusCode.Created, GameWithPlayers(game, players))
                } else {
                    // This case might indicate an issue with player creation,
                    // though create() in PlayerRepository should ideally handle errors gracefully.
                    // For simplicity, we'll assume player creation is successful if game creation was.
                    // A more robust solution would involve transactions and error handling for player creation.
                    call.respond(HttpStatusCode.InternalServerError, "Failed to create all players.")
                }
            }

            head("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()

                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest)
                    return@head
                }

                if (gameRepository.existsById(id)) {
                    call.respond(HttpStatusCode.OK)
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }

            get("/{id}") {
                val id = call.parameters["id"]?.toIntOrNull()
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest, "Game ID must be a number")
                    return@get
                }

                val game = gameRepository.findById(id)
                if (game == null) {
                    call.respond(HttpStatusCode.NotFound, "Game with ID $id not found")
                    return@get
                }

                val players = playerRepository.findByGameId(id)
                val rounds = roundRepository.findByGameId(id)
                val roundIds = rounds.map { it.id }
                val allResultsForGame: List<Result> = if (roundIds.isNotEmpty()) {
                    resultRepository.findByRoundIds(roundIds) // Using the new method
                } else {
                    emptyList()
                }

                val resultsByRoundId: Map<Int, List<Result>> = allResultsForGame.groupBy { it.roundId }

                val roundWithResultsList: List<RoundWithResults> = rounds.map { round ->
                    RoundWithResults(
                        roundId = round.id,
                        bet = round.bet,
                        results = resultsByRoundId[round.id] ?: emptyList()
                    )
                }

                val gameDetails = GameDetails(
                    game = game,
                    players = players,
                    roundWithResults = roundWithResultsList,
                )

                call.respond(HttpStatusCode.OK, gameDetails)
            }

            sse("/{id}/sse", serialize = { typeInfo, it ->
                val serializer = Json.serializersModule.serializer(typeInfo.kotlinType!!)
                Json.encodeToString(serializer, it)
            }) {
                heartbeat {
                    period = 45.seconds
                    event = ServerSentEvent("heartbeat")
                }

                val gameId = call.parameters["id"]?.toIntOrNull()
                if (gameId == null) {
                    call.respond(HttpStatusCode.BadRequest, "Game ID must be a number.")
                    return@sse
                }

                if (!gameRepository.existsById(gameId)) {
                    call.respond(HttpStatusCode.NotFound, "Game with ID $gameId not found.")
                    return@sse
                }

                try {
                    // Send the initial state once upon connection.
                    getGameDetails(gameId)?.let {
                        val initialStateJson = Json.encodeToString(GameDetails.serializer(), it)
                        send(ServerSentEvent(data = initialStateJson, event = "update"))
                    }

                    // Subscribe to the update flow and send events to the client.
                    GameUpdateManager.getUpdatesFlow(gameId).collect { gameDetails ->
                        val updateJson = Json.encodeToString(GameDetails.serializer(), gameDetails)
                        send(ServerSentEvent(data = updateJson, event = "update"))
                    }
                } finally {
                    // This block will be executed when the client disconnects.
                    println("Client disconnected from game $gameId observation.")
                }
            }
        }

        route("/round") {
            post("/create") {
                val request = call.receive<CreateRoundRequest>()
                if (request.results.size != 4) {
                    call.respond(HttpStatusCode.BadRequest, "Exactly four player results are required.")
                    return@post
                }

                val gameId = request.gameId

                val createdRoundDetails: RoundDetails? = newSuspendedTransaction {
                    // 1. Validate Game
                    val gameEntity = GameEntity.findById(gameId)
                    if (gameEntity == null) {
                        call.application.environment.log.warn("Game with ID $gameId not found during round creation.")
                        return@newSuspendedTransaction null // Will lead to 404 or other error outside
                    }

                    // 2. Create Round
                    val newRound = roundRepository.create(bet = request.bet, gameId = gameId)
                    if (newRound == null) {
                        call.application.environment.log.error("Failed to create round for game ID $gameId.")
                        return@newSuspendedTransaction null
                    }

                    val createdResults = mutableListOf<Result>()
                    val updatedPlayers = mutableListOf<Player>()

                    // 3. Create Results and Update Player Balances
                    for (playerResultRequest in request.results) {
                        val player = playerRepository.findById(playerResultRequest.playerId)
                        if (player == null) {
                            call.application.environment.log.warn("Player with ID ${playerResultRequest.playerId} not found.")
                            throw IllegalStateException("Player with ID ${playerResultRequest.playerId} not found.")
                        }

                        playerRepository.updateBalance(player.id, player.balance + (playerResultRequest.profit))?.let {
                            updatedPlayers.add(it)
                        }

                        // Create Result
                        val result = resultRepository.create(
                            profit = playerResultRequest.profit,
                            roundId = newRound.id,
                            playerId = playerResultRequest.playerId
                        )
                        if (result == null) {
                            call.application.environment.log.error("Failed to create result for round ${newRound.id} and player ${playerResultRequest.playerId}.")
                            throw IllegalStateException("Failed to create result for player ${playerResultRequest.playerId}.")
                        }
                        createdResults.add(result)
                    }

                    RoundDetails(round = newRound, players = updatedPlayers, results = createdResults)
                }

                if (createdRoundDetails != null) {
                    // After a successful update, fetch the latest game state.
                    val updatedGameDetails = getGameDetails(gameId)
                    if (updatedGameDetails != null) {
                        GameUpdateManager.notifyUpdate(gameId, updatedGameDetails)
                    }
                    call.respond(HttpStatusCode.Created, createdRoundDetails)
                } else {
                    call.respond(HttpStatusCode.InternalServerError, "Failed to create round and results. Check game ID or player IDs.")
                }
            }
        }
        get("/") {
            call.respondText("Hello World!")
        }
    }
}

