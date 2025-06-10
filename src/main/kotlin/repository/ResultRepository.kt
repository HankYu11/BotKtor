package com.hank.repository

import com.hank.db.PlayerEntity
import com.hank.db.ResultEntity
import com.hank.db.Results
import com.hank.db.RoundEntity
import com.hank.model.domain.Result
import com.hank.model.domain.toResultData
import org.jetbrains.exposed.sql.transactions.transaction

class ResultRepository {

    fun create(profit: Int, roundId: Int, playerId: Int): Result? = transaction {
        val round = RoundEntity.findById(roundId) ?: return@transaction null
        val player = PlayerEntity.findById(playerId) ?: return@transaction null

        ResultEntity.new {
            this.profit = profit
            this.round = round
            this.player = player
        }.toResultData()
    }

    fun findById(id: Int): Result? = transaction {
        ResultEntity.findById(id)?.toResultData()
    }

    fun findAll(): List<Result> = transaction {
        ResultEntity.all().map { it.toResultData() }
    }

    fun findByRoundId(roundId: Int): List<Result> = transaction {
        ResultEntity.find { Results.roundId eq roundId }.map { it.toResultData() }
    }

    fun findByRoundIds(roundIds: List<Int>): List<Result> = transaction {
        ResultEntity.find { Results.roundId inList roundIds }.map { it.toResultData() }
    }

    fun findByPlayerId(playerId: Int): List<Result> = transaction {
        ResultEntity.find { Results.playerId eq playerId }.map { it.toResultData() }
    }

    fun update(id: Int, profit: Int?, roundId: Int?, playerId: Int?): Result? = transaction {
        val result = ResultEntity.findById(id) ?: return@transaction null
        result.apply {
            profit?.let { this.profit = it }
            roundId?.let { newRoundId ->
                RoundEntity.findById(newRoundId)?.let { this.round = it }
            }
            playerId?.let { newPlayerId ->
                PlayerEntity.findById(newPlayerId)?.let { this.player = it }
            }
        }.toResultData()
    }

    fun delete(id: Int): Boolean = transaction {
        ResultEntity.findById(id)?.delete() != null
    }
}