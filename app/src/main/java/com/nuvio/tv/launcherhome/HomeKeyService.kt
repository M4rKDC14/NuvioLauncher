package com.nuvio.tv.launcherhome

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.SystemClock
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

/**
 * Nuvio Launcher: makes the remote's Home button open this launcher, and its Input button open the launcher's
 * Inputs popup.
 *
 * Google TV keeps its own home screen as the default and offers no setting to change it, and pointing Home at this
 * app over adb was not enough on the owner's TV (Google TV still opened). This accessibility service sees key presses
 * before the system acts on them: it takes the Home press and opens the launcher instead. It also takes Input and the
 * gear button (see onKeyEvent) and does nothing with any other key.
 *
 * Start-up and standby: Google TV still brings its own home to the front after the TV starts or wakes. The service is
 * told when a new screen comes to the front (only which app and screen, never what is on it); when that is Google
 * TV's home screen, it opens this launcher's home instead. Shortly after the TV starts it also opens the launcher
 * once on its own, in case Google TV's home was already up before the service was running.
 *
 * Switched on once with adb (see the owner's instructions); switching it off gives Home back to Google TV.
 */
class HomeKeyService : AccessibilityService() {

    override fun onKeyEvent(event: KeyEvent): Boolean {
        val extra = when (event.keyCode) {
            KeyEvent.KEYCODE_HOME -> com.nuvio.tv.ui.launcher.EXTRA_GO_HOME
            // the remote's Input button (HID 0x0c0049 -> TV_INPUT): the launcher's Inputs popup instead of TCL's menu
            KeyEvent.KEYCODE_TV_INPUT -> com.nuvio.tv.ui.launcher.EXTRA_OPEN_INPUTS
            // the remote's gear button (key 204 -> NOTIFICATION in the remote's key map): the launcher's Quick settings
            // panel instead of Google TV's. SETTINGS too, for remotes whose gear sends that.
            KeyEvent.KEYCODE_NOTIFICATION, KeyEvent.KEYCODE_SETTINGS -> com.nuvio.tv.ui.launcher.EXTRA_OPEN_QUICK_SETTINGS
            else -> return false
        }
        // Act on the release of a short press; take the press and any repeats so the system never sees the key.
        if (event.action == KeyEvent.ACTION_UP && !event.isCanceled) openLauncher(extra)
        return true
    }

    private fun openLauncher(extra: String) {
        val intent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_HOME)
            .setClassName(packageName, "com.nuvio.tv.MainActivity")
            // back to the launcher's own task, closing anything opened on top of it (player, details, ...)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(extra, true)
        runCatching { startActivity(intent) }
    }

    private var lastRedirectAt = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        // the TV has just started: show the launcher rather than Google TV's home
        if (SystemClock.elapsedRealtime() < BOOT_WINDOW_MS) redirectHome()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        if (event.packageName?.toString() != GOOGLE_TV_HOME_PACKAGE) return
        // only Google TV's home screen itself, not its quick settings panel or other screens
        if (event.className?.toString()?.endsWith(GOOGLE_TV_HOME_SCREEN) != true) return
        redirectHome()
    }

    private fun redirectHome() {
        val now = SystemClock.elapsedRealtime()
        if (now - lastRedirectAt < REDIRECT_GAP_MS) return
        lastRedirectAt = now
        openLauncher(com.nuvio.tv.ui.launcher.EXTRA_GO_HOME)
    }

    private companion object {
        const val GOOGLE_TV_HOME_PACKAGE = "com.google.android.apps.tv.launcherx"
        const val GOOGLE_TV_HOME_SCREEN = ".home.HomeActivity"
        const val BOOT_WINDOW_MS = 3L * 60_000L
        const val REDIRECT_GAP_MS = 1_500L
    }

    override fun onInterrupt() = Unit
}
