param([Parameter(Mandatory=$true)][string]$InstallerDirectory)
$ErrorActionPreference = 'Stop'
$installer = Get-ChildItem $InstallerDirectory -Filter '*.exe' -Recurse | Select-Object -First 1
if (-not $installer) { throw 'The installer artifact contains no EXE.' }
New-Item verification-results -ItemType Directory -Force | Out-Null
& "$env:ProgramFiles\\7-Zip\\7z.exe" l $installer.FullName | Out-File verification-results/installer-contents.txt
Write-Host 'Prepare the Windows runner WebView2 runtime before installation.'
choco install webview2-runtime --yes --no-progress --limit-output
if ($LASTEXITCODE -notin @(0,1641,3010)) { throw "WebView2 runtime setup failed: $LASTEXITCODE" }
$destination = Join-Path $env:RUNNER_TEMP 'LireliaReadestTest'
$setup = Start-Process $installer.FullName -ArgumentList @('/S', '/CURRENTUSER') -Wait -PassThru
if ($setup.ExitCode -ne 0) { throw "Installer failed: $($setup.ExitCode)" }
$registered = Get-ChildItem 'HKCU:\\Software\\Microsoft\\Windows\\CurrentVersion\\Uninstall' |
    Get-ItemProperty | Where-Object { $_.DisplayName -eq 'Lirelia Readest' } | Select-Object -First 1
if ($registered.InstallLocation) { $destination = $registered.InstallLocation.Trim('"') }
Write-Host "Registered application location: $destination"
$application = Join-Path $destination 'readest.exe'
if (-not (Test-Path $application)) {
    Write-Host "Requested install folder: $destination"
    foreach ($hive in @('HKCU:\\Software\\Microsoft\\Windows\\CurrentVersion\\Uninstall', 'HKLM:\\Software\\Microsoft\\Windows\\CurrentVersion\\Uninstall')) {
        Get-ChildItem $hive -ErrorAction SilentlyContinue | Get-ItemProperty |
            Where-Object { $_.DisplayName -match 'Lirelia|Readest|WebView' } |
            Select-Object DisplayName,InstallLocation,DisplayVersion | Format-Table -AutoSize
    }
    Get-ChildItem "$env:TEMP\\msedge_installer.log","$env:LOCALAPPDATA\\Temp\\msedge_installer.log" -ErrorAction SilentlyContinue |
        ForEach-Object { Get-Content $_.FullName -Tail 60 }
    if (Test-Path $destination) { Get-ChildItem $destination -Recurse | Select-Object FullName,Length | Format-Table -AutoSize }
    foreach ($base in @("$env:LOCALAPPDATA\\Lirelia Readest", "$env:ProgramFiles\\Lirelia Readest")) {
        if (Test-Path $base) { Get-ChildItem $base -Recurse | Select-Object FullName,Length | Format-Table -AutoSize }
    }
    throw 'Installed readest.exe was not found after waiting for the installer process tree.'
}
$sidecar = Join-Path $destination 'lirelia-core.exe'
if (-not (Test-Path $sidecar)) { throw 'The installer omitted the native learning sidecar.' }
python ../windows-readest/overlay/lirelia/smoke_core.py $sidecar
if ($LASTEXITCODE -ne 0) { throw 'The installed learning sidecar failed its protocol and persistence checks.' }
$env:WEBVIEW2_ADDITIONAL_BROWSER_ARGUMENTS = '--remote-debugging-port=9222'
$policy = 'HKLM:\SOFTWARE\Policies\Microsoft\Edge\WebView2\AdditionalBrowserArguments'
New-Item $policy -Force | Out-Null
New-ItemProperty $policy -Name 'readest.exe' -Value '--remote-debugging-port=9222' -PropertyType String -Force | Out-Null
try {
    $app = Start-Process $application -PassThru
    python create-test-book.py
    if ($LASTEXITCODE -ne 0) { throw "Creating the native reader test EPUB failed." }
    $env:LIRELIA_TEST_APP = $application
    node check-ui.mjs
    if ($LASTEXITCODE -ne 0) { throw 'Installed application UI verification failed.' }
} finally {
    Remove-ItemProperty $policy -Name 'readest.exe' -ErrorAction SilentlyContinue
    Get-Process readest,lirelia-core -ErrorAction SilentlyContinue | Stop-Process -Force
}
