package com.worldvisionsoft.farminghelper.presentation.navigation

import kotlinx.serialization.Serializable

sealed interface Route {

    @Serializable
    data object Home : Route

    @Serializable
    data object Camera : Route

    @Serializable
    data object Result : Route

    @Serializable
    data object History : Route
}
