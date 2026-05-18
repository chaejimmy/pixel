package com.shourov.apps.pacedream.di

import android.content.Context
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Provides the app-wide Coil [ImageLoader] so screens can either resolve it
 * implicitly via [PaceDreamApplication.newImageLoader] (the `ImageLoaderFactory`
 * path Coil uses by default) or `@Inject` it directly when they need to share
 * the same singleton (e.g. background prefetching, tests).
 */
@Module
@InstallIn(SingletonComponent::class)
object ImageLoaderModule {

    private const val DISK_CACHE_BYTES = 250L * 1024L * 1024L // 250 MB
    private const val MEMORY_CACHE_PERCENT = 0.25

    @Provides
    @Singleton
    fun provideImageLoader(@ApplicationContext context: Context): ImageLoader =
        ImageLoader.Builder(context)
            .crossfade(200)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            // Honor the CDN's Cache-Control / ETag.  Without this Coil defaults
            // to ignoring server cache directives, which means the same image
            // URL is re-validated on every cache miss even when the response
            // headers say "fresh for 24 h".  Listing photos on the Pacedream
            // CDN already serve sensible max-age values, so deferring to them
            // cuts repeat bandwidth on warm caches without affecting cold load.
            .respectCacheHeaders(true)
            .memoryCache {
                MemoryCache.Builder(context)
                    .maxSizePercent(MEMORY_CACHE_PERCENT)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache"))
                    .maxSizeBytes(DISK_CACHE_BYTES)
                    .build()
            }
            .build()
}
