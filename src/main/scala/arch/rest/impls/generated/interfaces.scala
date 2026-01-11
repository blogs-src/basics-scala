package arch
package rest

import smithy_rest.wallet_ops as wops

import logstage.LogIO

//import org.typelevel.otel4s.context.propagation.TextMapGetter.given
//import org.typelevel.otel4s.context.propagation.TextMapGetter.forMapLike
import org.typelevel.otel4s.trace.Span
import org.typelevel.otel4s.trace.Tracer
//import org.typelevel.otel4s.oteljava.context._

//import cats.FlatMap

trait WalletService[F[_]] {

   def getBalance(
     id: wops.RequestId,
   )(
     using span: Span[F],
     log: LogIO[F],
     tracer: Tracer[F],
   ): F[wops.Balance]
}
