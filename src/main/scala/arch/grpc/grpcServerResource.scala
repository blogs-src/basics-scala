package arch

import cats.data.EitherT
import cats.effect.*
import com.wallet.demo.clustering.rpc.admin.*
import fs2.grpc.syntax.all.*
import io.grpc.*
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder
//import io.grpc.okhttp.OkHttpServerBuilder
import io.grpc.protobuf.services.ProtoReflectionServiceV1
// import io.scalaland.chimney.*

import org.typelevel.otel4s.trace.Tracer

class GrpcServerResource:

  def helloService[G: ExceptionGenerator]
  (
    wService: WalletServiceIO2[Result],
//    tracer: Tracer[Result],
  ): Resource[IO, ServerServiceDefinition] = {
    val transformers = new MyTransformers
//    given t: Tracer[Result] = tracer
    val sImpl = new ClusteringWalletGrpcServiceImpl[Result, G](wService)(using transformers)
    WalletCommandRpcServiceFs2Grpc.bindServiceResource[cats.effect.IO](
      new ClusteringWalletFs2GrpcServiceImpl[G](sImpl, transformers)
    )
  }

  //    def run[F[_]: Async](service: ServerServiceDefinition): Resource[F, Server] =
  def run[F[_] : Async](service: ServerServiceDefinition): Resource[F, Server] = {
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
