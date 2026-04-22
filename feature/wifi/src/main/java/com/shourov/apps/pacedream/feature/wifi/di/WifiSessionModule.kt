package com.shourov.apps.pacedream.feature.wifi.di

import com.shourov.apps.pacedream.feature.wifi.data.WifiSessionApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object WifiSessionModule {

    @Provides
    @Singleton
    fun providesWifiSessionApi(retrofit: Retrofit): WifiSessionApi =
        retrofit.create(WifiSessionApi::class.java)
}
