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

trait WalletService[F[_]: Functor]:
  def getBalance(id: wops.RequestId)(span: Span[F], log: LogIO[F], tracer: Tracer[F]): F[wops.Balance]

class WalletServiceImpl[F[_]: Functor](channel: io.grpc.ManagedChannel)(using F: Async[F], FR: Raise[F, ServiceError], M: Monad[F], MT: MonadThrow[F])
  extends WalletService[F]:

  def getBalance(id: wops.RequestId)(span: Span[F], log: LogIO[F], tracer: Tracer[F]): F[wops.Balance] = {
    val resX = padmin.WalletCommandRpcServiceGrpc.WalletCommandRpcServiceBlockingStub(channel).getBalance(padmin.GetBalanceRequest(Some(padmin.RequestId(Some("b")))))
    val clientOptions = ClientOptions.default
    def mkMetadata(headers: Map[String, String]): F[io.grpc.Metadata] = {
      val metadata = new io.grpc.Metadata()
      val key = io.grpc.Metadata.Key.of("my-header", io.grpc.Metadata.ASCII_STRING_MARSHALLER)
      metadata.put(key, "my-value")
      F.pure{metadata}
    }

    val c = padmin.WalletCommandRpcServiceFs2Grpc.mkClient[F, Map[String, String]](???, channel, mkMetadata)
    val rr: F[cmds.Balance] = c.getBalance(padmin.GetBalanceRequest(Some(padmin.RequestId(Some("b")))), Map("name" -> "yo"))
//    ff.map[cmds.Balance, wops.Balance](rr, xr => wops.Balance(3))
//    .flatMap(res1 => res1.transformInto[wops.Balance])
//    Dispatcher.parallel[F].use { dispatcher =>
//      val c = padmin.WalletCommandRpcServiceFs2Grpc.mkClient[F, Map[String, String]](dispatcher, channel, mkMetadata, clientOptions)
//      c.getBalance(padmin.GetBalanceRequest(Some(padmin.RequestId(Some("b")))), Map("name" -> "yo"))
//      .flatMap(res1 => res1.transformInto[wops.Balance])
//    }
//    ???
    
    }


