@ECHO off & setlocal enableextensions enabledelayedexpansion

:: Usage:
:: Run the service in the local environment:  .\scripts\run
:: Run the service inside a Docker container: .\scripts\run -s
:: Run the service inside a Docker container: .\scripts\run --in-sandbox
:: Run only the web service:                  .\scripts\run --webservice
:: Run only the streaming agent:              .\scripts\run --stream
:: Show how to use this script:               .\scripts\run -h
:: Show how to use this script:               .\scripts\run --help

:: strlen("\scripts\") => 9
SET APP_HOME=%~dp0
SET APP_HOME=%APP_HOME:~0,-9%
cd %APP_HOME%

IF "%1"=="-h" GOTO :Help
IF "%1"=="--help" GOTO :Help
IF "%1"=="-s" GOTO :RunInSandbox
IF "%1"=="--in-sandbox" GOTO :RunInSandbox
IF "%1"=="--webservice" GOTO :RunWebService
IF "%1"=="--stream" GOTO :RunStream
IF "%1"=="--iothubman" GOTO :RunIoTHubMan

SET STREAM_CLASS=com.microsoft.azure.iotsolutions.iotstreamanalytics.streamingAgent.Main

:RunLocally

    :: Check dependencies
    java -version > NUL 2>&1
    IF %ERRORLEVEL% NEQ 0 GOTO MISSING_JAVA

    :: Check settings
    call .\scripts\env-vars-check.cmd
    IF %ERRORLEVEL% NEQ 0 GOTO FAIL

    :: Run the Play Framework service
    start "" sbt run
    IF %ERRORLEVEL% NEQ 0 GOTO FAIL

    :: Run the stream agent
    start "" sbt "runMain %STREAM_CLASS%"
    IF %ERRORLEVEL% NEQ 0 GOTO FAIL

    goto :END


:RunWebService

    :: Check dependencies
    java -version > NUL 2>&1
    IF %ERRORLEVEL% NEQ 0 GOTO MISSING_JAVA

    :: Check settings
    call .\scripts\env-vars-check.cmd
    IF %ERRORLEVEL% NEQ 0 GOTO FAIL

    :: Run the Play Framework service
    call sbt run
    IF %ERRORLEVEL% NEQ 0 GOTO FAIL

    goto :END


:RunStream

    :: Check dependencies
    java -version > NUL 2>&1
    IF %ERRORLEVEL% NEQ 0 GOTO MISSING_JAVA

    :: Check settings
    call .\scripts\env-vars-check.cmd
    IF %ERRORLEVEL% NEQ 0 GOTO FAIL

    :: Run the stream agent
    call sbt "runMain %STREAM_CLASS%"
    IF %ERRORLEVEL% NEQ 0 GOTO FAIL

    goto :END


:RunInSandbox

    :: Folder where PCS sandboxes cache data. Reuse the same folder to speed up the
    :: sandbox and to save disk space.
    :: Use PCS_CACHE="%APP_HOME%\.cache" to cache inside the project folder
    SET PCS_CACHE="%TMP%\azure\iotpcs\.cache"

    :: Check dependencies
    docker version > NUL 2>&1
    IF %ERRORLEVEL% NEQ 0 GOTO MISSING_DOCKER

    :: Create cache folders to speed up future executions
    mkdir %PCS_CACHE%\sandbox\.ivy2 > NUL 2>&1
    mkdir %PCS_CACHE%\sandbox\.sbt > NUL 2>&1
    echo Note: caching build files in %PCS_CACHE%

    :: Check settings
    call .\scripts\env-vars-check.cmd
    IF %ERRORLEVEL% NEQ 0 GOTO FAIL

    :: Start the sandbox and run the service
    docker run -it ^
        -p 9023:9023 ^
        -e PCS_TELEMETRYAGENT_DOCUMENTDB_CONNSTRING ^
        -e PCS_TELEMETRY_WEBSERVICE_URL ^
        -e PCS_CONFIG_WEBSERVICE_URL ^
        -e PCS_IOTHUBMANAGER_WEBSERVICE_URL ^
        -e PCS_IOTHUBREACT_AZUREBLOB_ACCOUNT ^
        -e PCS_IOTHUBREACT_AZUREBLOB_KEY ^
        -e PCS_IOTHUBREACT_HUB_NAME ^
        -e PCS_IOTHUBREACT_HUB_ENDPOINT ^
        -e PCS_IOTHUBREACT_HUB_PARTITIONS ^
        -e PCS_IOTHUBREACT_ACCESS_CONNSTRING ^
        -v %PCS_CACHE%\sandbox\.ivy2:/root/.ivy2 ^
        -v %PCS_CACHE%\sandbox\.sbt:/root/.sbt ^
        -v %APP_HOME%:/opt/code ^
        azureiotpcs/code-builder-java:1.0 /opt/code/scripts/run

    :: Error 125 typically triggers in Windows if the drive is not shared
    IF %ERRORLEVEL% EQU 125 GOTO DOCKER_SHARE
    IF %ERRORLEVEL% NEQ 0 GOTO FAIL

    goto :END


:MISSING_JAVA
    echo ERROR: 'java' command not found.
    echo Install OpenJDK or Oracle JDK and make sure the 'java' command is in the PATH.
    echo OpenJDK installation: http://openjdk.java.net/install
    echo Oracle Java Standard Edition: http://www.oracle.com/technetwork/java/javase/downloads
    exit /B 1

:MISSING_DOCKER
    echo ERROR: 'docker' command not found.
    echo Install Docker and make sure the 'docker' command is in the PATH.
    echo Docker installation: https://www.docker.com/community-edition#/download
    exit /B 1

:DOCKER_SHARE
    echo ERROR: the drive containing the source code cannot be mounted.
    echo Open Docker settings from the tray icon, and fix the settings under 'Shared Drives'.
    exit /B 1

:FAIL
    echo Command failed
    endlocal
    exit /B 1

:END
endlocal
