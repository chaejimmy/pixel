package com.shourov.apps.pacedream.feature.help.chat

import com.shourov.apps.pacedream.core.network.api.ApiClient
import com.shourov.apps.pacedream.core.network.config.AppConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import javax.inject.Singleton

/**
 * Explicit Hilt binding for [SupportChatRepository].
 *
 * The repository would otherwise be discovered through an `@Inject constructor`,
 * but on KSP2 + Hilt 2.58 + Kotlin 2.3 the `InjectProcessingStep` fails to
 * resolve `SupportChatRepository` while processing `SupportChatViewModel`'s
 * constructor, aborting the whole `:app:kspProdReleaseKotlin` task. Routing
 * the binding through `@Provides` uses Hilt's `ProvidesProcessingStep`
 * instead, which is unaffected by the bug.
 */
@Module
@InstallIn(SingletonComponent::class)
object SupportChatModule {

    @Provides
    @Singleton
    fun provideSupportChatRepository(
        apiClient: ApiClient,
        appConfig: AppConfig,
        json: Json,
    ): SupportChatRepository = SupportChatRepository(apiClient, appConfig, json)
}
