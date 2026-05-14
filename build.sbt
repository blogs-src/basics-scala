import Dependencies.V
import Dependencies.Deps
import Dependencies.HybridDeps

import scala.collection.mutable

// format: off

lazy val autoImportSettings = Seq(
  scalacOptions ++= Seq(
    Seq(
    "java.lang",
    "scala",
    "scala.Predef",
    "scala.util.chaining",
    "akka.actor.typed",
    "scala.concurrent",
    "akka.actor.typed.scaladsl",
    "akka.actor.typed.scaladsl.AskPattern",
    "scala.concurrent.duration",
    "akka.util",
    "scala.util",

    // "cats.implicits",
    // "cats",
    // "cats.effect",
  )
    .mkString(start = "-Yimports:", sep = ",", end = ""),
  )

)

lazy val commonSettings = Seq(
  update / evictionWarningOptions := EvictionWarningOptions.empty,
  scalaVersion := V.scalaLTSVersion,
  organization := "org",
  organizationName := "Demos",
  semanticdbEnabled := true, // enable SemanticDB
  ThisBuild / evictionErrorLevel := Level.Info,
  dependencyOverrides ++= Seq(
    "com.fasterxml.jackson.core"     % "jackson-databind"                  % "2.19.0",
    "com.fasterxml.jackson.core"     % "jackson-core"                  % "2.19.0",
  ),
  scalacOptions ++=
    Seq(
      "-Yretain-trees",
    ),
//  ThisBuild / resolvers += Resolver.mavenLocal,

//  ThisBuild / resolvers += "Akka library repository".at("https://repo.akka.io/maven"),
//  ThisBuild / resolvers += "Confluent Maven Repository".at("https://packages.confluent.io/maven/"),

//  ThisBuild / resolvers += "local-reposilite".at("http://localhost:8080/releases"),

//  ThisBuild / resolvers += "default".at("http://localhost:8080/releases"),
//  ThisBuild / resolvers += "public".at("http://localhost:8080/releases"),
//  ThisBuild / resolvers -= "public".at("https://repo1.maven.org/maven2/"),
//  ThisBuild / externalResolvers := Resolver.combineDefaultResolvers(resolvers.value.toVector, mavenCentral = false, jcenter = false),

  ThisBuild / externalResolvers := Seq(
//    "default".at("http://localhost:8080/releases"),
//    "public".at("http://localhost:8080/releases"),
  ),

)

lazy val appSettings = Seq(
  scalaVersion := V.scalaLatestVersion,
  dependencyOverrides ++= Seq(
  ),
  scalacOptions ++=
    Seq(
      "-explain",
      "-Wsafe-init",
      "-deprecation",     // show deprecation warnings
//      "-unchecked",       // additional warnings
//      "-Xfatal-warnings", // treat warnings as errors
      "-feature",
      "-Xmax-inlines",
      "50",
      "-source",
      "future",
      // "-Yexplicit-nulls",
    ) ++ scalacOptionsValue
)

lazy val scalacOptionsCustom = sys.env.getOrElse("SCALAC_OPTIONS", "default")
lazy val scalacOptionsValue = scalacOptionsCases(scalacOptionsCustom)

// format: off
lazy val scalacOptionsCases = Map(
  // https://docs.scala-lang.org/scala3/guides/migration/tooling-syntax-rewriting.html
  "default"            -> Seq[String](),
  "new_syntax"         -> Seq[String]("-new-syntax",  "-rewrite"),
  "new_syntax_updated" -> Seq[String]("-new-syntax",  "-rewrite", "-source", "future-migration"),
  "indent"             -> Seq[String]("-Wunused:all", "-indent",  "-rewrite"),
  "no-indent"          -> Seq[String]("-no-indent",  "-rewrite"),
  "3_7_migration"      -> Seq[String]("-rewrite",     "-source",  "3.7-migration"),
)
// format: on

def mapGen(name: String) = {
  val m = new mutable.HashMap[String, String]
  sys.env.get("VARIABLES") match {
    case Some(variables) => variables.split(",").foreach { v =>
      val res = v -> sys.env(s"${name.toUpperCase()}_$v")
      println(s"Adding env-var: '${res._1}' with value '${res._2}'")
      m += res
    }
    case None =>
  }
  m.toMap
}

lazy val grpcApi = project
  .in(file("modules/grpc-api"))
  .enablePlugins(Fs2Grpc)
  .disablePlugins(ScalafixPlugin)
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
      Deps.grpc,
      Deps.scalapbCommonProtos,
      Deps.scalapbProtobufu,
      "com.thesamet.scalapb" %% "scalapb-validate-core" % scalapb.validate.compiler.BuildInfo.version % "protobuf",
    ),

//    Compile / PB.targets := Seq(
//      scalapb.gen() -> (Compile / sourceManaged).value / "scalapb"
//    ),

    // https://github.com/typelevel/fs2-grpc/issues/489
//    scalapbCodeGeneratorOptions += CodeGeneratorOption.Fs2Grpc,
//    scalapbCodeGeneratorOptions += CodeGeneratorOption.FlatPackage,
//    Compile / PB.targets := scalapbCodeGenerators.value
//      .map(_.copy(outputPath = (Compile / sourceManaged).value / "scala")) //intellij specific
//      .:+(scalapb.validate.gen(GeneratorOption.FlatPackage) -> (Compile / sourceManaged).value / "scala": protocbridge.Target),

    //https://repo1.maven.org/maven2/com/google/protobuf/protoc/
    //https://repo1.maven.org/maven2/com/google/protobuf/protoc/4.31.1/protoc-4.31.1-linux-x86_64.exe
    //  100.0% [##########] 9.7 MiB (16.2 MiB / s)
    PB.protocVersion := "4.34.1",
    // fs2GrpcOutputPath := (Compile / baseDirectory).value / "src/main/scala/fs2-grpc",
    // scalapbProtobufDirectory := (Compile / baseDirectory).value / "src/main/scala/scalapb",
  )

lazy val tapirVersion = "1.13.19"

lazy val restApi = project
  .in(file("modules/rest-api"))
  .enablePlugins(Smithy4sCodegenPlugin)
  .disablePlugins(ScalafixPlugin)
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
      "com.disneystreaming.smithy4s" %% "smithy4s-http4s"         % smithy4sVersion.value,
      "com.disneystreaming.smithy"   %  "smithytranslate-traits"  % V.smithytranslateTraitsVersion,
    ),
    // Compile / smithy4sOutputDir := (Compile / baseDirectory).value / "src/main/scala/smithy",
  )

//"org.tpolecat"  %% "skunk-core"            % "1.0.0",
lazy val root = project
  .in(file("."))
  //.enablePlugins(JavaAgent)
  .settings(autoImportSettings)
  .settings(commonSettings)
  .settings(appSettings)
  .settings(
    // https://www.wartremover.org/doc/warts.html
    // Compile / compile / wartremoverErrors ++= Warts.all,
    // Compile / compile / wartremoverWarnings ++= Warts.all,
    name := "scala-basics",
    scalaVersion := V.scalaLatestVersion,
    // scalafmtOnCompile := true,
    Compile / run / fork := true,
    Compile / console / initialCommands += scenarioInititalCommands.mkString(";\n"),
    mainClass := Some("demo.examples.run"),
    // Compile / discoveredMainClasses := Seq(),
    Compile / run / javaOptions ++= {
      val props = sys.props.toList
      props.filter(
        (p: (String, String)) => p._1 == "config.file"
      ).map {
        case (key, value) => s"""-D$key="$value""""
      }
    },
    libraryDependencies ++= HybridDeps ++ List(
      "dev.optics" %% "monocle-core"  % "3.3.0" withSources(),
      "dev.optics" %% "monocle-macro" % "3.3.0" withSources(),
      "dev.optics" %% "monocle-unsafe" % "3.3.0" withSources(),
      "dev.optics" %% "monocle-state" % "3.3.0" withSources(),
      "dev.optics" %% "monocle-refined" % "3.3.0" withSources(),
      "dev.optics" %% "monocle-law" % "3.3.0" withSources(),
      "com.softwaremill.magnolia1_3" %% "magnolia" % "1.3.20" withSources(),
      "org.parboiled" %% "parboiled" % "2.5.1" withSources(),
      "org.typelevel" %% "squants" % "1.8.3" withSources(),
      "com.monovore" %% "decline" % "2.6.2" withSources(),
      "com.monovore" %% "decline-effect" % "2.6.2" withSources(),
      "com.monovore" %% "decline-refined" % "2.6.2" withSources(),
      "com.softwaremill.ox" %% "core" % "1.0.4" withSources(),

      "org.tpolecat" %% "doobie-postgres" % "1.0.0-RC12" withSources(),
      "org.tpolecat" %% "doobie-postgres-circe" % "1.0.0-RC12" withSources(),
      "net.postgis" % "postgis-jdbc" % "2025.1.1" withSources(),

//          "io.getkyo" %% "kyo-core" % "3.0.7",
      "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui" % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-core" % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-json-circe" % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-server" % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-openapi-docs" % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-http4s-server" % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-sttp-client" % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle" % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-apispec-docs" % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-json-zio" % tapirVersion % Test,
      "com.softwaremill.sttp.tapir" %% "tapir-tests" % tapirVersion % Test,
      "com.softwaremill.sttp.tapir" %% "tapir-cats" % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-cats-effect" % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-sttp-client4" % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-netty-server" % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-sttp-stub-server" % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-enumeratum" % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-files" % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-iron" % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-jsoniter-scala" % tapirVersion % Test,
      "com.softwaremill.sttp.tapir" %% "tapir-prometheus-metrics" % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-client" % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-http4s-client" % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-redoc-bundle" % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-refined" % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-json-json4s" % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-netty-server-sync" % tapirVersion,

//      "org.apache.avro" % "avro-tools" % "1.12.1",
//      "org.business4s" %% "workflows4s-core" % "0.6.0" withSources(),


      "org.typelevel" %% "laika-core" % "1.3.2",
      "org.typelevel" %% "laika-io" % "1.3.2",
      "org.typelevel" %% "laika-pdf" % "1.3.2",

      "org.typelevel" %% "squants" % "1.8.3",
      "org.typelevel" %% "cats-tagless-core" % "0.16.5",


//      "com.typesafe" % "config" % "1.4.8",

//      "com.github.pureconfig" %% "pureconfig-core" % "0.17.10",
//      "com.github.pureconfig" %% "pureconfig-generic-scala3" % "0.17.10",
//      "com.github.pureconfig" %% "pureconfig-cats-effect" % "0.17.10",
//      "com.github.pureconfig" %% "pureconfig-cats" % "0.17.10",
//      "com.github.pureconfig" %% "pureconfig-generic-base" % "0.17.10",
//      "com.github.pureconfig" %% "pureconfig-http4s" % "0.17.10",
//      "com.github.pureconfig" %% "pureconfig-squants" % "0.17.10",
//      "com.github.pureconfig" %% "pureconfig-yaml" % "0.17.10",

      "com.beachape" %% "enumeratum" % "1.9.7",

      "is.cir" %% "ciris" % "3.14.1",
      "is.cir" %% "ciris-circe" % "3.14.1",
      "is.cir" %% "ciris-circe-yaml" % "3.14.1",
      "is.cir" %% "ciris-enumeratum" % "3.14.1",
      "is.cir" %% "ciris-http4s" % "3.14.1",
      "is.cir" %% "ciris-refined" % "3.14.1",
      "is.cir" %% "ciris-squants" % "3.14.1",
      "lt.dvim.ciris-hocon" %% "ciris-hocon" % "1.3.0",

      "com.github.cb372" %% "cats-retry" % "4.0.0",
      "com.github.cb372" %% "cats-retry-mtl" % "4.0.0",

      "dev.profunktor" %% "http4s-jwt-auth" % "2.0.15",
      "dev.profunktor" %% "redis4cats-effects" % "2.0.3",
      "dev.profunktor" %% "redis4cats-log4cats" % "2.0.3",
      "org.typelevel" %% "weaver-cats" % "0.12.0" % Test,
      "org.typelevel" %% "weaver-discipline" % "0.12.0" % Test,
      "org.typelevel" %% "weaver-scalacheck" % "0.12.0" % Test,

    ),
    Compile / run / javaOptions += "-Dcats.effect.trackFiberContext=true",
    Compile / run / javaOptions += "-Dotel.java.global-autoconfigure.enabled=false",
  )
  .dependsOn(grpcApi)
  .dependsOn(restApi)


val scenario1 = Seq(
  "import arch.WalletOperations.*",
  "init",
)

val scenario2 = Seq(
//  "import arch.rest.*",
//  "init",
)

// val scenario8 = Seq(
//   "import components.infrastructure.cluster.WalletOperations.*",
//   "init",
// )

lazy val selectedScenario = sys.env.getOrElse("SCENARIO", "scenario1")
lazy val scenarioInititalCommands = scenarios(selectedScenario)

lazy val scenarios = Map(
  "scenario1" -> scenario1,
  "scenario2" -> scenario2,
)

ThisBuild / watchTriggeredMessage := Watch.clearScreenOnTrigger

selectedScenario match {

  case "scenario1" =>
    TaskKey[Unit]("r") := (root / Compile / runMain)
      .toTask(" arch.rest.Main")
      .value

  case _ =>
    TaskKey[Unit]("r") := (root / Compile / runMain)
      .toTask(" nope")
      .value

}

// https://www.scala-sbt.org/1.x/docs/Howto-Logging.html
// sbt --debug
