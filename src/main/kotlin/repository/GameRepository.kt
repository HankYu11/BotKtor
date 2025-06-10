package com.hank.repository

import com.hank.db.GameEntity
import com.hank.model.domain.Game
import com.hank.model.domain.toGameData
import org.jetbrains.exposed.sql.transactions.transaction

class GameRepository {

    // Create a new game
    // If GameEntity had properties like 'name', you'd pass them as parameters
    fun create(): Game = transaction {
        GameEntity.new {
        }.toGameData()
    }

    // Read a game by ID
    fun findById(id: Int): Game? = transaction {
        GameEntity.findById(id)?.toGameData()
    }

    fun existsById(id: Int): Boolean = transaction {
        GameEntity.findById(id) != null
    }


    // Read all games
    fun findAll(): List<Game> = transaction {
        GameEntity.all().map { it.toGameData() }
    }

    // Update a game (example, if Game had a 'name' property)
    /*
    fun update(id: Int, newName: String): GameData? = transaction {
        GameEntity.findById(id)?.apply {
            this.name = newName
        }?.toGameData()
    }
    */

    // Delete a game by ID
    fun delete(id: Int): Boolean = transaction {
        GameEntity.findById(id)?.delete()
        true // Or return true if findById was not null, false otherwise
    }
}
