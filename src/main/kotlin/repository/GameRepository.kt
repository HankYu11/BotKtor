package com.hank.repository

import com.hank.db.GameEntity
import com.hank.model.domain.Game
import com.hank.model.domain.toGameData
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

class GameRepository {

    suspend fun create(): Game = newSuspendedTransaction {
        GameEntity.new {
        }.toGameData()
    }

    suspend fun findById(id: Int): Game? = newSuspendedTransaction {
        GameEntity.findById(id)?.toGameData()
    }

    suspend fun existsById(id: Int): Boolean = newSuspendedTransaction {
        GameEntity.findById(id) != null
    }
}
