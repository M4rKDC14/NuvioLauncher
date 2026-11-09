# Restore Google TV: removes Nuvio Launcher and puts the TV back the way it was.
# Run from the folder this file is in, with the TV connected over adb:
#   powershell -ExecutionPolicy Bypass -File restore-google-tv.ps1
#
# 1. Switches back on the 19 apps that were switched off.
# 2. Turns off the launcher's Home/Input/gear button service and the sleep timer's device admin.
# 3. Uninstalls Nuvio Launcher (this also deletes the backup file kept on the TV).
# 4. Switches Google TV's home (launcherx) back on and sets it as the home screen.

$apps = @(
  "com.tcl.exhibit", "com.tcl.esticker", "com.tcl.repairguide", "com.tcl.ocean.instructions",
  "com.tcl.messagebox", "com.tcl.waterfall.overseas", "com.tcl.magiconnectfree", "com.tcl.usercenter",
  "tv.mopa.ginga", "com.google.android.videos", "com.google.android.play.games",
  "com.google.android.feedback", "com.google.android.syncadapters.calendar", "com.android.printspooler",
  "com.android.dreams.basic", "android.autoinstalls.config.google.gtvpai", "tv.gallery.nuviohome",
  "com.nuviodebug.com", "dev.vodik7.tvquickactions.free"
)

Write-Host "Switching apps back on..."
foreach ($app in $apps) { adb shell pm enable $app | Out-Null }

Write-Host "Turning off the launcher's button service..."
adb shell settings put secure enabled_accessibility_services null

Write-Host "Switching off the sleep timer's device admin (Android won't uninstall an active admin)..."
adb shell dpm remove-active-admin tv.nuviolauncher/com.nuvio.tv.launcherhome.LauncherDeviceAdmin

Write-Host "Uninstalling Nuvio Launcher..."
adb uninstall tv.nuviolauncher

Write-Host "Switching Google TV's home back on..."
adb shell pm enable com.google.android.apps.tv.launcherx

Write-Host "Setting Google TV as home..."
adb shell cmd package set-home-activity --user 0 com.google.android.apps.tv.launcherx/.home.HomeActivity

Write-Host "Done. Press Home on the remote."
