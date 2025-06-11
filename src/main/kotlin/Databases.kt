package com.hank

import com.hank.db.Games
import com.hank.db.Players
import com.hank.db.Results
import com.hank.db.Rounds
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.sql.Connection
import java.sql.DriverManager
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory

fun Application.configureDatabases() {
    val databasesLogger = LoggerFactory.getLogger("BotDatabase")

    databasesLogger.info("Attempting to configure databases...")

    try {
        val dbUrl = environment.config.property("postgres.url").getString()
        val dbUser = environment.config.property("postgres.user").getString()
        val dbPassword = environment.config.property("postgres.password").getString()
        val dbDriver = "org.postgresql.Driver" // PostgreSQL driver

        Database.connect(url = dbUrl, driver = dbDriver, user = dbUser, password = dbPassword)

        transaction {
            SchemaUtils.create(Games, Players, Rounds, Results)

            databasesLogger.info("Database tables created/verified with Exposed.")
        }
    } catch (e: Exception) {
        databasesLogger.error("Failed to configure databases.", e)
        throw e
    }
}