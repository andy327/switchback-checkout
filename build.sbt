// Global settings
ThisBuild / organization := "io.github.andy327"
ThisBuild / homepage := Some(url("https://github.com/andy327/switchback-checkout"))
ThisBuild / description := "A Scala microservices checkout showcase: synchronous REST orchestration and asynchronous Kafka event fan-out"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / versionScheme := Some("early-semver")
ThisBuild / scalaVersion := "2.13.18"

ThisBuild / scalacOptions ++= Seq(
  "-deprecation",
  "-Wunused", // For RemoveUnused rule
  "-Wunused:imports" // For OrganizeImports.removeUnused = true
)

// Required for Scalafix semantic rules
ThisBuild / semanticdbEnabled := true
// scalafix 0.14.3 bakes in an older semanticdb; 4.13.9 is the first scalameta release with a
// semanticdb-scalac build for Scala 2.13.18
ThisBuild / semanticdbVersion := "4.13.9"

// scalafix and scalafmt each run twice on purpose: the first pass can produce code the second pass
// still rewrites (e.g. an import removal that changes line widths), so a single run is not a fixpoint
addCommandAlias("formatAll", ";scalafixAll;scalafixAll;scalafmtAll;scalafmtAll;scalafmtSbt")
addCommandAlias("ci", ";clean;scalafixAll --check;scalafmtCheckAll;scalafmtSbtCheck;coverage;test;coverageAggregate")

// Suppress -Wunused warnings in the sbt console so REPL use isn't noisy
lazy val noUnusedInConsoles = {
  def dropUnused(opts: Seq[String]) =
    opts.filterNot(o => o.startsWith("-Wunused") || o.startsWith("-Ywarn-unused"))
  Seq(
    Compile / console / scalacOptions := dropUnused((Compile / console / scalacOptions).value),
    Test / console / scalacOptions := dropUnused((Test / console / scalacOptions).value)
  )
}

// NOTE: versions are a reasonable recent set for the Scala 2.13 typelevel stack. Run `sbt update`
// after cloning and bump anything that fails to resolve.
val versions: Map[String, String] = Map(
  "cats-effect" -> "3.5.7",
  "circe" -> "0.14.10",
  "ciris" -> "3.7.0",
  "fs2-kafka" -> "3.6.0",
  "http4s" -> "0.23.30",
  "cats-retry" -> "3.1.3",
  "tapir" -> "1.11.13",
  "sttp" -> "3.10.2",
  "log4cats" -> "2.7.0",
  "logback" -> "1.5.12",
  "scalatest" -> "3.2.19",
  "ce-testing" -> "1.6.0",
  "dimafeng" -> "0.43.0",
  "testcontainers" -> "1.21.1"
)

// Dependency bundles, assembled per module below.
lazy val catsEffect = Seq(
  "org.typelevel" %% "cats-effect" % versions("cats-effect")
)

lazy val circe = Seq(
  "io.circe" %% "circe-core" % versions("circe"),
  "io.circe" %% "circe-generic" % versions("circe"),
  "io.circe" %% "circe-parser" % versions("circe")
)

// Tapir endpoint definitions live in `common`; server + client interpreters are added per service.
lazy val tapirCore = Seq(
  "com.softwaremill.sttp.tapir" %% "tapir-core" % versions("tapir"),
  "com.softwaremill.sttp.tapir" %% "tapir-json-circe" % versions("tapir")
)

lazy val tapirServer = Seq(
  "com.softwaremill.sttp.tapir" %% "tapir-http4s-server" % versions("tapir"),
  "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle" % versions("tapir"),
  "org.http4s" %% "http4s-ember-server" % versions("http4s")
)

// Type-safe inter-service client, derived from the same Tapir endpoints defined in `common`.
lazy val tapirClient = Seq(
  "com.softwaremill.sttp.tapir" %% "tapir-sttp-client" % versions("tapir"),
  "com.softwaremill.sttp.client3" %% "cats" % versions("sttp")
)

lazy val fs2Kafka = Seq(
  "com.github.fd4s" %% "fs2-kafka" % versions("fs2-kafka")
)

lazy val retry = Seq(
  "com.github.cb372" %% "cats-retry" % versions("cats-retry")
)

lazy val config = Seq(
  "is.cir" %% "ciris" % versions("ciris")
)

lazy val logging = Seq(
  "org.typelevel" %% "log4cats-slf4j" % versions("log4cats"),
  "ch.qos.logback" % "logback-classic" % versions("logback")
)

lazy val testDeps = Seq(
  "org.scalatest" %% "scalatest" % versions("scalatest") % Test,
  "org.typelevel" %% "cats-effect-testing-scalatest" % versions("ce-testing") % Test
)

// Integration tests spin up a real Kafka broker via Testcontainers.
lazy val kafkaItDeps = Seq(
  "com.dimafeng" %% "testcontainers-scala-scalatest" % versions("dimafeng") % Test,
  "com.dimafeng" %% "testcontainers-scala-kafka" % versions("dimafeng") % Test,
  "org.testcontainers" % "kafka" % versions("testcontainers") % Test
)

lazy val commonSettings = Seq(
  libraryDependencies ++= testDeps
)

// A single fat-jar merge strategy shared by the runnable services.
lazy val serviceAssembly = assembly / assemblyMergeStrategy := {
  case PathList("META-INF", "services", _*) => MergeStrategy.concat
  case PathList("META-INF", _ @_*)          => MergeStrategy.discard
  case "reference.conf"                     => MergeStrategy.concat
  case _                                    => MergeStrategy.first
}

// ---------------------------------------------------------------------------
// common - the shared contract: domain model, JSON codecs, Kafka topics/events,
// and the Tapir endpoint definitions that both servers and clients derive from.
// ---------------------------------------------------------------------------
lazy val common = project
  .in(file("common"))
  .settings(
    commonSettings,
    noUnusedInConsoles,
    name := "common",
    libraryDependencies ++= catsEffect ++ circe ++ tapirCore
  )

// ---------------------------------------------------------------------------
// order-service - the orchestrator: REST entry point, synchronous sttp clients
// to inventory + payment (with retries + a fail-fast guard), and the Kafka producer.
// ---------------------------------------------------------------------------
lazy val order = project
  .in(file("order-service"))
  .dependsOn(common)
  .settings(
    commonSettings,
    noUnusedInConsoles,
    name := "order-service",
    Compile / mainClass := Some("io.github.andy327.switchback.order.OrderServer"),
    libraryDependencies ++= catsEffect ++ circe ++ tapirServer ++ tapirClient ++ fs2Kafka ++ retry ++ config ++ logging,
    libraryDependencies ++= kafkaItDeps,
    serviceAssembly
  )

// ---------------------------------------------------------------------------
// inventory-service - REST: reserve / release stock against an in-memory store.
// ---------------------------------------------------------------------------
lazy val inventory = project
  .in(file("inventory-service"))
  .dependsOn(common)
  .settings(
    commonSettings,
    noUnusedInConsoles,
    name := "inventory-service",
    Compile / mainClass := Some("io.github.andy327.switchback.inventory.InventoryServer"),
    libraryDependencies ++= catsEffect ++ circe ++ tapirServer ++ config ++ logging,
    serviceAssembly
  )

// ---------------------------------------------------------------------------
// payment-service - REST: charge / refund against an in-memory ledger.
// ---------------------------------------------------------------------------
lazy val payment = project
  .in(file("payment-service"))
  .dependsOn(common)
  .settings(
    commonSettings,
    noUnusedInConsoles,
    name := "payment-service",
    Compile / mainClass := Some("io.github.andy327.switchback.payment.PaymentServer"),
    libraryDependencies ++= catsEffect ++ circe ++ tapirServer ++ config ++ logging,
    serviceAssembly
  )

// ---------------------------------------------------------------------------
// notification-service - pure Kafka consumer (no REST): sends confirmations.
// ---------------------------------------------------------------------------
lazy val notification = project
  .in(file("notification-service"))
  .dependsOn(common)
  .settings(
    commonSettings,
    noUnusedInConsoles,
    name := "notification-service",
    Compile / mainClass := Some("io.github.andy327.switchback.notification.NotificationApp"),
    libraryDependencies ++= catsEffect ++ circe ++ fs2Kafka ++ config ++ logging,
    libraryDependencies ++= kafkaItDeps,
    serviceAssembly
  )

// ---------------------------------------------------------------------------
// audit-log-service - pure Kafka consumer (no REST): appends an immutable event log.
// ---------------------------------------------------------------------------
lazy val auditLog = project
  .in(file("audit-log-service"))
  .dependsOn(common)
  .settings(
    commonSettings,
    noUnusedInConsoles,
    name := "audit-log-service",
    Compile / mainClass := Some("io.github.andy327.switchback.audit.AuditLogApp"),
    libraryDependencies ++= catsEffect ++ circe ++ fs2Kafka ++ config ++ logging,
    libraryDependencies ++= kafkaItDeps,
    serviceAssembly
  )

lazy val root = project
  .in(file("."))
  .aggregate(common, order, inventory, payment, notification, auditLog)
  .settings(
    name := "switchback-checkout",
    assembly / aggregate := false
  )
