param([switch]$Dev, [switch]$SeparateTypecheck)
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
Set-Location $root

function Invoke-Checked {
    param([string]$Command, [string[]]$Arguments)
    & $Command @Arguments
    if ($LASTEXITCODE -ne 0) { throw "$Command failed ($LASTEXITCODE)" }
}

foreach ($command in @('node', 'pnpm', 'python', 'rustc', 'cargo')) {
    if (-not (Get-Command $command -ErrorAction SilentlyContinue)) {
        throw "Missing $command. See LIRELIA_README.md for Windows build prerequisites."
    }
}
if ((node -p "process.arch") -ne 'x64') { throw 'This build script targets Windows x64.' }
if ((Test-Path '.git') -and ((& git ls-files --stage -- packages/foliate-js) -match '^160000 ')) {
    Invoke-Checked 'git' @('submodule', 'update', '--init', '--depth', '1',
        'packages/foliate-js', 'packages/tauri', 'packages/simplecc-wasm', 'packages/qcms',
        'packages/js-mdict', 'apps/readest-app/src-tauri/plugins/tauri-plugin-turso',
        'apps/readest-app/src-tauri/plugins/tauri-plugin-webview-upgrade')
}
if (-not (Test-Path 'lirelia/.venv/Scripts/python.exe')) {
    Invoke-Checked 'python' @('-m', 'venv', 'lirelia/.venv')
}
$python = Join-Path $root 'lirelia/.venv/Scripts/python.exe'
Invoke-Checked $python @('-m', 'pip', 'install', '-r', 'lirelia/requirements.txt')
Invoke-Checked $python @('-m', 'unittest', 'discover', '-s', 'lirelia/tests', '-v')
Invoke-Checked $python @('lirelia/build_sidecar.py')
Invoke-Checked $python @('lirelia/smoke_core.py', 'lirelia/build/dist/lirelia-core.exe')
Invoke-Checked 'pnpm' @('install', '--frozen-lockfile')
Invoke-Checked 'pnpm' @('--filter', '@readest/readest-app', 'setup-vendors')
Invoke-Checked 'pnpm' @('--filter', '@readest/readest-app', 'exec', 'dotenv', '-e', '.env.tauri', '--', 'next', 'typegen')
Invoke-Checked 'pnpm' @('--filter', '@readest/readest-app', 'test', '--run',
    'src/__tests__/components/LireliaPanel.test.tsx', 'src/__tests__/services/lirelia-client.test.ts')
Invoke-Checked 'pnpm' @('--filter', '@readest/readest-app', 'lint')
if ($Dev) {
    Invoke-Checked 'pnpm' @('tauri', 'dev', '--config', 'src-tauri/tauri.lirelia.conf.json')
} else {
    $configPath = Join-Path $root 'apps/readest-app/next.config.mjs'
    $originalConfig = [System.IO.File]::ReadAllText($configPath)
    try {
        if ($SeparateTypecheck) {
            # pnpm lint above has already run tsc. Avoid a duplicate typecheck
            # worker while the bundler retains its compilation cache in memory.
            if (-not $originalConfig.Contains('const nextConfig = {')) {
                throw 'Cannot locate the pinned Next config for separate type checking.'
            }
            $temporaryConfig = $originalConfig.Replace('const nextConfig = {', "const nextConfig = {`n  typescript: { ignoreBuildErrors: true },")
            [System.IO.File]::WriteAllText($configPath, $temporaryConfig, [System.Text.UTF8Encoding]::new($false))
        }
        Invoke-Checked 'pnpm' @('tauri', 'build', '--config', 'src-tauri/tauri.lirelia.conf.json',
            '--target', 'x86_64-pc-windows-msvc', '--bundles', 'nsis')
    } finally {
        if ($SeparateTypecheck) {
            [System.IO.File]::WriteAllText($configPath, $originalConfig, [System.Text.UTF8Encoding]::new($false))
        }
    }
    Write-Host 'Installer: target/x86_64-pc-windows-msvc/release/bundle/nsis/'
}
