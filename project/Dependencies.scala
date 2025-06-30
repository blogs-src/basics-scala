import sbt.*

object Dependencies {

// format: off
  object V {
    val scalaLTSVersion      = "3.3.6"
    val distage              = "1.2.19"

    val scalaLatestVersion   = "3.7.1"
    val logstage             = distage
    val scalatest            = "3.2.18"
    val scalacheck           = "1.17.0"
    val catsCore             = "2.10.0"
    val zio                  = "2.0.21"
    val zioCats              = "23.0.0.8"
    val circeGeneric         = "0.14.6"
    // val akkaVersion          = "2.9.3"
    val akkaVersion          = "2.9.5" // (Aug 19 2024) from 2.9.6 a license key is required
    // val akkaGrpc             = "2.4.4"
    val akkaGrpc             = "2.4.3"
    val kafkaVersion         = "6.0.0"
    val logbackVersion       = "1.5.18"
    val jacksonVersion       = "2.11.4"
    val jacksonScalaVersion  = "2.18.0"
    val akkaHttpVersion      = "10.6.3"
    val akkaManagement       = "1.5.3"

    val akkaPersistenceR2dbc = "1.2.5"
    // val akkaPersistenceR2dbc = "1.2.4"

    val akkaCassandra        = "1.2.1"
    val akkaProjection       = "1.5.5"
    val cats                 = "2.13.0"
    val catsEffect           = "3.6.1"
    val catsMTL              = "1.5.0"
    val munit                = "1.1.1"
    val munitCatsEffect      = "2.1.0"
    val fs2                  = "3.12.0"
    val iron                 = "3.0.2"
    val grpc                 = "1.73.0"
    val scalapbCommonProtos  = "2.9.6-0"
    val avroCompiler         = "1.12.0"
//    val chimney              = "1.6.0"
    val chimney              = "1.8.1"
    val doobie               = "1.0.0-RC9"
    val skunk                = "1.1.0-M3"
    val postgress            = "42.7.7"
    val commonsCompress      = "1.27.1"
    // https://packages.confluent.io/maven/io/confluent/kafka-avro-serializer/
    val kafkaAvroSerializer  = "8.0.0"
    val smithytranslateTraitsVersion = "0.5.7"
    val http4s                       = "0.23.30"
    val http4s_blaze                 = "0.23.17"
    val scalapb                      = "0.11.18"
    val avroCompilerVersion          = "1.12.0"
    val fs2Kafka                     = "3.8.0"

    val helenus                      = "1.1.0"
    val otel4s                       = "0.12.0"
    val opentelemetry                = "1.51.0"
    val jwt_scala                    = "11.0.0"
    val jose_jwt                     = "10.3"

    val smithy4s                     = "0.18.38"
  }

  object Deps {
    // val doobieRefined     = "org.tpolecat" %% "doobie-refined" % V.doobie
    val commonsCompress            = "org.apache.commons" % "commons-compress"  % V.commonsCompress withSources()
    val postgresql                 = "org.postgresql" %  "postgresql"           % V.postgress withSources()
    val doobieCore                 = "org.tpolecat"  %% "doobie-core"           % V.doobie withSources()
    val doobieHikari               = "org.tpolecat"  %% "doobie-hikari"         % V.doobie withSources()
    val doobiePostgres             = "org.tpolecat"  %% "doobie-postgres"       % V.doobie withSources()
    val doobiePostgresCirce        = "org.tpolecat"  %% "doobie-postgres-circe" % V.doobie withSources()
    val doobieScalatest            = "org.tpolecat"  %% "doobie-scalatest"      % V.doobie % Test withSources()
    val doobieMunit                = "org.tpolecat"  %% "doobie-munit"          % V.doobie % Test withSources()
    val doobieFree                 = "org.tpolecat"  %% "doobie-free"           % V.doobie withSources()

    val skunkRefined               = "org.tpolecat"  %% "skunk-refined"         % V.skunk withSources()
    val skunkPostgis               = "org.tpolecat"  %% "skunk-postgis"         % V.skunk withSources()
    val skunkDocs                  = "org.tpolecat"  %% "skunk-docs"            % V.skunk withSources()
    val skunkCirce                 = "org.tpolecat"  %% "skunk-circe"           % V.skunk withSources()
    val skunkCore                  = "org.tpolecat"  %% "skunk-core"            % V.skunk withSources()

    val logbackClassic             = "ch.qos.logback"  % "logback-classic"                   % V.logbackVersion withSources()
    val requests                   = "com.lihaoyi"    %% "requests"                          % "0.9.0" withSources()
    val json4sNative               = "org.json4s"     %% "json4s-native"                     % "4.0.7" withSources()

    val distageCore                = "io.7mind.izumi" %% "distage-core"                      % V.distage withSources()
    val distageConfig              = "io.7mind.izumi" %% "distage-extension-config"          % V.distage withSources()
    val distagePlugins             = "io.7mind.izumi" %% "distage-extension-plugins"         % V.distage withSources()
    val logstage                   = "io.7mind.izumi" %% "logstage-core"                     % V.distage withSources()
    val logstage_circe             = "io.7mind.izumi" %% "logstage-rendering-circe"          % V.distage withSources()
    val logstage_adapter_slf4j     = "io.7mind.izumi" %% "logstage-adapter-slf4j"            % V.distage withSources()
    val logstage_distage_extension = "io.7mind.izumi" %% "distage-extension-logstage"        % V.distage withSources()
    val logstage_sink_slf4j        = "io.7mind.izumi" %% "logstage-sink-slf4j"               % V.distage withSources()

    val kafkaAvroSerializer        = "io.confluent"    % "kafka-avro-serializer"             % V.kafkaAvroSerializer withSources()

    val akkaSlf4j                  = "com.typesafe.akka"             %% "akka-slf4j"                        % V.akkaVersion withSources()
    // "ch.qos.logback" % "logback-classic" % "1.2.3"

    val akkaActorTyped             = "com.typesafe.akka"             %% "akka-actor-typed"                  % V.akkaVersion withSources()
    val akkaDiscovery              = "com.typesafe.akka"             %% "akka-discovery"                    % V.akkaVersion withSources()
    val akkaKubernetes             = "com.lightbend.akka.discovery"  %% "akka-discovery-kubernetes-api"     % V.akkaManagement withSources()
    val akkaTestkitTyped           = "com.typesafe.akka"             %% "akka-actor-testkit-typed"          % V.akkaVersion % Test withSources()
    val akkaStream                 = "com.typesafe.akka"             %% "akka-stream"                       % V.akkaVersion withSources()
    val akkaStreamKafka            = "com.typesafe.akka"             %% "akka-stream-kafka"                 % V.kafkaVersion withSources()
    val jacksonDatabind            = "com.fasterxml.jackson.core"     % "jackson-databind"                  % V.jacksonVersion withSources()
    val jacksonScalaModule         = "com.fasterxml.jackson.module"  %% "jackson-module-scala"              % V.jacksonScalaVersion withSources()
    val akkaSerializationJackson   = "com.typesafe.akka"             %% "akka-serialization-jackson"        % V.akkaVersion withSources()
    val akkaHttp                   = "com.typesafe.akka"             %% "akka-http"                         % V.akkaHttpVersion withSources()
    val akkaClusterTyped           = "com.typesafe.akka"             %% "akka-cluster-typed"                % V.akkaVersion withSources()
    val akkaClusterSharding        = "com.typesafe.akka"             %% "akka-cluster-sharding-typed"       % V.akkaVersion withSources()
    val akkaClusterBootstrap       = "com.lightbend.akka.management" %% "akka-management-cluster-bootstrap" % V.akkaManagement withSources()
    val akkaClusterHttp            = "com.lightbend.akka.management" %% "akka-management-cluster-http"      % V.akkaManagement withSources()
    val akkaPersistence            = "com.typesafe.akka"             %% "akka-persistence-typed"            % V.akkaVersion withSources()
    val akkaPersistenceCassandra   = "com.typesafe.akka"             %% "akka-persistence-cassandra"        % V.akkaCassandra withSources()
    val akkaPersistenceR2dbc       = "com.lightbend.akka"            %% "akka-persistence-r2dbc"            % V.akkaPersistenceR2dbc withSources()
    val akkaProjectionR2dbc        = "com.lightbend.akka"            %% "akka-projection-r2dbc"             % V.akkaProjection withSources()
    val akkaProjectionCore         = "com.lightbend.akka"            %% "akka-projection-core"              % V.akkaProjection withSources()
    val akkaProjectionEventsourced = "com.lightbend.akka"            %% "akka-projection-eventsourced"      % V.akkaProjection withSources()
    val akkaGrpc                   = "com.lightbend.akka.grpc"       %% "akka-grpc-runtime"                 % V.akkaGrpc withSources()

    val cats                       = "org.typelevel"                 %% "cats-core"                         % V.cats withSources()
    val catsEffect                 = "org.typelevel"                 %% "cats-effect"                       % V.catsEffect withSources()
    val catsMtl                    = "org.typelevel"                 %% "cats-mtl"                          % V.catsMTL withSources()

    val fs2                        = "co.fs2"                        %% "fs2-core"                          % V.fs2 withSources()
    val fs2Io                      = "co.fs2"                        %% "fs2-io"                            % V.fs2 withSources()

    val iron                       = "io.github.iltotore"            %% "iron"                              % V.iron withSources()
    val ironCirce                  = "io.github.iltotore"            %% "iron-circe"                        % V.iron withSources()
    val ironCats                   = "io.github.iltotore"            %% "iron-cats"                         % V.iron withSources()
    val ironDecline                = "io.github.iltotore"            %% "iron-decline"                      % V.iron withSources()

    val grpcNettyShaded            = "io.grpc"                             %  "grpc-netty-shaded"                      % scalapb.compiler.Version.grpcJavaVersion withSources()
    val grpc                       = "io.grpc"                             %  "grpc-services"                          % V.grpc withSources()
    val scalapbCommonProtos        = "com.thesamet.scalapb.common-protos" %% "proto-google-common-protos-scalapb_0.11" % V.scalapbCommonProtos % "protobuf" withSources()

    val scalapbRuntime               = "com.thesamet.scalapb" %% "scalapb-runtime"                            % V.scalapb withSources()
    val scalapbProtobufu             = "com.thesamet.scalapb" %% "scalapb-runtime"                            % V.scalapb % "protobuf" withSources()

    val chimney                    = "io.scalaland"       %% "chimney"                        % V.chimney withSources()
    val chimneyProtobufs           = "io.scalaland"       %% "chimney-protobufs"              % V.chimney withSources()
    val chimneyJavaCollections     = "io.scalaland"       %% "chimney-java-collections"       % V.chimney withSources()

    val http4s_ember_server        = "org.http4s"         %% "http4s-ember-server"            % V.http4s withSources()
    val http4s_blaze_client        = "org.http4s"         %% "http4s-blaze-client"            % V.http4s_blaze withSources()

    val avro                       = "org.apache.avro"     % "avro"                           % V.avroCompilerVersion withSources()

    val fs2Kafka                   = "com.github.fd4s"    %% "fs2-kafka"                      % V.fs2Kafka withSources()
    val munit                      = "org.scalameta"      %% "munit"                          % V.munit % Test withSources()
    val catsMunit                  = "org.typelevel"      %% "munit-cats-effect"              % V.munitCatsEffect % Test withSources()

    // Cassandra
    // https://github.com/nMoncho/helenus3
    val helenus                    = "net.nmoncho"        %% "helenus-core"                   % V.helenus withSources()

    val scala3Compiler             = "org.scala-lang"     %% "scala3-compiler"                % V.scalaLatestVersion withSources()

    val otel4s_inst_metrics        = "org.typelevel"      %% "otel4s-instrumentation-metrics"             % V.otel4s withSources()
    val otel4s                     = "org.typelevel"      %% "otel4s-oteljava"                            % V.otel4s exclude("io.opentelemetry", "opentelemetry-sdk-extension-autoconfigure") withSources()
    val otel4s_otelj_ctx_storage   = "org.typelevel"      %% "otel4s-oteljava-context-storage"            % V.otel4s withSources()
    val opentelemetry_expr_otlp    = "io.opentelemetry"    % "opentelemetry-exporter-otlp"                % V.opentelemetry withSources() // % Runtime
    val opentelemetry_autoconf     = "io.opentelemetry"    % "opentelemetry-sdk-extension-autoconfigure"  % V.opentelemetry withSources() // % Runtime
    val opentelemetry_semconv      = "io.opentelemetry.semconv" % "opentelemetry-semconv"                 % "1.34.0" withSources() // % Runtime

    val jwt_scala                  = "com.github.jwt-scala" %% "jwt-core"                                 % V.jwt_scala withSources()
    val jose_jwt                   = "com.nimbusds"          % "nimbus-jose-jwt"                          % V.jose_jwt withSources()

    val smithy4s_json = "com.disneystreaming.smithy4s" %% "smithy4s-json" % V.smithy4s
  }

// format: on

  val HybridDeps = Seq(
    Deps.commonsCompress,
    Deps.postgresql,
    Deps.doobiePostgresCirce,
    Deps.doobieHikari,
    Deps.doobiePostgres,
    Deps.doobieCore,
    Deps.doobieScalatest,
    Deps.doobieMunit,
    Deps.doobieFree,
    Deps.grpc,
    Deps.grpcNettyShaded,
//    "io.grpc" % "grpc-okhttp" % scalapb.compiler.Version.grpcJavaVersion,
    Deps.scalapbCommonProtos,
    Deps.http4s_ember_server,
    Deps.http4s_blaze_client,
    Deps.chimney,
    Deps.chimneyProtobufs,
    Deps.chimneyJavaCollections,
    Deps.akkaActorTyped,
    Deps.akkaSlf4j,
    Deps.akkaStream,
    Deps.akkaStreamKafka,
    Deps.jacksonDatabind,
    Deps.jacksonScalaModule,
    Deps.akkaSerializationJackson,
    Deps.akkaHttp,
    Deps.akkaClusterTyped,
    Deps.akkaClusterSharding,
    Deps.akkaClusterBootstrap,
    Deps.akkaClusterHttp,
    Deps.akkaPersistence,
    Deps.akkaPersistenceCassandra,
    Deps.akkaPersistenceR2dbc,
    Deps.akkaProjectionR2dbc,
    Deps.akkaProjectionCore,
    Deps.akkaProjectionEventsourced,
    Deps.akkaGrpc,
    Deps.cats,
    Deps.catsEffect,
    Deps.catsMtl,
    Deps.fs2,
    Deps.logbackClassic,
    Deps.requests,
    Deps.json4sNative,
    Deps.distageCore,
    Deps.distageConfig,
    Deps.distagePlugins,

    //    Deps.logstage_adapter_slf4j,
    //    Deps.logstage_sink_slf4j,
    Deps.logstage,
    Deps.logstage_circe,
    Deps.logstage_distage_extension,
    Deps.akkaDiscovery,
    Deps.akkaKubernetes,
    Deps.iron,
    Deps.ironCirce,
    Deps.ironCats,
    Deps.ironDecline,
    Deps.avro,
    Deps.fs2Kafka,
    Deps.kafkaAvroSerializer,
    Deps.munit,
    Deps.catsMunit,
    Deps.helenus,
    Deps.otel4s,
    Deps.otel4s_otelj_ctx_storage,
    Deps.otel4s_inst_metrics,
    Deps.opentelemetry_expr_otlp,
    Deps.opentelemetry_semconv,
    Deps.jwt_scala,
    Deps.jose_jwt,
//    Deps.smithy4s_json, // TODO: check if this is unnecesary
//    Deps.opentelemetry_autoconf,
    // only scala 2
//    "ch.epfl.scala" %% "scala-debug-adapter" % "4.2.5",
  )

}
