package com.hank.repository

import com.hank.db.PlayerEntity
import com.hank.db.ResultEntity
import com.hank.db.Results
import com.hank.db.RoundEntity
import com.hank.model.domain.Result
import com.hank.model.domain.toResultData
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

class ResultRepository {

    suspend fun create(profit: Int, roundId: Int, playerId: Int): Result? = newSuspendedTransaction {
        val round = RoundEntity.findById(roundId) ?: return@newSuspendedTransaction null
        val player = PlayerEntity.findById(playerId) ?: return@newSuspendedTransaction null

        ResultEntity.new {
            this.profit = profit
            this.round = round
            this.player = player
        }.toResultData()
    }

    suspend fun findByRoundIds(roundIds: List<Int>): List<Result> = newSuspendedTransaction {
        ResultEntity.find { Results.roundId inList roundIds }.map { it.toResultData() }
    }
}