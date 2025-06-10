package com.hank.db

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

object Games : IntIdTable("game")

class GameEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<GameEntity>(Games)
}