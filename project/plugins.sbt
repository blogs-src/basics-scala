import PluginsDependencies.V

resolvers += Resolver.mavenLocal

//resolvers += "Akka library repository".at("https://repo.akka.io/maven")
//addSbtPlugin("com.lightbend.akka.grpc" % "sbt-akka-grpc" % "2.4.3")

addSbtPlugin(
  "org.typelevel" % "sbt-fs2-grpc" % V.sbt_fs2_grpc)

addSbtPlugin(
  "ch.epfl.scala" % "sbt-scalafix" % V.sbt_scalafix)

addSbtPlugin(
  "org.scalameta" % "sbt-scalafmt" % V.sbt_scalafmt)

addSbtPlugin(
  "com.thesamet" % "sbt-protoc" % V.sbt_protoc)

libraryDependencies += "com.thesamet.scalapb" %% "compilerplugin" % V.scalapbCompiler

// https://scalapb.github.io/docs/validation
// https://scalapb.github.io/docs/validation#unboxing-required-fields
libraryDependencies ++= Seq(
  "com.thesamet.scalapb" %% "compilerplugin"           % V.scalapbCompiler,
  "com.thesamet.scalapb" %% "scalapb-validate-codegen" % "0.3.6",
)

addSbtPlugin(
  "com.disneystreaming.smithy4s" % "smithy4s-sbt-codegen" % V.smithy4s_sbt_codegen)

addSbtPlugin(
  "com.github.sbt" % "sbt-avro" % V.sbt_avro)

// Java sources compiled with one version of Avro might be incompatible with a
// different version of the Avro library. Therefore we specify the compiler
// version here explicitly.
libraryDependencies += "org.apache.avro" % "avro-compiler" % V.avro_compiler

// https://www.wartremover.org/doc/install-setup.html
// addSbtPlugin("org.wartremover" % "sbt-wartremover" % "3.1.8")

addDependencyTreePlugin

// project/plugins.sbt
addSbtPlugin("ch.epfl.scala" % "sbt-debug-adapter" % "1.0.0")
// project/project/plugins.sbt
addSbtPlugin("com.github.sbt" % "sbt-jdi-tools" % "1.2.0")

addSbtPlugin("com.github.sbt" % "sbt-javaagent" % "0.1.8")

