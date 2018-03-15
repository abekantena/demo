[![Build][build-badge]][build-url]
[![Issues][issues-badge]][issues-url]
[![Gitter][gitter-badge]][gitter-url]

Telemetry Agent Overview
========================

This service analyzes the telemetry stream, stores messages from Azure IoT Hub
to DocumentDb, and generates alerts according to defined rules.
The IoT stream is analyzed using a set of rules defined in the
[Telemetry service](https://github.com/Azure/device-telemetry-java),
and generating "alarms" when a message matches some of these rules. The alarms
are also stored in DocumentDb.

We provide also a
[.NET version](https://github.com/Azure/telemetry-agent-dotnet)
of this project.

Dependencies
============
* [Azure Cosmos DB account](https://ms.portal.azure.com/#create/Microsoft.DocumentDB) use the one created for [Storage Adapter microservice](https://github.com/Azure/pcs-storage-adapter-java)
* [Telemetry](https://github.com/Azure/device-telemetry-java)
* [Config](https://github.com/Azure/pcs-config-java)
* [IoT Hub Manager](https://github.com/Azure/iothub-manager-java)
* [Azure IoT Hub](https://azure.microsoft.com/services/iot-hub) use the one created for [IoT Hub Manager](https://github.com/Azure/iothub-manager-java)

How to use the microservice
===========================

## Quickstart - Running the service with Docker

1. Install Docker Compose: https://docs.docker.com/compose/install
1. Create an instance of an [Azure IoT Hub](https://azure.microsoft.com/services/iot-hub)
1. Store the "IoT Hub" connection string  in the [env-vars-setup](scripts)
   script.  The service internally uses
   [IoT Hub React](https://github.com/Azure/toketi-iothubreact) so you will notice some
   variables with an "IOTHUBREACT" prefix. For more information about
   environment variables, see the
   [Running the service with IntelliJ IDEA](README.md#running-the-service-with-intellij-idea).
1. Open a terminal console into the project folder, and run these commands to start
   the [Telemetry Agent](https://github.com/Azure/telemetry-agent-java) service
   ```
   cd scripts
   env-vars-setup      // (Bash users: ./env-vars-setup).  This script creates your env. variables
   cd docker
   docker-compose up
   ```
The Docker compose configuration requires the [dependencies](README.md#dependencies) resolved and
environment variables set as described previously. You should now start seeing the stream
content in the console.

## Java setup
1. Install the latest Java SDK.
2. Use your preferred IDE,
   [IntelliJ IDEA](https://www.jetbrains.com/idea/) and
   [Eclipse](https://www.eclipse.org) are the most popular,
   however anything should be just fine.

## Running the service with IntelliJ IDEA

1. Install Docker: https://docs.docker.com/engine/installation
1. Create an instance of [Azure IoT Hub](https://azure.microsoft.com/services/iot-hub)
1. Install [IntelliJ IDEA Community 2017](https://www.jetbrains.com/idea/), with SBT plugin enabled
1. Open the solution in IntelliJ IDEA
1. "Open" the project with IntelliJ, the IDE should automatically recognize the SBT structure. Wait for the IDE to download some dependencies (see IntelliJ status bar)
1. Create a new Run Configuration, of type "SBT Task" and enter "run 9023" (including the double quotes). This ensures that the service will start using the TCP port 9023
1. Either in IntelliJ Run Configuration or in your system, define the following
   environment variables:
    1. `PCS_TELEMETRYAGENT_DOCUMENTDB_CONNSTRING` = {your CosmosDb DocumentDb connection string}
    1. `PCS_TELEMETRY_WEBSERVICE_URL` = {the Telemetry service endpoint}
    1. `PCS_CONFIG_WEBSERVICE_URL` = {the Config service endpoint}
    1. `PCS_IOTHUBMANAGER_WEBSERVICE_URL` = {the IoT HubManager service endpoint}
    1. `PCS_IOTHUBREACT_AZUREBLOB_ACCOUNT` = {your Azure Blob Storage account name}
    1. `PCS_IOTHUBREACT_AZUREBLOB_KEY` = {your Azure Blob Storage account key}
    1. `PCS_IOTHUBREACT_HUB_NAME` = {your Azure IoT Hub - Event Hub compatible name}
    1. `PCS_IOTHUBREACT_HUB_ENDPOINT` = {your Azure IoT Hub endpoint}
    1. `PCS_IOTHUBREACT_HUB_PARTITIONS` = {your Azure IoT Hub partition count}
    1. `PCS_IOTHUBREACT_ACCESS_CONNSTRING` = {your Azure IoT Hub access connection string}
1. Start the StreamingAgent project
1. Using an HTTP client like [Postman](https://www.getpostman.com),
   test if the service is up and running  http://127.0.0.1:9023/v1/status

You should now start seeing the stream content in the IDE console.

## Running the service with Eclipse

The integration with Eclipse requires the
[sbteclipse plugin](https://github.com/typesafehub/sbteclipse), already
included, and an initial setup via command line (more info
[here](https://www.playframework.com/documentation/2.6.x/IDE)).

Steps using Eclipse Oxygen ("Eclipse for Java Developers" package):

1. Open a console (either Bash or Windows CMD), move into the project folder,
  execute `sbt compile` and then `sbt eclipse`. This generates some files
  required by Eclipse to recognize the project.
1. Open Eclipse, and from the Welcome screen "Import" an existing project,
  navigating to the root folder of the project.
1. Run the [env-vars-setup](scripts/env-vars-setup) script after adding the requried values
1. From the console run `sbt -jvm-debug 9999 "run 9023"` to start the project
1. Test that the service is up and running pointing your browser to
  http://127.0.0.1:9023/v1/status
1. In Eclipse, select "Run -> Debug Configurations" and add a "Remote Java
  Application", using "localhost" and port "9999".
1. After saving this configuration, you can click "Debug" to connect to the
  running application.

## Project Structure
This microservice contains the following projects:
* **Code** for the application is in ```app/com.microsoft.azure.iotsolutions.telemetryagent/```
    * **WebService** - Java web service exposing REST interface for status checkpoint to verify that the dependencies are resolved
    * **Services** - Java project containing business logic for interacting with Azure services (IoTHub, DocDb etc.) and device telemetry microservice
    * **StreamingAgent** - Java project that reads messages from IoT Hub, processes them using rules that generate alarms and store alarms and messages in the storage
* **Tests**
    * **WebService** - Unit tests for web services functionality
    * **Services** - Unit tests for services functionality
* **Scripts** - contains build scripts, docker container creation scripts,
and scripts for running the microservice from the command line
* **Routes** - defines the URL mapping to web service classes

## Build & Run from the command line

The [scripts](scripts) folder includes some scripts for frequent tasks you
might want to run from the command line:

* `compile`: compile the project.
* `build`: compile the project and run the tests.
* `run`: compile the project and run the service.

The scripts check for the environment variables setup. You can set the
environment variables globally in your OS, or use the [env-vars-setup](scripts/env-vars-setup)
in the scripts folder.

If you are familiar with [SBT](http://www.scala-sbt.org), you can also use SBT
directly. A copy of SBT is included in the root of the project.

## Updating the Docker image

The `scripts` folder includes a [docker](scripts/docker) subfolder with the files
required to package the service into a Docker image:

* `build`: build a Docker container and store the image in the local registry
* `run`: run the Docker container from the image stored in the local registry
script

You might notice that there is no `Dockerfile`. All Docker settings are
defined in [build.sbt](build.sbt).

```scala
dockerRepository := Some("azureiotpcs")
dockerAlias := DockerAlias(dockerRepository.value, None, packageName.value + "-java", Some((version in Docker).value))
dockerBaseImage := "toketi/openjdk-8-jre-alpine-bash"
dockerUpdateLatest := false
dockerBuildOptions ++= Seq("--compress", "--label", "Tags=Azure,IoT,PCS,Java")
dockerEntrypoint := Seq("bin/telemetry-agent")
```

The package logic is executed via
[sbt-native-packager](https://github.com/sbt/sbt-native-packager), installed
in [plugins.sbt](project/plugins.sbt).

You can also start Telemetry service and its dependencies in one simple step,
using Docker Compose with the
[docker-compose.yml](scripts/docker/docker-compose.yml) file in the project:

```
cd scripts/docker
docker-compose up
```

The Docker compose configuration requires the [dependencies](README.md#dependencies) resolved and environment variables set as described previously.

## Configuration and Environment variables

The service configuration is stored using Akka's
[HOCON](https://github.com/typesafehub/config/blob/master/HOCON.md)
format in [application.conf](conf/application.conf).

The HOCON format is a human readable format, very close to JSON, with some
useful features:

* Ability to write comments
* Support for substitutions, e.g. referencing environment variables
* Supports JSON notation

The configuration file in the repository references some environment
variables that need to created at least once. Depending on your OS and
the IDE, there are several ways to manage environment variables:

* For Windows users, the [env-vars-setup.cmd](scripts/env-vars-setup.cmd)
  script needs to be prepared and executed just once. When executed, the
  settings will persist across terminal sessions and reboots.
* For Linux and OSX environments, the [env-vars-setup](scripts/env-vars-setup)
  script needs to be executed every time a new console is opened.
  Depending on the OS and terminal, there are ways to persist values
  globally, for more information these pages should help:
  * https://stackoverflow.com/questions/13046624/how-to-permanently-export-a-variable-in-linux
  * https://stackoverflow.com/questions/135688/setting-environment-variables-in-os-x
  * https://help.ubuntu.com/community/EnvironmentVariables
* IntelliJ IDEA: env. vars can be set in each Run Configuration, see
  https://www.jetbrains.com/help/idea/run-debug-configuration-application.html

Contributing to the solution
============================
Please follow our [contribution guildelines](CONTRIBUTING.md).  We love PRs too.

Troubleshooting
===============

Feedback
========
Please enter issues, bugs, or suggestions as GitHub Issues [here](https://github.com/Azure/telemetry-agent-java/issues)

[build-badge]: https://img.shields.io/travis/Azure/telemetry-agent-java.svg
[build-url]: https://travis-ci.org/Azure/telemetry-agent-java
[issues-badge]: https://img.shields.io/github/issues/azure/telemetry-agent-java.svg
[issues-url]: https://github.com/azure/telemetry-agent-java/issues
[gitter-badge]: https://img.shields.io/gitter/room/azure/iot-solutions.js.svg
[gitter-url]: https://gitter.im/azure/iot-solutions
