package com.hank.model.request

import kotlinx.serialization.Serializable

@Serializable
data class CreateGameRequest(val playerNames: List<String>)