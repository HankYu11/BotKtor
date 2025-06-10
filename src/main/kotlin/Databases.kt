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

fun Application.configureDatabases() {
    val dbUrl = environment.config.property("postgres.url").getString()
    val dbUser = environment.config.property("postgres.user").getString()
    val dbPassword = environment.config.property("postgres.password").getString()
    val dbDriver = "org.postgresql.Driver" // PostgreSQL driver

    // Connect to the database using Exposed
    Database.connect(url = dbUrl, driver = dbDriver, user = dbUser, password = dbPassword)

    // Initialize the database schema (create tables if they don't exist)
    // This is typically done once at application startup.
    transaction {
        // Add logging if needed: addLogger(StdOutSqlLogger)
        SchemaUtils.create(Games, Players, Rounds, Results) // Add all your table objects here
        log.info("Database tables created/verified with Exposed.")
    }
}