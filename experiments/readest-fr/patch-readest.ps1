$ErrorActionPreference = "Stop"
$root = Resolve-Path (Join-Path $PSScriptRoot "..\..")
$readest = Join-Path $root "readest-upstream"

$dest = Join-Path $readest "apps\readest-app\src\services\dictionaries\lemmatize"
$testDest = Join-Path $readest "apps\readest-app\src\__tests__\services\dictionaries\lemmatize"
Copy-Item (Join-Path $PSScriptRoot "french.ts") (Join-Path $dest "french.ts") -Force
Copy-Item (Join-Path $PSScriptRoot "french.test.ts") (Join-Path $testDest "french.test.ts") -Force

$index = Join-Path $dest "index.ts"
$text = Get-Content $index -Raw
if ($text -notmatch "lemmatizeFrench") {
  $nl = [Environment]::NewLine
  $text = $text.Replace("import { lemmatizeEnglish } from './english';", "import { lemmatizeEnglish } from './english';" + $nl + "import { lemmatizeFrench } from './french';")
  $text = $text.Replace("en: lemmatizeEnglish,", "en: lemmatizeEnglish," + $nl + "  fr: lemmatizeFrench,")
  Set-Content -Path $index -Value $text -Encoding utf8
}
Write-Host "French lemmatizer patch applied."
