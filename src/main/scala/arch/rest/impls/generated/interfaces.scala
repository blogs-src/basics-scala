package arch
package rest

import smithy_rest.wallet_ops as wops


import com.wallet.proto.messages.commands as cmds
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

trait WalletService[F[_]]:
  def getBalance(id: wops.RequestId)(using span: Span[F], log: LogIO[F], tracer: Tracer[F]): F[wops.Balance]

