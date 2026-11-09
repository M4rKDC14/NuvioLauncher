# Set up Nuvio Launcher on the TV: the reverse of restore-google-tv.ps1.
# Run from the folder that holds the APK, with the TV connected over adb:
#   powershell -ExecutionPolicy Bypass -File setup-launcher.ps1 NuvioLauncher-1.6.15.apk
#
# 1. Installs (or updates) Nuvio Launcher.
# 2. Lets TCL start its button service (AUTO_START), then switches the service on, so Home, Input and the gear
#    button open the launcher.
# 3. Lets the sleep timer put the TV into standby (device admin, force-lock only), and gives notification access.
# 4. Switches off Google TV's home (launcherx), so Nuvio Launcher is the only home screen.
# 5. Switches off the 19 apps trimmed in Phase 3 (restore-google-tv.ps1 switches them back on).

param([Parameter(Mandatory = $true)][string]$Apk)

$pkg = "tv.nuviolauncher"
$service = "$pkg/com.nuvio.tv.launcherhome.HomeKeyService"
$listener = "$pkg/com.nuvio.tv.launcherhome.LauncherNotificationService"
$apps = @(
  "com.tcl.exhibit", "com.tcl.esticker", "com.tcl.repairguide", "com.tcl.ocean.instructions",
  "com.tcl.messagebox", "com.tcl.waterfall.overseas", "com.tcl.magiconnectfree", "com.tcl.usercenter",
  "tv.mopa.ginga", "com.google.android.videos", "com.google.android.play.games",
  "com.google.android.feedback", "com.google.android.syncadapters.calendar", "com.android.printspooler",
  "com.android.dreams.basic", "android.autoinstalls.config.google.gtvpai", "tv.gallery.nuviohome",
  "com.nuviodebug.com", "dev.vodik7.tvquickactions.free"
)

Write-Host "Installing $Apk..."
adb install -r $Apk

Write-Host "Opening the launcher (TCL only lets the button service start while the app is on screen)..."
adb shell monkey -p $pkg -c android.intent.category.LAUNCHER 1 | Out-Null
Start-Sleep -Seconds 3

Write-Host "Switching on the button service..."
adb shell appops set $pkg AUTO_START allow
adb shell appops write-settings
adb shell settings put secure enabled_accessibility_services $service
adb shell settings put secure accessibility_enabled 1

Write-Host "Allowing the sleep timer to put the TV into standby..."
adb shell dpm set-active-admin tv.nuviolauncher/com.nuvio.tv.launcherhome.LauncherDeviceAdmin

Write-Host "Giving notification access..."
adb shell cmd notification allow_listener $listener

Write-Host "Switching off Google TV's home, so Nuvio Launcher is the TV's only home screen..."
adb shell pm disable-user --user 0 com.google.android.apps.tv.launcherx

Write-Host "Switching off the trimmed apps..."
foreach ($app in $apps) { adb shell pm disable-user --user 0 $app | Out-Null }

Write-Host "Done. Press Home on the remote."
