package com.hank.db

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

object Rounds : IntIdTable("round") {
    val bet = integer("bet")
    val gameId = reference("gameid", Games) // Foreign key to Games table
}

// DAO Entity for Round
class RoundEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<RoundEntity>(Rounds)

    var bet by Rounds.bet
    var game by GameEntity referencedOn Rounds.gameId
}