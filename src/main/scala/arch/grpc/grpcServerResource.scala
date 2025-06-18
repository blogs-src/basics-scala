package arch

import cats.data.EitherT
import cats.effect.*
import com.wallet.demo.clustering.rpc.admin.*
import fs2.grpc.syntax.all.*
import io.grpc.*
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.sdk.trace.`export`.BatchSpanProcessor
import org.typelevel.otel4s.Otel4s
import org.typelevel.otel4s.oteljava.OtelJava
//import io.grpc.okhttp.OkHttpServerBuilder
import io.grpc.protobuf.services.ProtoReflectionServiceV1
// import io.scalaland.chimney.*

import org.typelevel.otel4s.Attribute
import org.typelevel.otel4s.trace.Tracer

import _root_.io.opentelemetry.sdk.OpenTelemetrySdk
import _root_.io.opentelemetry.api.OpenTelemetry
import _root_.io.opentelemetry.sdk.trace.SdkTracerProvider
import _root_.io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator
import _root_.io.opentelemetry.context.propagation.ContextPropagators
import _root_.io.opentelemetry.sdk.trace.`export`.SimpleSpanProcessor

import cats.effect.kernel.Resource
import cats.~>
import cats.arrow.FunctionK
import cats.syntax.all.*

import cats.mtl.*
import org.typelevel.otel4s.trace.Tracer

class GrpcServerResource:

//  def ioToResult[T](io: IO[T]): Result[T] = ???

  def convertResource[F[_] : MonadCancelThrow, G[_] : MonadCancelThrow, A](resource: Resource[F, A], nt: F ~> G): Resource[G, A] = {
    resource.mapK(nt)
  }

//  val optionToList: Option ~> List = [A] => (a: Option[A]) => a.toList

  val optionToList: Option ~> List = new FunctionK[Option, List]{
    def apply[A](fa: Option[A]): List[A] = ???
  }

  val ioToResult: IO ~> Result = new FunctionK[IO, Result]{
    def apply[A](fa: IO[A]): Result[A] = EitherT.right(fa)
  }

  val resultToIO: Result ~> IO = new FunctionK[Result, IO]{
    def apply[A](fa: Result[A]): IO[A] ={
      fa.foldF(
        error => IO.raiseError(error),
        value => IO {
          value
        }
      )
    }
  }

  def createService[G: ExceptionGenerator]
  (
    wService: WalletServiceIO[Result],
//    tracer: Tracer[Result],
  )/*: Resource[IO, ServerServiceDefinition]*/ = {
    val transformers = new MyTransformers
    //    given t: Tracer[Result] = tracer
    val resOtel = makeOtel
//    val res: Resource[IO, ServerServiceDefinition] = WalletCommandRpcServiceFs2Grpc.bindServiceResource[cats.effect.IO](
//      new ClusteringWalletFs2GrpcServiceImpl[G](sImpl, transformers)
//    )
//    val res2: Resource[Result, ServerServiceDefinition] = convertResource(res, ioToResult)
    val rx : Resource[Result, (ServerServiceDefinition, Tracer[Result])]= resOtel.flatMap{ (tracer: Tracer[Result]) =>

          val sImpl = new ClusteringWalletGrpcServiceImpl[Result, G](wService, tracer)(using transformers)
          val res: Resource[IO, ServerServiceDefinition] = WalletCommandRpcServiceFs2Grpc.bindServiceResource[cats.effect.IO](
            new ClusteringWalletFs2GrpcServiceImpl[G](sImpl, transformers, tracer)
          )
          convertResource(res, ioToResult).map( x => (x, tracer))
        }
    val rx2: Resource[IO, (ServerServiceDefinition, Tracer[Result])] = convertResource(rx, resultToIO)

//    res
    rx2

  }

  //    def run[F[_]: Async](service: ServerServiceDefinition): Resource[F, Server] =
  def createIO[F[_] : Async](service: ServerServiceDefinition): Resource[F, Server] = {
    //      val creds = TlsServerCredentials.create(certChainFile, privateKeyFile)
    //      val creds = InsecureServerCredentials.create()

    NettyServerBuilder
      .forPort(8090)
      //      OkHttpServerBuilder
      //      .forPort(8090, creds)
      .addService(service)
      .addService(ProtoReflectionServiceV1.newInstance())
      .resource[F]
  }
  // otel4s

//  def tracerR: Resource[IO, Tracer[IO]] =
//    OtelJava.autoConfigured[IO]().evalMap(_.tracerProvider.get("Example"))

  def makeOtel: Resource[Result, Tracer[Result]] = {
    val jaegerEndpoint = "http://localhost:4317"
    import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter
    import java.util.concurrent.TimeUnit

    import org.typelevel.otel4s.oteljava.context.IOLocalContextStorage
    import org.typelevel.otel4s.oteljava.context.Context
    import org.typelevel.otel4s.context.LocalProvider

    given LocalProvider[Result, Context] =
      IOLocalContextStorage.localProvider[Result]

    val serviceNameResource = io.opentelemetry.sdk.resources.Resource.create(
      Attributes.of(io.opentelemetry.semconv.ServiceAttributes.SERVICE_NAME,
        "otel-basic-app"))
    val jaegerOtlpExporter = OtlpGrpcSpanExporter.builder.setEndpoint(jaegerEndpoint).setTimeout(30, TimeUnit.SECONDS).build
    val sdkTracerProvider = SdkTracerProvider.builder()
      .addSpanProcessor(BatchSpanProcessor.builder(jaegerOtlpExporter).build)
      .setResource(io.opentelemetry.sdk.resources.Resource.getDefault.merge(serviceNameResource))
      .build()
    val sdk: OpenTelemetrySdk =
      OpenTelemetrySdk.builder()
        .setTracerProvider(sdkTracerProvider)
        .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
        .build()
    val z: Result[OpenTelemetrySdk] = EitherT.right(IO.pure(sdk))
    OtelJava.resource(z).evalMap(_.tracerProvider.get("Example"))
//    OtelJava.autoConfigured[IO]().evalMap(_.tracerProvider.get("Example"))

  }
  // otel4s-END
