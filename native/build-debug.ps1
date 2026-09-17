# Builds the debug APK and installs it if a phone is plugged in.
#
#   cd native
#   .\build-debug.ps1
#
# It sets up the two things the Windows machine needs on every Gradle run, both documented in
# docs/NEXT-STEPS.md: Android Studio's JDK (Java 8 on PATH cannot configure the build) and the
# truststore that carries Avast's root CA (its HTTPS scanning breaks every Gradle download).

$ErrorActionPreference = 'Stop'
Set-Location -Path $PSScriptRoot

# ── Java ─────────────────────────────────────────────────────────────────────
$jbr = 'C:\Program Files\Android\Android Studio\jbr'
if (Test-Path $jbr) {
    $env:JAVA_HOME = $jbr
    $env:PATH = "$jbr\bin;$env:PATH"
} elseif (-not $env:JAVA_HOME) {
    throw "Android Studio's JDK is not at $jbr and JAVA_HOME is unset. Set JAVA_HOME to a JDK 17+."
}
Write-Host "JDK: $env:JAVA_HOME" -ForegroundColor DarkGray

# ── Avast truststore, only if that copy exists ───────────────────────────────
$trustStore = Join-Path $env:USERPROFILE '.gradle\cacerts-avast'
if (Test-Path $trustStore) {
    $path = $trustStore -replace '\\', '/'
    $env:JAVA_TOOL_OPTIONS = "-Djavax.net.ssl.trustStore=$path -Djavax.net.ssl.trustStorePassword=changeit"
    Write-Host "Truststore: $trustStore" -ForegroundColor DarkGray
}

# -- Android SDK ---------------------------------------------------------------
# AGP finds the SDK through local.properties or ANDROID_HOME. Neither exists on a machine
# where the project was never opened in Android Studio, and the failure then reads
# "SDK location not found", which says nothing about how to fix it.
$sdk = $null
foreach ($candidate in @($env:ANDROID_HOME, $env:ANDROID_SDK_ROOT, (Join-Path $env:LOCALAPPDATA 'Android\Sdk'))) {
    if ($candidate -and (Test-Path (Join-Path $candidate 'platform-tools'))) { $sdk = $candidate; break }
}
if (-not $sdk) {
    throw @"
The Android SDK was not found.

Open Android Studio -> More Actions -> SDK Manager, install "Android SDK Platform 36"
and "Android SDK Platform-Tools", then run this script again. The SDK normally lands in
$env:LOCALAPPDATA\Android\Sdk.

Without it only the desktop preview can be built: .\gradlew.bat :composeApp:run
"@
}
$env:ANDROID_HOME = $sdk
Write-Host "Android SDK: $sdk" -ForegroundColor DarkGray

if (-not (Test-Path 'local.properties')) {
    # Forward slashes: a .properties file treats a backslash as an escape, and sdk.dir
    # accepts either form on Windows.
    "sdk.dir=" + ($sdk -replace '\\', '/') | Set-Content -Path 'local.properties' -Encoding ASCII
    Write-Host 'Wrote native/local.properties (git-ignored).' -ForegroundColor DarkGray
}

# ── The three files that must exist ──────────────────────────────────────────
if (-not (Test-Path 'secrets.properties')) {
    throw "native/secrets.properties is missing. Copy secrets.properties.example and fill it in (README section 2)."
}
if (-not (Test-Path 'composeApp/google-services.json')) {
    Write-Host 'composeApp/google-services.json is missing: the build will work, push notifications will not.' -ForegroundColor Yellow
} else {
    # The debug variant carries the .debug applicationId suffix. The google-services plugin
    # refuses to configure a variant whose applicationId matches no client in the file, and
    # fails mid-build with "No matching client found for package name". Catch it up front,
    # because the message does not say what to do about it.
    $clients = (Get-Content 'composeApp/google-services.json' -Raw | ConvertFrom-Json).client
    $packages = @($clients | ForEach-Object { $_.client_info.android_client_info.package_name })
    if ($packages -notcontains 'fr.paulbr.nookmind.debug') {
        throw @"
composeApp/google-services.json has no client for fr.paulbr.nookmind.debug.

It declares: $($packages -join ', ')

The debug build appends the .debug suffix so it can sit next to the Play Store app, and the
google-services plugin fails on a variant it has no client for. Pick one:

  1. Firebase console, project nookmind-8f5be -> add an Android app with the package name
     fr.paulbr.nookmind.debug, download google-services.json again (it will hold both
     clients) and replace the file. Needed anyway to test push on a debug build.
  2. Just to get an APK now: rename composeApp/google-services.json out of the way. The
     build then succeeds and push notifications are the one feature that stays off.
"@
    }
    Write-Host 'google-services.json: debug client present.' -ForegroundColor DarkGray
}

# Same signing key as the CI build. Without it AGP invents one per machine, so this APK
# could not replace an install coming from GitHub Actions, and Google Sign-In would refuse
# it because its SHA-1 is not the one registered for fr.paulbr.nookmind.debug.
if (-not (Test-Path 'composeApp/debug.keystore')) {
    Write-Host 'composeApp/debug.keystore is missing: this APK will not be able to replace one built by CI, and Google sign-in will not work. See README section 3.' -ForegroundColor Yellow
} else {
    $sha1 = (& "$env:JAVA_HOME\bin\keytool.exe" -list -v -keystore 'composeApp/debug.keystore' `
        -storepass android -alias androiddebugkey 2>$null |
        Select-String -Pattern 'SHA1:\s*(.+)$' |
        ForEach-Object { $_.Matches[0].Groups[1].Value.Trim() } |
        Select-Object -First 1)
    if ($sha1) { Write-Host "Signing SHA-1: $sha1" -ForegroundColor DarkGray }
}

# ── Build ────────────────────────────────────────────────────────────────────
Write-Host 'Building the debug APK...' -ForegroundColor Cyan
& .\gradlew.bat :composeApp:assembleDebug
if ($LASTEXITCODE -ne 0) { throw "Gradle failed with exit code $LASTEXITCODE." }

$apk = Get-ChildItem -Path 'composeApp\build\outputs\apk\debug' -Filter '*.apk' -Recurse |
    Sort-Object LastWriteTime -Descending | Select-Object -First 1
if (-not $apk) { throw 'The build reported success but produced no APK.' }

$target = Join-Path $PSScriptRoot 'NookMind-debug.apk'
Copy-Item $apk.FullName $target -Force
$size = [math]::Round($apk.Length / 1MB, 1)
Write-Host ''
Write-Host "APK: $target  ($size MB)" -ForegroundColor Green

# ── Install, if a device is there ────────────────────────────────────────────
$adb = Get-Command adb -ErrorAction SilentlyContinue
if (-not $adb) {
    $adb = Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools\adb.exe'
    if (-not (Test-Path $adb)) { $adb = $null }
}
if (-not $adb) {
    Write-Host 'adb not found: copy the APK to the phone by hand, or plug it in and re-run.' -ForegroundColor Yellow
    exit 0
}

$devices = & $adb devices | Select-Object -Skip 1 | Where-Object { $_ -match '\tdevice$' }
if (-not $devices) {
    Write-Host 'No phone connected. Plug it in with USB debugging on and re-run to install.' -ForegroundColor Yellow
    exit 0
}

Write-Host 'Installing...' -ForegroundColor Cyan
& $adb install -r $target
if ($LASTEXITCODE -ne 0) {
    Write-Host 'Install failed. If it mentions signatures, uninstall fr.paulbr.nookmind.debug first.' -ForegroundColor Yellow
    exit 1
}
Write-Host 'Installed. The debug build is "NookMind" with the .debug suffix, next to the Play Store one.' -ForegroundColor Green
