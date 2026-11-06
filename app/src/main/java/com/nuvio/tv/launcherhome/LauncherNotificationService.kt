package com.nuvio.tv.launcherhome

import android.app.Notification
import android.app.PendingIntent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** One row of the Quick settings panel's notification list. */
data class LauncherNotification(
    val key: String,
    val title: String,
    val text: String,
    val contentIntent: PendingIntent?
)

/**
 * Nuvio Launcher: reads the TV's notifications for the Quick settings panel. It shows nothing until notification
 * access is switched on with adb (cmd notification allow_listener); without it the panel has no notification list.
 * Only the title, text and the notification's own "open" action are kept; nothing is stored or sent anywhere.
 */
class LauncherNotificationService : NotificationListenerService() {

    override fun onListenerConnected() = refresh()

    override fun onListenerDisconnected() {
        _items.value = emptyList()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) = refresh()

    override fun onNotificationRemoved(sbn: StatusBarNotification?) = refresh()

    private fun refresh() {
        val active = runCatching { activeNotifications?.toList() }.getOrNull().orEmpty()
        _items.value = active
            .asSequence()
            .filter { it.packageName != packageName }
            .filter { it.notification.flags and Notification.FLAG_GROUP_SUMMARY == 0 }
            .sortedByDescending { it.postTime }
            .mapNotNull { sbn ->
                val extras = sbn.notification.extras
                val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
                val text = (extras.getCharSequence(Notification.EXTRA_TEXT)
                    ?: extras.getCharSequence(Notification.EXTRA_SUB_TEXT))?.toString()?.trim().orEmpty()
                if (title.isEmpty() && text.isEmpty()) return@mapNotNull null
                LauncherNotification(
                    key = sbn.key,
                    title = title.ifEmpty { text },
                    text = if (title.isEmpty()) "" else text,
                    contentIntent = sbn.notification.contentIntent
                )
            }
            .toList()
    }

    companion object {
        private val _items = MutableStateFlow<List<LauncherNotification>>(emptyList())
        val items: StateFlow<List<LauncherNotification>> = _items.asStateFlow()
    }
}
