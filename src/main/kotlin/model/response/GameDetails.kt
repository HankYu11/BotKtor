package com.hank.model.response

import com.hank.model.domain.Game
import com.hank.model.domain.Player
import com.hank.model.domain.RoundWithResults
import kotlinx.serialization.Serializable

@Serializable
data class GameDetails(
    val game: Game,
    val players: List<Player>,
    val roundWithResults: List<RoundWithResults>,
)