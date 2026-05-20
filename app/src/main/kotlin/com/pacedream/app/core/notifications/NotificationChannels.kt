package com.pacedream.app.core.notifications

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat

/**
 * The canonical set of user-visible notification categories on Android.
 *
 * Each entry maps 1:1 to an Android NotificationChannel so the user can
 * independently mute "Bookings" from "Marketing" via system Settings.
 * Channel IDs are stable strings — never renumber, or existing devices
 * will end up with orphaned channels and the user's importance choices
 * will be lost.
 *
 * Some IDs (`bookings`, `messages`, `payments`, `marketing`) deliberately
 * match the IDs used by [com.shourov.apps.pacedream.notification.PaceDreamNotificationService]
 * so the two registration paths are idempotent and reference the same
 * underlying system channels.
 */
object NotificationChannels {

    data class Channel(
        val id: String,
        val displayName: String,
        val description: String,
        val importance: Int,
    )

    val BOOKINGS = Channel(
        id = "bookings",
        displayName = "Bookings",
        description = "Booking confirmations, changes, and check-in reminders.",
        importance = NotificationManagerCompat.IMPORTANCE_HIGH,
    )

    val MESSAGES = Channel(
        id = "messages",
        displayName = "Messages",
        description = "New chat messages from hosts, guests, and roommates.",
        importance = NotificationManagerCompat.IMPORTANCE_HIGH,
    )

    val PAYMENTS = Channel(
        id = "payments",
        displayName = "Payments",
        description = "Receipts, refunds, payouts, and payment failures.",
        importance = NotificationManagerCompat.IMPORTANCE_DEFAULT,
    )

    val HOST_UPDATES = Channel(
        id = "host_updates",
        displayName = "Host updates",
        description = "Listing approvals, inquiries, and other host-side alerts.",
        importance = NotificationManagerCompat.IMPORTANCE_DEFAULT,
    )

    val MARKETING = Channel(
        id = "marketing",
        displayName = "Promotions",
        description = "Deals, promotions, and other marketing messages.",
        importance = NotificationManagerCompat.IMPORTANCE_LOW,
    )

    val ALL: List<Channel> = listOf(BOOKINGS, MESSAGES, PAYMENTS, HOST_UPDATES, MARKETING)

    /**
     * Register every channel on the system NotificationManager.
     *
     * Safe to call on every app start: `createNotificationChannel` only
     * sets fields the user has not yet customised, so calling it again
     * with the same id preserves any importance change the user made
     * from system Settings.
     */
    fun registerAll(context: Context) {
        val manager = NotificationManagerCompat.from(context)
        val compatChannels = ALL.map { channel ->
            NotificationChannelCompat.Builder(channel.id, channel.importance)
                .setName(channel.displayName)
                .setDescription(channel.description)
                .build()
        }
        manager.createNotificationChannelsCompat(compatChannels)
    }

    /**
     * Whether the given category will actually surface a notification.
     *
     * Returns true only when both the app-level master toggle
     * (`areNotificationsEnabled`) is on and the per-channel importance
     * is not IMPORTANCE_NONE. On pre-O devices channels do not exist,
     * so we fall back to the master toggle alone.
     */
    fun isEnabled(context: Context, channel: Channel): Boolean {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return false
        // Pre-O devices don't have channels; areNotificationsEnabled is the
        // only signal we can use.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return true
        val sys = manager.getNotificationChannelCompat(channel.id) ?: return true
        return sys.importance != NotificationManagerCompat.IMPORTANCE_NONE
    }

    /** Master per-app toggle in system Settings. */
    fun isMasterEnabled(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    /**
     * Open the system settings page for a specific channel so the user
     * can mute it / change its importance. We never mutate channel state
     * from inside the app — Android forbids it after the channel is
     * created, and faking it would be a lying UI.
     *
     * Falls back to the app-level notification settings on pre-O.
     */
    fun openChannelSettings(context: Context, channel: Channel) {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                putExtra(Settings.EXTRA_CHANNEL_ID, channel.id)
            }
        } else {
            appNotificationSettingsIntent(context)
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /** Open the app-level notification settings (master toggle, channel list). */
    fun openAppNotificationSettings(context: Context) {
        val intent = appNotificationSettingsIntent(context)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    private fun appNotificationSettingsIntent(context: Context): Intent =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(android.net.Uri.fromParts("package", context.packageName, null))
        }
}
