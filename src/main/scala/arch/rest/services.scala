package arch
package rest

import cats.*
import cats.effect.*
import cats.mtl.*
import smithy_rest.wallet_ops as wops


import com.wallet.proto.messages.commands as cmds
import com.wallet.demo.clustering.rpc.admin as padmin

import org.typelevel.otel4s.trace.Tracer
import org.typelevel.otel4s.trace.Span

import scala.jdk.CollectionConverters.*

import logstage.LogIO

import io.scalaland.chimney.dsl.*
import io.grpc.ManagedChannel
import fs2.grpc.client.ClientOptions
import cats.effect.std.Dispatcher

import org.http4s.syntax.literals._
import org.typelevel.otel4s.context.propagation._
import org.typelevel.otel4s.context.propagation.TextMapGetter.given
import org.typelevel.otel4s.context.propagation.TextMapGetter.forMapLike
import org.typelevel.otel4s.trace.Tracer
import org.typelevel.otel4s.oteljava.context._
import scala.collection.mutable as mut

trait WalletService[F[_]: Functor]:
  def getBalance(id: wops.RequestId)(span: Span[F], log: LogIO[F], tracer: Tracer[F]): F[wops.Balance]

class WalletServiceImpl[F[_]: Functor](client: com.wallet.demo.clustering.rpc.admin.WalletCommandRpcServiceFs2Grpc[F, Map[String, String]])(using F: Async[F], FR: Raise[F, ServiceError], M: Monad[F], MT: MonadThrow[F])
  extends WalletService[F]:

  def getBalance(id: wops.RequestId)(span: Span[F], log: LogIO[F], tracer: Tracer[F]): F[wops.Balance] = {
    val hdrs = Map[String, String]().empty
    tracer.span("send-request").surround {
      val traceHeaders = tracer.propagate(hdrs)
      M.flatMap(traceHeaders)( (hd: Map[String, String]) => {
        val x = client.getBalance(padmin.GetBalanceRequest(Some(padmin.RequestId(Some("b")))), hd)
        Functor[F].map(x)((x1: cmds.Balance) => x1.transformInto[wops.Balance])
      })

//      Functor[F].map(traceHeaders)((x1: Map[String, String]) => {
//
//      })

//      val x = client.getBalance(padmin.GetBalanceRequest(Some(padmin.RequestId(Some("b")))), hdrs)
//      Functor[F].map(x)((x1: cmds.Balance) => x1.transformInto[wops.Balance])
    }
  }


