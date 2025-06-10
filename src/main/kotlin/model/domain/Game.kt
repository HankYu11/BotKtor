package com.hank.model.domain

import com.hank.db.GameEntity
import kotlinx.serialization.Serializable

@Serializable
data class Game(val id: Int)

fun GameEntity.toGameData(): Game = Game(id.value)