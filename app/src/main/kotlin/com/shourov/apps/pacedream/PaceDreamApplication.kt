package com.shourov.apps.pacedream

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.shourov.apps.pacedream.core.network.auth.AuthSession
import com.shourov.apps.pacedream.util.ProfileVerifierLogger
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * [Application] class for PaceDream
 */
@HiltAndroidApp
class PaceDreamApplication : Application() {

    @Inject
    lateinit var profileVerifierLogger: ProfileVerifierLogger

    @Inject
    lateinit var authSession: AuthSession

    override fun onCreate() {
        super.onCreate()

        profileVerifierLogger()

        // iOS parity: bootstrap session on app start if tokens exist.
        // Do not block app startup; protected actions can still gate via AuthFlowSheet.
        ProcessLifecycleOwner.get().lifecycleScope.launch(Dispatchers.IO) {
            authSession.initialize()
        }
    }
}
