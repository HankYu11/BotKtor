package com.hank.model.domain

import kotlinx.serialization.Serializable

@Serializable
data class RoundWithResults(
    val roundId: Int,
    val bet: Int?,
    val results: List<Result>
)
