package com.hank.model.response

import com.hank.model.domain.Player
import com.hank.model.domain.Result
import com.hank.model.domain.Round
import kotlinx.serialization.Serializable

@Serializable
data class RoundDetails(
    val round: Round,
    val players: List<Player>,
    val results: List<Result>
)
