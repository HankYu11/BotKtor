package com.hank

import com.hank.model.response.GameDetails
import kotlinx.coroutines.flow.MutableSharedFlow
import java.util.concurrent.ConcurrentHashMap

object GameUpdateManager {
    // A map from gameId to a shared flow of its updates.
    private val gameUpdateFlows = ConcurrentHashMap<Int, MutableSharedFlow<GameDetails>>()

    /**
     * Gets or creates a SharedFlow for a given game ID.
     * New subscribers will receive the last emitted value upon subscription (replay = 1).
     */
    fun getUpdatesFlow(gameId: Int): MutableSharedFlow<GameDetails> {
        return gameUpdateFlows.computeIfAbsent(gameId) {
            MutableSharedFlow(replay = 1)
        }
    }

    /**
     * Notifies all subscribers for a given game that an update has occurred.
     */
    suspend fun notifyUpdate(gameId: Int, gameDetails: GameDetails) {
        // If a flow for this game exists, emit the new details.
        getUpdatesFlow(gameId).emit(gameDetails)
    }
}