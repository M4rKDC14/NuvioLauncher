# Nuvio Launcher: one script to set up the TV, or to put Google TV back.
#
# Set up, or update to a newer APK (run from the folder with the APK; the TV needs Developer options with network
# debugging on):
#   powershell -ExecutionPolicy Bypass -File nuvio-launcher.ps1 -TvIp 192.168.1.50
#   (uses the newest NuvioLauncher-*.apk in this folder; or name one with -Apk NuvioLauncher-1.6.19.apk)
#
# Back to Google TV:
#   powershell -ExecutionPolicy Bypass -File nuvio-launcher.ps1 -TvIp 192.168.1.50 -Restore
#
# Safety: Google TV's home is switched on while the launcher is installed, so the TV always has a home screen, and
# is switched off only at the very end, once Home is confirmed to open Nuvio Launcher. Every step checks the TV is
# still connected; if anything fails the script stops and says where, leaving Google TV's home on.

param(
  [string]$TvIp,
  [string]$Apk,
  [switch]$Restore
)

$ErrorActionPreference = "Continue"
$pkg = "tv.nuviolauncher"
$nuvioHome = "$pkg/com.nuvio.tv.MainActivity"
$service = "$pkg/com.nuvio.tv.launcherhome.HomeKeyService"
$listener = "$pkg/com.nuvio.tv.launcherhome.LauncherNotificationService"
$admin = "$pkg/com.nuvio.tv.launcherhome.LauncherDeviceAdmin"
$dream = "$pkg/com.nuvio.tv.ui.screens.ambient.AmbientDreamService"
$googleHome = "com.google.android.apps.tv.launcherx"
$googleHomeActivity = "$googleHome/.home.HomeActivity"
$backupOnTv = "/sdcard/Android/data/$pkg/files/nuvio-launcher-backup.json"
$apps = @(
  "com.tcl.exhibit", "com.tcl.esticker", "com.tcl.repairguide", "com.tcl.ocean.instructions",
  "com.tcl.messagebox", "com.tcl.waterfall.overseas", "com.tcl.magiconnectfree", "com.tcl.usercenter",
  "tv.mopa.ginga", "com.google.android.videos", "com.google.android.play.games",
  "com.google.android.feedback", "com.google.android.syncadapters.calendar", "com.android.printspooler",
  "com.android.dreams.basic", "android.autoinstalls.config.google.gtvpai", "tv.gallery.nuviohome",
  "com.nuviodebug.com", "dev.vodik7.tvquickactions.free"
)

function Step($text) { Write-Host "-> $text" -ForegroundColor Cyan }

function Fail($text) {
  Write-Host ""
  Write-Host "STOPPED: $text" -ForegroundColor Red
  Write-Host "Nothing after this step was done. Fix the cause and run the same command again." -ForegroundColor Red
  exit 1
}

# Waits up to 30 seconds for the TV to answer, reconnecting if it dropped.
function Wait-Online {
  for ($i = 0; $i -lt 15; $i++) {
    $state = (adb -s $script:serial get-state 2>$null | Out-String).Trim()
    if ($state -eq "device") { return $true }
    if ($TvIp) { adb connect $script:serial 2>&1 | Out-Null }
    Start-Sleep -Seconds 2
  }
  return $false
}

# Runs one shell command on the TV and returns its output; stops the script if the TV can't be reached.
function Tv {
  if (-not (Wait-Online)) { Fail "lost the connection to the TV ($script:serial)." }
  (adb -s $script:serial shell @args 2>&1 | Out-String).Trim()
}

function Home-Target {
  $out = Tv cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME
  ($out -split "`r?`n" | Select-Object -Last 1).Trim()
}

# ---- connect to the TV (only this TV; other devices are left alone) ----
if ($TvIp) {
  $serial = "${TvIp}:5555"
  Step "Connecting to $serial"
  adb connect $serial 2>&1 | Out-Null
} else {
  $online = @(adb devices | Select-String "\tdevice$" | ForEach-Object { ($_.Line -split "\t")[0] })
  if ($online.Count -ne 1) {
    Write-Host "Found $($online.Count) connected devices. Run with -TvIp <tv ip>." -ForegroundColor Red
    exit 1
  }
  $serial = $online[0]
}
if (-not (Wait-Online)) {
  Fail "can't reach the TV at $serial. Check the IP, that network debugging is on, and accept 'Allow debugging?' on the TV."
}
Write-Host "   Connected to $serial" -ForegroundColor DarkGray

if ($Restore) {
  # ---- back to Google TV ----
  Step "Copying your launcher backup off the TV (if there is one)"
  adb -s $serial pull $backupOnTv . 2>&1 | Out-Null
  if (Test-Path "nuvio-launcher-backup.json") { Write-Host "   Saved nuvio-launcher-backup.json here" -ForegroundColor DarkGray }

  Step "Switching Google TV's home back on and making it home (before anything is removed)"
  Tv pm enable $googleHome | Out-Null
  Tv cmd package set-home-activity --user 0 $googleHomeActivity | Out-Null
  if ((Home-Target) -notmatch [regex]::Escape($googleHome)) {
    Fail "Home doesn't open Google TV yet (it opens: $(Home-Target)). The launcher was left installed."
  }

  Step "Switching the trimmed apps back on"
  foreach ($app in $apps) { Tv pm enable $app | Out-Null }

  Step "Turning off the button service, notification access and screensaver"
  Tv settings put secure enabled_accessibility_services null | Out-Null
  Tv cmd notification disallow_listener $listener | Out-Null
  Tv settings delete secure screensaver_components | Out-Null

  Step "Switching off the sleep timer's device admin (Android won't uninstall an active admin)"
  Tv dpm remove-active-admin $admin | Out-Null

  Step "Uninstalling Nuvio Launcher"
  if (-not (Wait-Online)) { Fail "lost the connection to the TV before uninstalling." }
  $out = (adb -s $serial uninstall $pkg 2>&1 | Out-String).Trim()
  if ($out -notmatch "Success") { Fail "uninstall reported: $out. Google TV is home again; run -Restore again to retry." }

  Write-Host "Done. Press Home on the remote." -ForegroundColor Green
  exit 0
}

# ---- set up ----
if (-not $Apk) {
  $Apk = Get-ChildItem -Filter "NuvioLauncher-*.apk" | Sort-Object LastWriteTime -Descending | Select-Object -First 1 -ExpandProperty Name
}
if (-not $Apk -or -not (Test-Path $Apk)) {
  Fail "no NuvioLauncher-*.apk found here. Put the APK in this folder or pass -Apk <file>."
}

Step "Switching Google TV's home on for now (the TV keeps a home screen while the launcher is replaced)"
Tv pm enable $googleHome | Out-Null

Step "Installing $Apk"
$installed = $false
for ($try = 1; $try -le 3 -and -not $installed; $try++) {
  if (-not (Wait-Online)) { Fail "lost the connection to the TV before installing." }
  $out = (adb -s $serial install -r $Apk 2>&1 | Out-String).Trim()
  if ($out -match "Success") {
    $installed = $true
  } else {
    Write-Host "   Attempt $try didn't finish: $out" -ForegroundColor Yellow
    Start-Sleep -Seconds 5
  }
}
if (-not $installed) { Fail "the launcher did not install after 3 tries. Google TV's home is on, so the TV stays usable." }
$version = [regex]::Match((Tv dumpsys package $pkg), "versionName=(\S+)").Groups[1].Value
Write-Host "   Installed $version" -ForegroundColor DarkGray

Step "Making Nuvio Launcher the home screen"
Tv cmd package set-home-activity --user 0 $nuvioHome | Out-Null

Step "Opening the launcher (TCL only lets its button service start while the app is on screen)"
Tv monkey -p $pkg -c android.intent.category.LAUNCHER 1 | Out-Null
Start-Sleep -Seconds 4

Step "Switching on the Home/Input/gear button service"
Tv appops set $pkg AUTO_START allow | Out-Null
Tv appops write-settings | Out-Null
Tv settings put secure enabled_accessibility_services $service | Out-Null
Tv settings put secure accessibility_enabled 1 | Out-Null
Start-Sleep -Seconds 2
if ((Tv dumpsys accessibility) -notmatch "HomeKeyService") {
  Step "Button service not running yet; trying once more"
  Tv settings put secure enabled_accessibility_services null | Out-Null
  Tv monkey -p $pkg -c android.intent.category.LAUNCHER 1 | Out-Null
  Start-Sleep -Seconds 3
  Tv settings put secure enabled_accessibility_services $service | Out-Null
  Tv settings put secure accessibility_enabled 1 | Out-Null
  Start-Sleep -Seconds 2
  if ((Tv dumpsys accessibility) -notmatch "HomeKeyService") {
    Write-Host "   Button service still not running. With the launcher on screen, run this command again." -ForegroundColor Yellow
  }
}

Step "Allowing the sleep timer to put the TV into standby"
Tv dpm set-active-admin $admin | Out-Null

Step "Giving notification access (Quick settings list)"
Tv cmd notification allow_listener $listener | Out-Null

Step "Setting Nuvio Ambient as the screensaver"
Tv settings put secure screensaver_enabled 1 | Out-Null
Tv settings put secure screensaver_components $dream | Out-Null

Step "Switching off the trimmed apps"
foreach ($app in $apps) { Tv pm disable-user --user 0 $app | Out-Null }

if (Test-Path "nuvio-launcher-backup.json") {
  Step "Copying your launcher backup to the TV"
  if (-not (Wait-Online)) { Fail "lost the connection to the TV before copying the backup." }
  adb -s $serial push nuvio-launcher-backup.json "/sdcard/Android/data/$pkg/files/" 2>&1 | Out-Null
  Write-Host "   Restore it on the TV: Settings > Launcher > Backup > Restore launcher settings" -ForegroundColor Yellow
}

Step "Checking Home opens Nuvio Launcher"
$homeNow = Home-Target
if ($homeNow -notmatch [regex]::Escape($nuvioHome)) {
  Fail "Home opens '$homeNow', not Nuvio Launcher. Google TV's home was left on."
}

Step "Switching off Google TV's home (last step; Nuvio Launcher is confirmed as home)"
Tv pm disable-user --user 0 $googleHome | Out-Null
Tv cmd package set-home-activity --user 0 $nuvioHome | Out-Null

Write-Host "Done. Press Home on the remote." -ForegroundColor Green
