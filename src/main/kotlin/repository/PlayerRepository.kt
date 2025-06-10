package com.hank.repository

import com.hank.db.GameEntity
import com.hank.db.PlayerEntity
import com.hank.db.Players
import com.hank.model.domain.Player
import com.hank.model.domain.toPlayerData
import org.jetbrains.exposed.sql.transactions.transaction

class PlayerRepository {

    fun create(name: String, balance: Int, gameId: Int): Player? = transaction {
        val game = GameEntity.findById(gameId) ?: return@transaction null // Ensure game exists
        PlayerEntity.new {
            this.name = name
            this.balance = balance
            this.game = game
        }.toPlayerData()
    }

    fun findById(id: Int): Player? = transaction {
        PlayerEntity.findById(id)?.toPlayerData()
    }

    fun findByGameId(gameId: Int): List<Player> = transaction {
        PlayerEntity.find { Players.gameId eq gameId }.map { it.toPlayerData() }
    }

    fun findAll(): List<Player> = transaction {
        PlayerEntity.all().map { it.toPlayerData() }
    }

    fun updateBalance(id: Int, balance: Int): Player? = transaction {
        val player = PlayerEntity.findById(id) ?: return@transaction null
        player.apply {
            this.balance = balance
        }.toPlayerData()
    }

    fun delete(id: Int): Boolean = transaction {
        PlayerEntity.findById(id)?.delete() != null
    }
}
