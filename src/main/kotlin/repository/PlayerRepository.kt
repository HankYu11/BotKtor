package com.hank.repository

import com.hank.db.GameEntity
import com.hank.db.PlayerEntity
import com.hank.db.Players
import com.hank.model.domain.Player
import com.hank.model.domain.toPlayerData
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

class PlayerRepository {

    suspend fun create(name: String, balance: Int, gameId: Int): Player? = newSuspendedTransaction {
        val game = GameEntity.findById(gameId) ?: return@newSuspendedTransaction null // Ensure game exists

        PlayerEntity.new {
            this.name = name
            this.balance = balance
            this.game = game
        }.toPlayerData()
    }

    suspend fun findById(id: Int): Player? = newSuspendedTransaction {
        PlayerEntity.findById(id)?.toPlayerData()
    }

    suspend fun findByGameId(gameId: Int): List<Player> = newSuspendedTransaction {
        PlayerEntity.find { Players.gameId eq gameId }.map { it.toPlayerData() }
    }

    suspend fun updateBalance(id: Int, balance: Int): Player? = newSuspendedTransaction {
        val player = PlayerEntity.findById(id) ?: return@newSuspendedTransaction null

        player.apply {
            this.balance = balance
        }.toPlayerData()
    }
}
