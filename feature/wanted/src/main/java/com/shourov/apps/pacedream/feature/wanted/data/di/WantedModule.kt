package com.shourov.apps.pacedream.feature.wanted.data.di

import com.shourov.apps.pacedream.feature.wanted.data.RequestsFiltersStore
import com.shourov.apps.pacedream.feature.wanted.data.RequestsFiltersStoreImpl
import com.shourov.apps.pacedream.feature.wanted.data.WantedRepository
import com.shourov.apps.pacedream.feature.wanted.data.WantedRepositoryImpl
import com.shourov.apps.pacedream.feature.wanted.data.remote.WantedApiService
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class WantedRepositoryModule {

    @Binds
    @Singleton
    abstract fun bindRepository(impl: WantedRepositoryImpl): WantedRepository

    @Binds
    @Singleton
    abstract fun bindRequestsFiltersStore(impl: RequestsFiltersStoreImpl): RequestsFiltersStore
}

@Module
@InstallIn(SingletonComponent::class)
object WantedNetworkModule {

    @Provides
    @Singleton
    fun provideWantedApiService(retrofit: Retrofit): WantedApiService =
        retrofit.create(WantedApiService::class.java)
}
