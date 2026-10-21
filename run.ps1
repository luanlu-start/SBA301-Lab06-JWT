param(
    [switch]$Test
)

$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot

$jdk21 = 'C:\Program Files\Java\jdk-21'
if (Test-Path -LiteralPath (Join-Path $jdk21 'bin\java.exe')) {
    $env:JAVA_HOME = $jdk21
    $env:Path = "$jdk21\bin;$env:Path"
}

$maven = Get-Command mvn -ErrorAction SilentlyContinue
if ($maven) {
    $mavenCommand = $maven.Source
} else {
    $mavenCommand = 'C:\Program Files\JetBrains\IntelliJ IDEA 2026.1.2\plugins\maven\lib\maven3\bin\mvn.cmd'
}

if (-not (Test-Path -LiteralPath $mavenCommand)) {
    throw 'Maven was not found. Open this project in IntelliJ and use its bundled Maven, or install Maven.'
}

if ($Test) {
    & $mavenCommand -B clean test
} else {
    & $mavenCommand spring-boot:run
}

exit $LASTEXITCODE
