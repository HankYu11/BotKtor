package com.hank.model.domain

import com.hank.db.ResultEntity
import kotlinx.serialization.Serializable

@Serializable
data class Result(
    val id: Int,
    val profit: Int?,
    val roundId: Int,
    val playerId: Int
)

fun ResultEntity.toResultData(): Result = Result(
    id = id.value,
    profit = profit,
    roundId = round.id.value,
    playerId = player.id.value,
)