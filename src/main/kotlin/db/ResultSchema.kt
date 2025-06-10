package com.hank.db

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

object Results : IntIdTable("result") {
    val roundId = reference("roundid", Rounds) // Foreign key to Rounds table
    val playerId = reference("playerid", Players) // Foreign key to Players table
    val profit = integer("profit")
}

// DAO Entity for Result
class ResultEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<ResultEntity>(Results)

    var round by RoundEntity referencedOn Results.roundId
    var player by PlayerEntity referencedOn Results.playerId
    var profit by Results.profit
}