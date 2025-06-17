package arch

import cats.data.EitherT
import cats.effect.*
import com.wallet.demo.clustering.rpc.admin.*
import io.grpc.*
import io.scalaland.chimney.dsl.*
import cats.mtl.*
import com.wallet.proto.messages.commands
import org.typelevel.otel4s.trace.Tracer

class ClusteringWalletFs2GrpcServiceImpl[G: ExceptionGenerator](service: ClusteringWalletGrpcService[Result], transformers: MyTransformers[G], tracer: Tracer[Result])
    extends WalletCommandRpcServiceFs2Grpc[cats.effect.IO, Metadata] {
  import transformers.othersTransformers

  def getBalance(request: GetBalanceRequest, ctx: Metadata) = service.getBalance(request, ctx).transformInto[IO[commands.Balance]]

}
