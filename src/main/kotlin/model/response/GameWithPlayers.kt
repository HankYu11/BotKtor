package com.hank.model.response

import com.hank.model.domain.Game
import com.hank.model.domain.Player
import kotlinx.serialization.Serializable

@Serializable
data class GameWithPlayers(
    val game: Game,
    val players: List<Player>,
)
