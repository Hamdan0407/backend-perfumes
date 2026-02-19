@REM ----------------------------------------------------------------------------
@REM Maven Wrapper startup batch script
@REM ----------------------------------------------------------------------------

@echo off
setlocal

set MAVEN_PROJECTBASEDIR=%~dp0
set MAVEN_CMD_LINE_ARGS=%*

set WRAPPER_JAR="%MAVEN_PROJECTBASEDIR%.mvn\wrapper\maven-wrapper.jar"
set WRAPPER_PROPERTIES="%MAVEN_PROJECTBASEDIR%.mvn\wrapper\maven-wrapper.properties"

@REM Download wrapper JAR if it doesn't exist
if not exist %WRAPPER_JAR% (
    echo Downloading Maven Wrapper JAR...
    mkdir "%MAVEN_PROJECTBASEDIR%.mvn\wrapper" 2>nul
    powershell -Command "& {[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; (New-Object System.Net.WebClient).DownloadFile('https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar', '%MAVEN_PROJECTBASEDIR%.mvn\wrapper\maven-wrapper.jar')}"
)

if exist %WRAPPER_JAR% (
    java -jar %WRAPPER_JAR% %MAVEN_CMD_LINE_ARGS%
) else (
    echo ERROR: Could not download Maven Wrapper JAR
    exit /b 1
)

endlocal
