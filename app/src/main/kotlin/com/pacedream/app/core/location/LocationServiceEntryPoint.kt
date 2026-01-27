package com.pacedream.app.core.location

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface LocationServiceEntryPoint {
    fun locationService(): LocationService
}
