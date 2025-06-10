package com.hank

import com.hank.db.GameEntity
import com.hank.db.PlayerEntity
import com.hank.model.domain.Player
import com.hank.model.domain.Result
import com.hank.model.domain.Round
import com.hank.model.domain.RoundWithResults
import com.hank.model.request.CreateGameRequest
import com.hank.model.request.CreateRoundRequest
import com.hank.model.response.GameDetails
import com.hank.repository.GameRepository
import com.hank.repository.PlayerRepository
import com.hank.repository.ResultRepository
import com.hank.repository.RoundRepository
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.sql.Connection
import java.sql.DriverManager
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json()
    }

    val gameRepository = GameRepository()
    val playerRepository = PlayerRepository()
    val roundRepository = RoundRepository()
    val resultRepository = ResultRepository()

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
                    call.respond(HttpStatusCode.Created, "Game and players created successfully")
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
        }

        route("/round") {
            post("/create") {
                val request = call.receive<CreateRoundRequest>()
                // Basic Validations
                if (request.results.size != 4) {
                    call.respond(HttpStatusCode.BadRequest, "Exactly four player results are required.")
                    return@post
                }

                val gameId = request.gameId

                // Perform database operations within a transaction
                val createdRoundDetails: Round? = transaction {
                    // 1. Validate Game
                    val gameEntity = GameEntity.findById(gameId)
                    if (gameEntity == null) {
                        call.application.environment.log.warn("Game with ID $gameId not found during round creation.")
                        return@transaction null // Will lead to 404 or other error outside
                    }

                    // 2. Create Round
                    val newRound = roundRepository.create(bet = request.bet, gameId = gameId)
                    if (newRound == null) {
                        call.application.environment.log.error("Failed to create round for game ID $gameId.")
                        return@transaction null // Will lead to 500 or other error outside
                    }

                    val createdResults = mutableListOf<Result>()

                    // 3. Create Results and Update Player Balances
                    for (playerResultRequest in request.results) {
                        val player = playerRepository.findById(playerResultRequest.playerId)
                        if (player == null) {
                            call.application.environment.log.warn("Player with ID ${playerResultRequest.playerId} not found.")
                            // Rollback transaction by throwing an exception or returning null
                            // to indicate failure.
                            throw IllegalStateException("Player with ID ${playerResultRequest.playerId} not found.")
                        }

                        // Update player balance
                        // Assuming PlayerEntity has a 'balance' property.
                        // Profit can be positive or negative.
                        playerRepository.updateBalance(player.id, player.balance + (playerResultRequest.profit))

                        // Create Result
                        val result = resultRepository.create(
                            profit = playerResultRequest.profit,
                            roundId = newRound.id,
                            playerId = playerResultRequest.playerId
                        )
                        if (result == null) {
                            call.application.environment.log.error("Failed to create result for round ${newRound.id} and player ${playerResultRequest.playerId}.")
                            // Rollback transaction
                            throw IllegalStateException("Failed to create result for player ${playerResultRequest.playerId}.")
                        }
                        createdResults.add(result)
                    }
                    // If all operations were successful, the transaction will commit.
                    newRound // Return the created round domain object
                }

                if (createdRoundDetails != null) {
                    // You might want to return the full RoundWithResults, or just the created Round, or just a success message.
                    // For now, returning the created Round object.
                    call.respond(HttpStatusCode.Created, createdRoundDetails)
                } else {
                    // Infer error based on logs or specific checks if needed
                    // This generic error can be improved if more specific states are returned from the transaction block
                    call.respond(HttpStatusCode.InternalServerError, "Failed to create round and results. Check game ID or player IDs.")
                }
            }
        }
    }
}