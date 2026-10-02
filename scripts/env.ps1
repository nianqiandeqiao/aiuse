# Configure JAVA_HOME / MAVEN_HOME for the current session.
# This machine has neither Java nor Maven on PATH, so every other script dot-sources this file.
$ErrorActionPreference = 'Stop'

$javaHomeCandidates = @(
    "C:\Users\hp\.jdks\ms-17.0.20.1",
    "C:\Program Files\Java\latest",
    "C:\Users\hp\.jdks\openjdk-26.0.2.1"
)

$javaHome = $javaHomeCandidates | Where-Object { Test-Path (Join-Path $_ "bin\java.exe") } | Select-Object -First 1
if (-not $javaHome) {
    throw "JDK not found. Please edit javaHomeCandidates in scripts/env.ps1"
}

$wrapperRoot = Join-Path $env:USERPROFILE ".m2\wrapper\dists"
$mvnBin = $null
if (Test-Path $wrapperRoot) {
    $mvnBin = Get-ChildItem -Path $wrapperRoot -Filter "mvn.cmd" -Recurse -ErrorAction SilentlyContinue |
        Where-Object { $_.FullName -match "apache-maven-3.9.16-bin" } | Select-Object -First 1
    if (-not $mvnBin) {
        $mvnBin = Get-ChildItem -Path $wrapperRoot -Filter "mvn.cmd" -Recurse -ErrorAction SilentlyContinue |
            Select-Object -First 1
    }
}

$mavenHome = $null
if ($mvnBin) {
    $mavenHome = Split-Path (Split-Path $mvnBin.FullName -Parent) -Parent
}

$env:JAVA_HOME = $javaHome
$env:Path = (Join-Path $javaHome "bin") + ";" + $env:Path

if ($mavenHome) {
    $env:MAVEN_HOME = $mavenHome
    $env:Path = (Join-Path $mavenHome "bin") + ";" + $env:Path
}

if (Test-Path "C:\Program Files\Git\cmd") {
    $env:Path = $env:Path + ";C:\Program Files\Git\cmd"
}

Write-Host "[env] JAVA_HOME  = $env:JAVA_HOME"
if ($mavenHome) {
    Write-Host "[env] MAVEN_HOME = $env:MAVEN_HOME"
} else {
    Write-Warning "[env] Maven not found under $wrapperRoot"
}

