package com.hank.repository

import com.hank.db.GameEntity
import com.hank.db.RoundEntity
import com.hank.db.Rounds
import com.hank.model.domain.Round
import com.hank.model.domain.toRoundData
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

class RoundRepository {
    suspend fun create(bet: Int, gameId: Int): Round? = newSuspendedTransaction {
        val game = GameEntity.findById(gameId) ?: return@newSuspendedTransaction null // Ensure game exists

        RoundEntity.new {
            this.bet = bet
            this.game = game
        }.toRoundData()
    }

    suspend fun findByGameId(gameId: Int): List<Round> = newSuspendedTransaction {
        RoundEntity.find { Rounds.gameId eq gameId }.map { it.toRoundData() }
    }
}