package arch
package rest

import cats.effect.*

import smithy_rest.wallet_ops.*

import cats.syntax.all.*

import org.typelevel.otel4s.Attribute

import io.opentelemetry.api.trace.Span as JSpan

class WalletOpsImpl[F[_]](
  using F: Async[F],
)(
  ser:     WalletService[F],
  info: F[domain.RequestInfo[F]]) extends WalletOpsService[F]:

   def getBalance(id: RequestId): F[Balance] = info.flatMap:
        rinfo =>
           rinfo.tracer.joinOrRoot(rinfo.headers):
                rinfo.tracer.span("Work.DoWork2", Attribute("custom_tag", "aa")).use:
                     span =>
                        println(s"jctx (WalletOpsImpl): ${JSpan.current().getSpanContext}") // get a span from a ThreadLocal
                        println(s"otel4s (WalletOpsImpl): ${span.context}")
                        println(s"User id (WalletOpsImpl): ${rinfo.userId}")

                        val log = auditing.Logger.getLogger("logs-rest").withCustomContext(
                          "traceId" -> span.context.traceIdHex)

                        for
                           _ <- log.info("calling rest_service.getBalance")
                           res <-
                             ser.getBalance(id)(
                               using span,
                               log,
                               rinfo.tracer)
                           _ <- span.addAttribute(Attribute("traceId", span.context.traceIdHex))
                        yield res

   def healthCheck(): F[Unit] =
      val res: F[Unit] =
        for
           d <- info
           _ <- F.pure { println(s"headers: $d") }
        yield ()
      res
