package com.shourov.apps.pacedream.feature.webflow

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import com.pacedream.common.composables.theme.PaceDreamPrimary
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Launcher for Stripe Checkout using Chrome Custom Tabs
 * 
 * Opens the checkoutUrl in a Custom Tab and handles the return
 * via app links (booking-success / booking-cancelled)
 */
@Singleton
class CheckoutLauncher @Inject constructor() {
    
    /**
     * Launch checkout URL in Custom Tab
     */
    fun launchCheckout(context: Context, checkoutUrl: String): Boolean {
        return try {
            val uri = Uri.parse(checkoutUrl)
            
            val customTabsIntent = CustomTabsIntent.Builder()
                .setShowTitle(true)
                .setUrlBarHidingEnabled(true)
                .setDefaultColorSchemeParams(
                    CustomTabColorSchemeParams.Builder()
                        .setToolbarColor(0xFF5527D7.toInt()) // PaceDreamPrimary
                        .setNavigationBarColor(0xFF5527D7.toInt())
                        .build()
                )
                .build()
            
            customTabsIntent.launchUrl(context, uri)
            Timber.d("Launched checkout URL: $checkoutUrl")
            true
        } catch (e: Exception) {
            Timber.e(e, "Failed to launch checkout URL")
            // Fallback to regular browser
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(checkoutUrl))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                true
            } catch (e2: Exception) {
                Timber.e(e2, "Failed to open in browser")
                false
            }
        }
    }
    
    /**
     * Launch Auth0 universal login in Custom Tab
     */
    fun launchAuth0Login(context: Context, authUrl: String): Boolean {
        return try {
            val uri = Uri.parse(authUrl)
            
            val customTabsIntent = CustomTabsIntent.Builder()
                .setShowTitle(true)
                .setDefaultColorSchemeParams(
                    CustomTabColorSchemeParams.Builder()
                        .setToolbarColor(0xFF5527D7.toInt())
                        .build()
                )
                .build()
            
            customTabsIntent.launchUrl(context, uri)
            true
        } catch (e: Exception) {
            Timber.e(e, "Failed to launch Auth0 URL")
            false
        }
    }
    
    /**
     * Open external URL (for help, terms, etc.)
     */
    fun openExternalUrl(context: Context, url: String): Boolean {
        return try {
            val customTabsIntent = CustomTabsIntent.Builder()
                .setShowTitle(true)
                .build()
            
            customTabsIntent.launchUrl(context, Uri.parse(url))
            true
        } catch (e: Exception) {
            Timber.e(e, "Failed to open external URL")
            false
        }
    }
}


