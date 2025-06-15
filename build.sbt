import Dependencies.V
import Dependencies.Deps
import Dependencies.HybridDeps

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
  ),
  scalacOptions ++=
    Seq(
      "-Yretain-trees",
    ),
//  ThisBuild / resolvers += Resolver.mavenLocal,

//  ThisBuild / resolvers += "Akka library repository".at("https://repo.akka.io/maven"),
//  ThisBuild / resolvers += "Confluent Maven Repository".at("https://packages.confluent.io/maven/"),
  ThisBuild / resolvers += "local-reposilite".at("http://localhost:8080/releases"),

)

lazy val appSettings = Seq(
  scalaVersion := V.scalaLatestVersion,
  dependencyOverrides ++= Seq(
  ),
  scalacOptions ++=
    Seq(
      "-explain",
      "-Wsafe-init",
      "-deprecation",
      "-feature",
//      "-Yretain-trees",
      "-Xmax-inlines",
      "50",
      // "-Yexplicit-nulls",
      // "-Wunused:all",
    )
  // ) ++ Seq("-new-syntax", "-rewrite")
  // ) ++ Seq("-rewrite", "-indent")
//   ) ++ Seq("-rewrite", "-source", "3.7-migration")
)

def mapGen(name: String) = {
  import scala.collection.mutable.HashMap

  val m = new HashMap[String, String]
  sys.env.get("VARIABLES") match{
    case Some(variables) => variables.split(",").foreach { v =>
      val res = (v -> sys.env.get(s"${name.toUpperCase()}_$v").get)
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

//     PB.protocVersion := "3.25.2",
    //https://repo1.maven.org/maven2/com/google/protobuf/protoc/4.31.1/protoc-4.31.1-linux-x86_64.exe
    //  100.0% [##########] 9.7 MiB (16.2 MiB / s)
    PB.protocVersion := "4.31.1",
//      PB.protocVersion := "4.29.2",
    // fs2GrpcOutputPath := (Compile / baseDirectory).value / "src/main/scala/fs2-grpc",
    // scalapbProtobufDirectory := (Compile / baseDirectory).value / "src/main/scala/scalapb",
  )

lazy val root = project
  .in(file("."))
  .enablePlugins(JavaAgent)
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
    libraryDependencies ++= HybridDeps,
    javaAgents += "io.opentelemetry.javaagent" % "opentelemetry-javaagent" % "2.16.0" % "runtime;dist",
//    javaOptions += "-Dotel.java.global-autoconfigure.enabled=true",
  )
  .dependsOn(grpcApi)
  // .aggregate(grpcApi)


val scenario1 = Seq(
  "import arch.WalletOperations.*",
  "init",
)

val scenario2 = Seq(
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

  case "scenario4" =>
    TaskKey[Unit]("r") := (root / Compile / runMain)
      .toTask(" components.examples.run")
      .value

  case _ =>
    TaskKey[Unit]("r") := (root / Compile / runMain)
      .toTask(" nope")
      .value

}

// https://www.scala-sbt.org/1.x/docs/Howto-Logging.html
// sbt --debug
