param([Parameter(Mandatory=$true)][string]$InstallerDirectory)
$ErrorActionPreference = 'Stop'
$installer = Get-ChildItem $InstallerDirectory -Filter '*.exe' -Recurse | Select-Object -First 1
if (-not $installer) { throw 'The installer artifact contains no EXE.' }
$destination = Join-Path $env:RUNNER_TEMP 'LireliaReadestTest'
$setup = Start-Process $installer.FullName -ArgumentList @('/S', "/D=$destination") -PassThru
if (-not $setup.WaitForExit(300000)) { $setup.Kill(); throw 'Installer did not complete in five minutes.' }
if ($setup.ExitCode -ne 0) { throw "Installer failed: $($setup.ExitCode)" }
$application = Join-Path $destination 'readest.exe'
if (-not (Test-Path $application)) { throw 'Installed readest.exe was not found.' }
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
    node check-ui.mjs
    if ($LASTEXITCODE -ne 0) { throw 'Installed application UI verification failed.' }
} finally {
    Remove-ItemProperty $policy -Name 'readest.exe' -ErrorAction SilentlyContinue
    Get-Process readest,lirelia-core -ErrorAction SilentlyContinue | Stop-Process -Force
}
