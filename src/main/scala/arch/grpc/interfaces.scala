package arch

import io.grpc.Metadata

import com.wallet.demo.clustering.rpc.admin.*
import com.wallet.proto.messages.commands


trait ExceptionGenerator[F]:
  def generateException(msg: String): Throwable

object ExceptionGenerator:
  def apply[F](using obj: ExceptionGenerator[F]): ExceptionGenerator[F] = obj

trait ClusteringWalletGrpcService[F[_]] {
  def getBalance(request: GetBalanceRequest, ctx: Metadata): F[commands.Balance]
}

trait WalletServiceIO[F[_]]:
  def getBalance(id: String): F[Domain.Balance]


