package com.nuvio.tv.launcherhome

import android.app.admin.DeviceAdminReceiver

/**
 * Nuvio Launcher: lets the sleep timer put the TV into standby (DevicePolicyManager.lockNow). Its only policy is
 * force-lock. Switched on once with adb (dpm set-active-admin); it must be switched off before uninstalling
 * (tools/restore-google-tv.ps1 does this).
 */
class LauncherDeviceAdmin : DeviceAdminReceiver()
