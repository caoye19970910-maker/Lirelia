$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$dest = Join-Path $root "gradle\wrapper\gradle-wrapper.jar"

function Test-WrapperJar([string]$path) {
    if (-not (Test-Path $path)) { return $false }
    try {
        Add-Type -AssemblyName System.IO.Compression.FileSystem -ErrorAction SilentlyContinue
        $zip = [System.IO.Compression.ZipFile]::OpenRead($path)
        try {
            return $null -ne ($zip.Entries | Where-Object { $_.FullName -eq "org/gradle/wrapper/GradleWrapperMain.class" } | Select-Object -First 1)
        } finally {
            $zip.Dispose()
        }
    } catch {
        return $false
    }
}

if (Test-WrapperJar $dest) {
    Write-Host "Gradle wrapper is ready."
    exit 0
}
if (Test-Path $dest) { Remove-Item $dest -Force }

New-Item -ItemType Directory -Force -Path (Split-Path -Parent $dest) | Out-Null
$urls = @(
    "https://raw.githubusercontent.com/gradle/gradle/v8.10.2/gradle/wrapper/gradle-wrapper.jar",
    "https://cdn.jsdelivr.net/gh/gradle/gradle@v8.10.2/gradle/wrapper/gradle-wrapper.jar",
    "https://github.com/gradle/gradle/raw/refs/tags/v8.10.2/gradle/wrapper/gradle-wrapper.jar"
)

foreach ($url in $urls) {
    try {
        Write-Host "Downloading Gradle wrapper from $url ..."
        Invoke-WebRequest -UseBasicParsing -Uri $url -OutFile $dest
        if (Test-WrapperJar $dest) {
            Write-Host "Done: $dest"
            exit 0
        }
        if (Test-Path $dest) { Remove-Item $dest -Force }
    } catch {
        if (Test-Path $dest) { Remove-Item $dest -Force }
    }
}

Write-Error "Could not download a valid gradle-wrapper.jar. Check the network/VPN and run this script again."
exit 1
