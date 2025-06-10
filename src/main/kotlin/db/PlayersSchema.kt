package com.hank.db

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

object Players : IntIdTable("player") {
    val gameId = reference("gameid", Games)
    var balance = integer("balance")
    var name = varchar("name", 255)
}

// DAO Entity for Player
class PlayerEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<PlayerEntity>(Players)

    var game by GameEntity referencedOn Players.gameId
    var balance by Players.balance
    var name by Players.name
}