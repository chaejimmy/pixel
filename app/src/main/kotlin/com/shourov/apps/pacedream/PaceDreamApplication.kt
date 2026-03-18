package com.shourov.apps.pacedream

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import com.google.firebase.FirebaseApp
import com.pacedream.app.core.auth.SessionManager
import com.shourov.apps.pacedream.core.network.auth.AuthSession
import com.shourov.apps.pacedream.util.ProfileVerifierLogger
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * [Application] class for PaceDream
 */
@HiltAndroidApp
class PaceDreamApplication : Application(), ImageLoaderFactory {

    @Inject
    lateinit var profileVerifierLogger: ProfileVerifierLogger

    @Inject
    lateinit var authSession: AuthSession

    @Inject
    lateinit var sessionManager: SessionManager

    override fun onCreate() {
        super.onCreate()

        // Initialize Firebase before any network calls. The firebase-perf gradle plugin
        // instruments OkHttp at bytecode level, so it requires Firebase to be initialized
        // prior to any OkHttpClient.execute() call (including those in authSession.initialize()).
        try {
            FirebaseApp.initializeApp(this)
        } catch (e: Exception) {
            Timber.e(e, "Firebase initialization failed; continuing without Firebase services")
        }

        profileVerifierLogger()

        // iOS parity: bootstrap session on app start if tokens exist.
        // Do not block app startup; protected actions can still gate via AuthFlowSheet.
        ProcessLifecycleOwner.get().lifecycleScope.launch(Dispatchers.IO) {
            authSession.initialize()
            sessionManager.initialize()
        }
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .crossfade(200)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.20)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(50L * 1024 * 1024) // 50 MB
                    .build()
            }
            .build()
    }
}
