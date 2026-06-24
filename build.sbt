import sbt._
import Keys._

lazy val scala3Version = "3.3.1"

lazy val commonSettings = Seq(
  version := "0.1.0",
  scalaVersion := scala3Version,
  organization := "com.education.grades",
  scalacOptions ++= Seq(
    "-deprecation",
    "-feature",
    "-unchecked",
    "-Xfatal-warnings",
    "-Ykind-projector"
  )
)

lazy val dependencies = Seq(
  // Cats Effect & Typelevel
  "org.typelevel" %% "cats-effect" % "3.5.2",
  "org.typelevel" %% "cats-core" % "2.10.0",

  // HTTP4s
  "org.http4s" %% "http4s-ember-server" % "0.23.23",
  "org.http4s" %% "http4s-ember-client" % "0.23.23",
  "org.http4s" %% "http4s-dsl" % "0.23.23",
  "org.http4s" %% "http4s-circe" % "0.23.23",

  // Circe for JSON
  "io.circe" %% "circe-core" % "0.14.6",
  "io.circe" %% "circe-generic" % "0.14.6",
  "io.circe" %% "circe-parser" % "0.14.6",

  // FS2 for streaming
  "co.fs2" %% "fs2-core" % "3.9.3",
  "co.fs2" %% "fs2-io" % "3.9.3",

  // Skunk for PostgreSQL
  "org.tpolecat" %% "skunk-core" % "0.6.0",
  "org.tpolecat" %% "skunk-circe" % "0.6.0",

  // Redis4Cats for Redis pub/sub
  "dev.profunktor" %% "redis4cats-effects" % "1.5.2",
  "dev.profunktor" %% "redis4cats-streams" % "1.5.2",

  // Logging
  "org.typelevel" %% "log4cats-slf4j" % "2.6.0",
  "ch.qos.logback" % "logback-classic" % "1.4.11",

  // Config
  "com.github.pureconfig" %% "pureconfig-core" % "0.17.4",

  // Testing
  "org.scalameta" %% "munit" % "0.7.29" % Test,
  "org.scalameta" %% "munit-scalacheck" % "0.7.29" % Test,
  "org.typelevel" %% "munit-cats-effect-3" % "1.0.7" % Test,
  "com.dimafeng" %% "testcontainers-scala-munit" % "0.41.0" % Test,
  "com.dimafeng" %% "testcontainers-scala-postgresql" % "0.41.0" % Test,

  // JWT for auth
  "com.github.jwt-scala" %% "jwt-circe" % "9.4.5",

  // BCrypt for password hashing (using Scala 2.13 version with cross-compilation)
  "com.github.t3hnar" % "scala-bcrypt_2.13" % "4.3.0",

  // Spring Security Crypto for BCrypt (alternative, pure Java)
  "org.springframework.security" % "spring-security-crypto" % "6.1.5",

  // Apache Commons Logging (required by Spring Security)
  "commons-logging" % "commons-logging" % "1.2"
)

// Root project
lazy val root = (project in file("."))
  .settings(
    name := "students-grade-microservices",
    commonSettings
  )
  .aggregate(
    common,
    gradeIngestion,
    gradeCalculation,
    reportGeneration,
    auditLogging,
    authService
  )

// Common shared module
lazy val common = (project in file("modules/common"))
  .settings(
    name := "common",
    commonSettings,
    libraryDependencies ++= dependencies
  )

// Grade Ingestion Service
lazy val gradeIngestion = (project in file("modules/grade-ingestion"))
  .enablePlugins(JavaAppPackaging)
  .settings(
    name := "grade-ingestion",
    commonSettings,
    libraryDependencies ++= dependencies,
    Compile / mainClass := Some("com.education.grades.ingestion.GradeIngestionServer"),
    Docker / packageName := "grade-ingestion-service",
    Docker / version := "0.1.0"
  )
  .dependsOn(common)

// Grade Calculation Service
lazy val gradeCalculation = (project in file("modules/grade-calculation"))
  .enablePlugins(JavaAppPackaging)
  .settings(
    name := "grade-calculation",
    commonSettings,
    libraryDependencies ++= dependencies,
    Compile / mainClass := Some("com.education.grades.calculation.GradeCalculationServer"),
    Docker / packageName := "grade-calculation-service",
    Docker / version := "0.1.0"
  )
  .dependsOn(common)

// Report Generation Service
lazy val reportGeneration = (project in file("modules/report-generation"))
  .enablePlugins(JavaAppPackaging)
  .settings(
    name := "report-generation",
    commonSettings,
    libraryDependencies ++= dependencies,
    Compile / mainClass := Some("com.education.grades.reports.ReportGenerationServer"),
    Docker / packageName := "report-generation-service",
    Docker / version := "0.1.0"
  )
  .dependsOn(common)

// Audit Logging Service
lazy val auditLogging = (project in file("modules/audit-logging"))
  .enablePlugins(JavaAppPackaging)
  .settings(
    name := "audit-logging",
    commonSettings,
    libraryDependencies ++= dependencies,
    Compile / mainClass := Some("com.education.grades.audit.AuditLoggingServer"),
    Docker / packageName := "audit-logging-service",
    Docker / version := "0.1.0"
  )
  .dependsOn(common)

// Auth Service
lazy val authService = (project in file("modules/auth-service"))
  .enablePlugins(JavaAppPackaging)
  .settings(
    name := "auth-service",
    commonSettings,
    libraryDependencies ++= dependencies,
    Compile / mainClass := Some("com.education.grades.auth.AuthServer"),
    Docker / packageName := "auth-service",
    Docker / version := "0.1.0"
  )
  .dependsOn(common)

