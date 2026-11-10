# Dot-source this file to use the portable JDK 21 + Maven from .tools (no system changes):  . .\scripts\env.ps1
$root = Split-Path $PSScriptRoot -Parent
$jdk  = (Get-ChildItem "$root\.tools" -Directory -Filter "jdk-21*" | Select-Object -First 1).FullName
$mvn  = (Get-ChildItem "$root\.tools" -Directory -Filter "apache-maven-*" | Select-Object -First 1).FullName
if ($jdk) { $env:JAVA_HOME = $jdk; $env:PATH = "$jdk\bin;$env:PATH" }
if ($mvn) { $env:PATH = "$mvn\bin;$env:PATH" }
java -version
mvn -v | Select-Object -First 1
