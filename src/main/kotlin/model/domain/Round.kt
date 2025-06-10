package com.hank.model.domain

import com.hank.db.RoundEntity
import kotlinx.serialization.Serializable

@Serializable // For Ktor JSON serialization if you plan to expose this via API
data class Round(
    val id: Int,
    val bet: Int?,
    val gameId: Int
)

fun RoundEntity.toRoundData(): Round = Round(
    id = id.value,
    bet = bet,
    gameId = game.id.value // Assumes 'game' is a non-null GameEntity in RoundEntity
)