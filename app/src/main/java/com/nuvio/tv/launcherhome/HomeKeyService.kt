package com.nuvio.tv.launcherhome

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

/**
 * Nuvio Launcher: makes the remote's Home button open this launcher, and its Input button open the launcher's
 * Inputs popup.
 *
 * Google TV keeps its own home screen as the default and offers no setting to change it, and pointing Home at this
 * app over adb was not enough on the owner's TV (Google TV still opened). This accessibility service sees key presses
 * before the system acts on them: it takes the Home press and opens the launcher instead. It reads no screen content
 * (it asks for no window events, only key events) and does nothing with any other key.
 *
 * Switched on once with adb (see the owner's instructions); switching it off gives Home back to Google TV.
 */
class HomeKeyService : AccessibilityService() {

    override fun onKeyEvent(event: KeyEvent): Boolean {
        val openInputs = when (event.keyCode) {
            KeyEvent.KEYCODE_HOME -> false
            // the remote's Input button (HID 0x0c0049 -> TV_INPUT): the launcher's Inputs popup instead of TCL's menu
            KeyEvent.KEYCODE_TV_INPUT -> true
            else -> return false
        }
        // Act on the release of a short press; take the press and any repeats so the system never sees the key.
        if (event.action == KeyEvent.ACTION_UP && !event.isCanceled) openLauncher(openInputs)
        return true
    }

    private fun openLauncher(openInputs: Boolean = false) {
        val intent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_HOME)
            .setClassName(packageName, "com.nuvio.tv.MainActivity")
            // back to the launcher's own task, closing anything opened on top of it (player, details, ...)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .apply { if (openInputs) putExtra(com.nuvio.tv.ui.launcher.EXTRA_OPEN_INPUTS, true) }
        runCatching { startActivity(intent) }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit
}
