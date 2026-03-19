# little helper script, compile and run in one go
$ErrorActionPreference = "Stop"

if (Test-Path bin) { Remove-Item -Recurse -Force bin }
New-Item -ItemType Directory -Path bin | Out-Null

javac -d bin src\*.java
java -cp bin Main
