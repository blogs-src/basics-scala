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
    val clientOptions = ClientOptions.default
    def mkMetadata(headers: Map[String, String]): F[io.grpc.Metadata] = {
      val metadata = new io.grpc.Metadata()
      val key = io.grpc.Metadata.Key.of("my-header", io.grpc.Metadata.ASCII_STRING_MARSHALLER)
      metadata.put(key, "my-value")
      F.pure{metadata}
    }

    Dispatcher.parallel[F].use { dispatcher =>
      val c: com.wallet.demo.clustering.rpc.admin.WalletCommandRpcServiceFs2Grpc[F, Map[String, String]] = padmin.WalletCommandRpcServiceFs2Grpc.mkClient[F, Map[String, String]](dispatcher, channel, mkMetadata)
      val x = c.getBalance(padmin.GetBalanceRequest(Some(padmin.RequestId(Some("b")))), Map("name" -> "yo"))
      val mf = summon[Functor[F]]
      mf.map[cmds.Balance, wops.Balance](x)((x1: cmds.Balance) => x1.transformInto[wops.Balance])
    }
    }


