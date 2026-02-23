package com.shourov.apps.pacedream.core.network.di

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.shourov.apps.pacedream.core.network.BuildConfig
import com.shourov.apps.pacedream.core.network.PaceDreamNetworkRepositoryImpl
import com.shourov.apps.pacedream.core.network.RetrofitPaceDreamApiService
import com.shourov.apps.pacedream.core.network.services.OtpService
import com.shourov.apps.pacedream.core.network.services.PaceDreamApiService
import com.shourov.apps.pacedream.core.network.services.VerificationApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

private const val PACEDREAM_BASE_URL = BuildConfig.SERVICE_URL

@Module
@InstallIn(SingletonComponent::class)
object ApiModule {

    @OptIn(ExperimentalSerializationApi::class)
    @Provides
    @Singleton
    fun providesNetworkJson(): Json = Json {
        explicitNulls = false
        ignoreUnknownKeys = true
    }

    @Singleton
    @Provides
    fun providesHttpLoggingInterceptor() = HttpLoggingInterceptor()
        .apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

    @Singleton
    @Provides
    fun providesOkHttpClient(
        httpLoggingInterceptor: HttpLoggingInterceptor,
        keyInterceptor: Interceptor,

        ): OkHttpClient =
        OkHttpClient
            .Builder()
            .addInterceptor(httpLoggingInterceptor)
            .addInterceptor(keyInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()

    @Singleton
    @Provides
    fun provideRetrofit(okHttpClient: OkHttpClient, gson: Gson): Retrofit = Retrofit.Builder()
        .addConverterFactory(GsonConverterFactory.create(gson))
        .baseUrl(PACEDREAM_BASE_URL)
        .client(okHttpClient)
        .build()

    @Singleton
    @Provides
    fun provideApiService(retrofit: Retrofit): RetrofitPaceDreamApiService =
        retrofit.create(RetrofitPaceDreamApiService::class.java)

    @Singleton
    @Provides
    fun providesPaceDreamApiService(retrofit: Retrofit): PaceDreamApiService =
        retrofit.create(PaceDreamApiService::class.java)

    @Singleton
    @Provides
    fun providesOtpService(retrofit: Retrofit): OtpService =
        retrofit.create(OtpService::class.java)

    @Singleton
    @Provides
    fun providesVerificationApi(retrofit: Retrofit): VerificationApi =
        retrofit.create(VerificationApi::class.java)

    @Singleton
    @Provides
    fun providesRepository(
        paceDreamNetworkRepositoryImpl: PaceDreamNetworkRepositoryImpl,
    ): PaceDreamNetworkRepositoryImpl = paceDreamNetworkRepositoryImpl

    @Singleton
    @Provides
    fun provideKeyInterceptor(): Interceptor =
        Interceptor {
            val request =
                it.request().newBuilder()
                    .addHeader("Accept", "application/json")
                    .build()
            it.proceed(request)
        }

    @Singleton
    @Provides
    fun provideGson(): Gson =
        GsonBuilder().registerTypeAdapter(RulesWrapper::class.java, RulesWrapperAdapter()).setLenient().create()
}