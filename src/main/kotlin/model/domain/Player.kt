package com.hank.model.domain

import com.hank.db.PlayerEntity
import kotlinx.serialization.Serializable

@Serializable
data class Player(
    val id: Int,
    val name: String,
    val balance: Int = 0,
    val gameId: Int
)

fun PlayerEntity.toPlayerData(): Player = Player(
    id = id.value,
    name = name,
    balance = balance,
    gameId = game.id.value
)