package com.shourov.apps.pacedream.core.network.di

import com.shourov.apps.pacedream.core.network.api.ApiClient
import com.shourov.apps.pacedream.core.network.api.TokenProvider
import com.shourov.apps.pacedream.core.network.auth.AuthSession
import com.shourov.apps.pacedream.core.network.auth.TokenStorage
import com.shourov.apps.pacedream.core.network.config.AppConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import javax.inject.Singleton

/**
 * Enhanced network module providing iOS-parity networking components
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
    // Note: Json is provided by ApiModule.providesNetworkJson()
    
    @Provides
    @Singleton
    fun provideAppConfig(): AppConfig = AppConfig()
    
    @Provides
    @Singleton
    fun provideTokenProvider(tokenStorage: TokenStorage): TokenProvider = tokenStorage
    
    @Provides
    @Singleton
    fun provideApiClient(
        appConfig: AppConfig,
        json: Json,
        tokenProvider: TokenProvider
    ): ApiClient = ApiClient(appConfig, json, tokenProvider)
    
    @Provides
    @Singleton
    fun provideAuthSession(
        tokenStorage: TokenStorage,
        appConfig: AppConfig,
        json: Json,
        apiClient: ApiClient
    ): AuthSession = AuthSession(tokenStorage, appConfig, json).apply {
        // Avoid circular constructor deps; wire client after construction.
        setApiClient(apiClient)
    }
}


