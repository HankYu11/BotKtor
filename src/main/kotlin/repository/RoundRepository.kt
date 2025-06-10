package com.hank.repository

import com.hank.db.GameEntity
import com.hank.db.RoundEntity
import com.hank.db.Rounds
import com.hank.model.domain.Round
import com.hank.model.domain.toRoundData
import org.jetbrains.exposed.sql.transactions.transaction

class RoundRepository {

    fun create(bet: Int, gameId: Int): Round? = transaction {
        val game = GameEntity.findById(gameId) ?: return@transaction null // Ensure game exists
        RoundEntity.new {
            this.bet = bet
            this.game = game
        }.toRoundData()
    }

    fun findById(id: Int): Round? = transaction {
        RoundEntity.findById(id)?.toRoundData()
    }

    fun findAll(): List<Round> = transaction {
        RoundEntity.all().map { it.toRoundData() }
    }

    fun findByGameId(gameId: Int): List<Round> = transaction {
        RoundEntity.find { Rounds.gameId eq gameId }.map { it.toRoundData() }
    }

    fun update(id: Int, bet: Int?, gameId: Int?): Round? = transaction {
        val round = RoundEntity.findById(id) ?: return@transaction null
        round.apply {
            bet?.let { this.bet = it }
            gameId?.let { newGameId ->
                GameEntity.findById(newGameId)?.let { this.game = it }
            }
        }.toRoundData()
    }

    fun delete(id: Int): Boolean = transaction {
        RoundEntity.findById(id)?.delete() != null
    }
}