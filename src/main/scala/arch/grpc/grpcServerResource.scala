package arch

import cats.data.EitherT
import cats.effect.*
import com.wallet.demo.clustering.rpc.admin.*
import fs2.grpc.syntax.all.*
import io.grpc.*
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder
import io.grpc.protobuf.services.ProtoReflectionService
// import io.scalaland.chimney.*

class GrpcServerResource:

    def helloService[G: ExceptionGenerator]
      (
        wService: WalletServiceIO2[Result],
      ): Resource[IO, ServerServiceDefinition] = {

      val transformers = new MyTransformers
      val sImpl = new ClusteringWalletGrpcServiceImpl[Result, G](wService)(using transformers)
      WalletCommandRpcServiceFs2Grpc.bindServiceResource[cats.effect.IO](
        new ClusteringWalletFs2GrpcServiceImpl[G](sImpl, transformers)
      )
    }

    def run[F[_]: Async](service: ServerServiceDefinition): Resource[F, Server] = NettyServerBuilder
      .forPort(8090)
      .addService(service)
      .addService(ProtoReflectionService.newInstance())
      .resource[F]
