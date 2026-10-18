param([Parameter(Mandatory=$true)][string]$InstallerDirectory)
$ErrorActionPreference = 'Stop'
$installer = Get-ChildItem $InstallerDirectory -Filter '*.exe' -Recurse | Select-Object -First 1
if (-not $installer) { throw 'The installer artifact contains no EXE.' }
$destination = Join-Path $env:RUNNER_TEMP 'LireliaReadestTest'
$setup = Start-Process $installer.FullName -ArgumentList @('/S', '/CURRENTUSER', "/D=$destination") -Wait -PassThru
if ($setup.ExitCode -ne 0) { throw "Installer failed: $($setup.ExitCode)" }
$application = Join-Path $destination 'readest.exe'
if (-not (Test-Path $application)) {
    Write-Host "Requested install folder: $destination"
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
    node check-ui.mjs
    if ($LASTEXITCODE -ne 0) { throw 'Installed application UI verification failed.' }
} finally {
    Remove-ItemProperty $policy -Name 'readest.exe' -ErrorAction SilentlyContinue
    Get-Process readest,lirelia-core -ErrorAction SilentlyContinue | Stop-Process -Force
}
