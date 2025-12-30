package com.shourov.apps.pacedream

import android.app.Application
import com.shourov.apps.pacedream.core.network.auth.AuthSession
import com.shourov.apps.pacedream.util.ProfileVerifierLogger
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * [Application] class for NiA
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
        CoroutineScope(Dispatchers.IO).launch {
            authSession.initialize()
        }
    }
}
