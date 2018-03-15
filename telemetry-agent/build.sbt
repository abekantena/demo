// Copyright (c) Microsoft. All rights reserved.

name := "telemetry-agent"
organization := "com.microsoft.azure.iotsolutions"

scalaVersion := "2.12.4"

libraryDependencies ++= {
  Seq(
    filters,
    guice,
    ws,

    // TODO: use official release
    // https://github.com/Azure/toketi-iothubreact/releases
    "com.microsoft.azure.iot" %% "iothub-react" % "0.10.0-DEV.170725c",

    // http://search.maven.org/#search%7Cga%7C1%7Ca%3A%22azure-documentdb%22
    "com.microsoft.azure" % "azure-documentdb" % "1.12.0"
  )
}

// Temporarily use IoT Hub React snapshots from Bintray
// https://bintray.com/microsoftazuretoketi/toketi-repo/iothub-react
resolvers += Resolver.bintrayRepo("microsoftazuretoketi", "toketi-repo")

lazy val commonSettings = Seq(
  version := "testing",

  organizationName := "Microsoft Azure",
  organizationHomepage := Some(new URL("https://www.microsoft.com/internet-of-things/azure-iot-suite")),
  homepage := Some(new URL("https://www.microsoft.com/internet-of-things/azure-iot-suite")),
  startYear := Some(2017),

  // Assembly
  assemblyMergeStrategy in assembly := {
    case m if m.startsWith("META-INF") ⇒ MergeStrategy.discard
    case m if m.contains(".txt")       ⇒ MergeStrategy.discard
    case x                             ⇒ (assemblyMergeStrategy in assembly).value(x)
  },

  // Publishing options, see http://www.scala-sbt.org/0.13/docs/Artifacts.html
  licenses += ("MIT", url("https://github.com/Azure/telemetry-agent-java/blob/master/LICENSE")),
  publishMavenStyle := true,
  publishArtifact in Test := true,
  publishArtifact in(Compile, packageDoc) := true,
  publishArtifact in(Compile, packageSrc) := true,
  publishArtifact in(Compile, packageBin) := true,

  // Test
  testOptions in Test := Seq(Tests.Argument(TestFrameworks.JUnit, "-a", "-v")),

  // Misc
  logLevel := Level.Info, // Debug|Info|Warn|Error
  scalacOptions ++= Seq("-deprecation", "-explaintypes", "-unchecked", "-feature"),
  showSuccess := true,
  showTiming := true,
  logBuffered := false,
  fork := true,
  parallelExecution := true
)

// Main module
lazy val telemetryagent = project.in(file("."))
  .enablePlugins(PlayJava)
  .configs(IntegrationTest)
  .settings(commonSettings)

// Play framework
PlayKeys.externalizeResources := false

// Docker
// Note: use lowercase name for the Docker image details
enablePlugins(JavaAppPackaging)
dockerRepository := Some("azureiotpcs")
dockerAlias := DockerAlias(dockerRepository.value, None, packageName.value + "-java", Some((version in Docker).value))
maintainer in Docker := "Devis Lucato (https://github.com/dluc)"
dockerBaseImage := "toketi/openjdk-8-jre-alpine-bash"
dockerUpdateLatest := false
dockerBuildOptions ++= Seq("--compress", "--label", "Tags=Azure,IoT,Solutions,Telemetry,Analytics,Java")
defaultLinuxInstallLocation in Docker := "/app"
dockerEntrypoint := Seq("./run.sh")
