package arch

import cats.data.EitherT
import cats.effect.*
import cats.implicits.*
import com.google.rpc.Code
import com.wallet.demo.clustering.rpc.admin.*
import com.wallet.proto.messages.commands
import cats.mtl.*
import org.typelevel.otel4s.Attribute
//import fs2.concurrent.Channel
import io.scalaland.chimney.Transformer
import io.scalaland.chimney.dsl.*
import io.scalaland.chimney.partial.syntax.*

import cats.*

import akka.grpc.GrpcServiceException

import io.grpc.Metadata

import org.typelevel.otel4s.trace.Tracer
import org.typelevel.otel4s.trace.Span

import scala.jdk.CollectionConverters.*

import io.opentelemetry.api.trace.Span as JSpan
import logstage.LogIO

class ClusteringWalletGrpcServiceImpl2[F[_], G: ExceptionGenerator](
  service:            WalletServiceIO[F],
)(
  using transformers: MyTransformers[G],
)(
  using F:            Async[F],
  FR:                 Raise[F, ServiceError],
  M:                  Monad[F],
  MT:                 MonadThrow[F])
    extends ClusteringWalletGrpcService2[F]:

   import io.scalaland.chimney.partial
   import io.scalaland.chimney.protobufs.*
   import transformers.given

   private def validateRequestId(request: GetBalanceRequest): F[BalanceRequest] =
      val res = request.transformIntoPartial[BalanceRequest].asEither.asResult.asEitherErrorPathMessageStrings
      res match
        case Right(r) => r.pure[F]
        case Left(e)  =>
          val (key, value) = e.toList.head
          FR.raise(ErrorsBuilder.badRequestError(s"$key: $value"))

   def getBalance(
     request:    GetBalanceRequest,
     ctx:        Metadata,
   )(
     using span: Span[F],
     log:        LogIO[F],
     tracer:     Tracer[F],
   ): F[commands.Balance] =
      commands.Balance(100).pure[F]
      // MT.raiseError(ErrorsBuilder.notFoundError("Not found"))
      // FR.raise(ErrorsBuilder.badRequestError("bad request"))
      //      given Tracer[F] = tracer

      //      val key = Metadata.Key.of("tracestate", Metadata.ASCII_STRING_MARSHALLER)
      //      val kys = ctx.getAll(key).asScala.toList
      //      println(kys.head)
      //      println(ctx.keys().asScala.toList)
      //      println("---------------------------------------------------------")

      println(s"jctx2: ${JSpan.current().getSpanContext}") // get a span from a ThreadLocal
      println(s"otel4s2: ${span.context}")
      println(s"traceId: ${span.context.traceIdHex}")
      println(s"spanId: ${span.context.spanIdHex}")

      for
         r <- validateRequestId(request)
         // res <- service.getBalance(r.id.get.value.get)
         //        _ <- F.pure{println(s"yeeeee ${Try{r.id.get.value.get}}")}
         res <-
           service.getBalance(r.id.value)(
             using Map("traceId" -> span.context.traceIdHex))
         _ <- log.info("Validation done...")
      yield commands.Balance(res.value)
