package com.hank

import com.hank.model.response.GameDetails
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

object GameUpdateManager {
    // A map from gameId to a shared flow of its updates.
    private val gameUpdateFlows = ConcurrentHashMap<Int, MutableSharedFlow<GameDetails>>()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    /**
     * Gets or creates a SharedFlow for a given game ID.
     * New subscribers will receive the last emitted value upon subscription (replay = 1).
     */
    @OptIn(FlowPreview::class)
    fun getUpdatesFlow(gameId: Int): MutableSharedFlow<GameDetails> {
        return gameUpdateFlows.computeIfAbsent(gameId) {
            val newFlow = MutableSharedFlow<GameDetails>(replay = 1)
            scope.launch {
                newFlow.subscriptionCount
                    .debounce { 5000L }
                    .filter { count -> count == 0 }
                    .collect {
                        if (newFlow.subscriptionCount.value == 0) {
                            gameUpdateFlows.remove(gameId, newFlow)
                            this.coroutineContext.cancel()
                        }
                    }
            }
            newFlow
        }
    }

    /**
     * Notifies all subscribers for a given game that an update has occurred.
     */
    suspend fun notifyUpdate(gameId: Int, gameDetails: GameDetails) {
        getUpdatesFlow(gameId).emit(gameDetails)
    }
}