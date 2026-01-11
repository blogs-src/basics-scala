package arch

import io.grpc.Metadata

import com.wallet.demo.clustering.rpc.admin.*
import com.wallet.proto.messages.commands

import org.typelevel.otel4s.trace.Tracer
import org.typelevel.otel4s.trace.Span

import logstage.LogIO

trait ExceptionGenerator[F] {
   def generateException(msg: String): Throwable
}

object ExceptionGenerator {

   def apply[F](
     using obj: ExceptionGenerator[F],
   ): ExceptionGenerator[F] = obj
}

trait ClusteringWalletGrpcService[F[_]] {
   def getBalance(request: GetBalanceRequest, ctx: Metadata): F[commands.Balance]
}

trait ClusteringWalletGrpcService2[F[_]] {

   def getBalance(
     request: GetBalanceRequest,
     ctx: Metadata,
   )(
     using span: Span[F],
     log: LogIO[F],
     tracer: Tracer[F],
   ): F[commands.Balance]
}

trait WalletServiceIO[F[_]] {

   def getBalance(
     id: String,
   )(
     using metadata: Map[String, String] = Map.empty,
   ): F[Domain.Balance]
}
