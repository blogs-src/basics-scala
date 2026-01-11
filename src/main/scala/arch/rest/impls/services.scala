package arch
package rest

import smithy_rest.wallet_ops as wops

import com.wallet.demo.clustering.rpc.admin as padmin

import logstage.LogIO

import io.scalaland.chimney.dsl.*

import org.typelevel.otel4s.context.propagation.*
//import org.typelevel.otel4s.context.propagation.TextMapGetter.given
//import org.typelevel.otel4s.context.propagation.TextMapGetter.forMapLike
import org.typelevel.otel4s.trace.Span
import org.typelevel.otel4s.trace.Tracer
//import org.typelevel.otel4s.oteljava.context._

import cats.*
import cats.effect.*
import cats.mtl.*
//import cats.FlatMap
import cats.syntax.flatMap.*
import cats.syntax.functor.*

class WalletServiceImpl[F[_]](
  client:  com.wallet.demo.clustering.rpc.admin.WalletCommandRpcServiceFs2Grpc[F, Map[String, String]],
)(
  using F: Async[F],
  FR:      Raise[F, ServiceError],
  M:       Monad[F],
  MT:      MonadThrow[F])
    extends WalletService[F] {

   def getBalance(
     id:         wops.RequestId,
   )(
     using span: Span[F],
     log:        LogIO[F],
     tracer:     Tracer[F],
   ): F[wops.Balance] = Tracer[F].span("send-request").surround {
        val r = id.transformInto[padmin.RequestId]
        for {
           traceHeaders <- Tracer[F].propagate(Map.empty[String, String])
           _ <- log.info("Sending getBalance to GRPC server")
           x <- client.getBalance(padmin.GetBalanceRequest(Some(r)), traceHeaders)
        }
        yield x.transformInto[wops.Balance]
   }
}

class WalletServiceImpl2[F[_]](
  client:  WalletServiceIO[F],
)(
  using F: Async[F],
  FR:      Raise[F, ServiceError],
  M:       Monad[F],
  MT:      MonadThrow[F])
    extends WalletService[F] {

   def getBalance(
     id:         wops.RequestId,
   )(
     using span: Span[F],
     log:        LogIO[F],
     tracer:     Tracer[F],
   ): F[wops.Balance] = Tracer[F].span("send-request").surround {
        val r = id.transformInto[padmin.RequestId]
        for {
           traceHeaders <- Tracer[F].propagate(Map.empty[String, String])
           _ <- log.info("Sending getBalance to GRPC server")
           x <-
             client.getBalance(id.value)(
               using traceHeaders)
        }
        yield x.transformInto[wops.Balance]
   }
}
