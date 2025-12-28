package com.pacedream.app.core.di

import android.content.Context
import com.pacedream.app.core.auth.AuthSession
import com.pacedream.app.core.auth.TokenStorage
import com.pacedream.app.core.config.AppConfig
import com.pacedream.app.core.network.ApiClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CoreModule {
    
    @Provides
    @Singleton
    fun provideJson(): Json {
        return Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
            encodeDefaults = true
        }
    }
    
    @Provides
    @Singleton
    fun provideAppConfig(): AppConfig {
        return AppConfig()
    }
    
    @Provides
    @Singleton
    fun provideTokenStorage(
        @ApplicationContext context: Context
    ): TokenStorage {
        return TokenStorage(context)
    }
    
    @Provides
    @Singleton
    fun provideApiClient(
        appConfig: AppConfig,
        tokenStorage: TokenStorage,
        json: Json
    ): ApiClient {
        return ApiClient(appConfig, tokenStorage, json)
    }
    
    @Provides
    @Singleton
    fun provideAuthSession(
        tokenStorage: TokenStorage,
        appConfig: AppConfig,
        json: Json,
        apiClient: ApiClient
    ): AuthSession {
        return AuthSession(tokenStorage, appConfig, json).also {
            it.setApiClient(apiClient)
        }
    }
}


