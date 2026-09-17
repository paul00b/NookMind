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

# ── The three files that must exist ──────────────────────────────────────────
if (-not (Test-Path 'secrets.properties')) {
    throw "native/secrets.properties is missing. Copy secrets.properties.example and fill it in (README section 2)."
}
if (-not (Test-Path 'composeApp/google-services.json')) {
    Write-Host 'composeApp/google-services.json is missing: the build will work, push notifications will not.' -ForegroundColor Yellow
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
