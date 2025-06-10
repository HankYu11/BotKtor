package com.hank.model.request

import kotlinx.serialization.Serializable

@Serializable
data class CreateRoundRequest(
    val gameId: Int,
    val bet: Int,
    val results: List<PlayerResultRequest>
)

@Serializable
data class PlayerResultRequest(
    val playerId: Int,
    val profit: Int
)
