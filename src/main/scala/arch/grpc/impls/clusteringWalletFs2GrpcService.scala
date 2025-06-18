package arch

import cats.data.EitherT
import cats.effect.*
import com.wallet.demo.clustering.rpc.admin.*
import io.grpc.*
import io.scalaland.chimney.dsl.*
import cats.mtl.*
import com.wallet.proto.messages.commands
import org.typelevel.otel4s.trace.Tracer

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

import scala.jdk.CollectionConverters.*

class ClusteringWalletFs2GrpcServiceImpl[G: ExceptionGenerator](service: ClusteringWalletGrpcService[Result], transformers: MyTransformers[G], tracer: Tracer[Result])
    extends WalletCommandRpcServiceFs2Grpc[cats.effect.IO, Metadata] {
  import transformers.othersTransformers

  def getBalance(request: GetBalanceRequest, ctx: Metadata) = service.getBalance(request, ctx).transformInto[IO[commands.Balance]]

}

class ClusteringWalletGrpcServiceImpl[F[_], G: ExceptionGenerator]
(service: WalletServiceIO[F], tracer: Tracer[F])
(using transformers: MyTransformers[G]
// , MTracer: Tracer[F]
)(using F: Async[F], FR: Raise[F, ServiceError], M: Monad[F], MT: MonadThrow[F])
  extends ClusteringWalletGrpcService[F]:

    import io.scalaland.chimney.partial
    import io.scalaland.chimney.protobufs.*
    import transformers.given

    private def validateRequestId(request: GetBalanceRequest): F[BalanceRequest] =
      val res = request.transformIntoPartial[BalanceRequest].asEither.asResult.asEitherErrorPathMessageStrings
      res match
        case Right(r) => r.pure[F]
        case Left(e) =>
              val (key, value) = e.toList.head
              FR.raise(ErrorsBuilder.badRequestError(s"$key: $value"))

    import org.typelevel.otel4s.context.propagation.*

    given TextMapGetter[Metadata] =
      new TextMapGetter[Metadata] {
        def get(headers: Metadata, key: String): Option[String] =
            println(s"key: ${key}")
            val akeys = keys(headers).toSet
            println(akeys)
            if (akeys.contains(key)) {
              val key_ = Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER)
              val kys = headers.getAll(key_).asScala.toList
              kys.headOption
            }else{
              None
            }

        def keys(headers: Metadata): Iterable[String] =
          println(s"keys")
          val keys = headers.keys().asScala.toList
          println(s"keys: ${keys}")
          keys
      }

    def getBalance(request: GetBalanceRequest, ctx: Metadata): F[commands.Balance] = {
      commands.Balance(100).pure[F]
      // MT.raiseError(ErrorsBuilder.notFoundError("Not found"))
      // FR.raise(ErrorsBuilder.badRequestError("bad request"))
      given Tracer[F] = tracer

//      val key = Metadata.Key.of("tracestate", Metadata.ASCII_STRING_MARSHALLER)
//      val kys = ctx.getAll(key).asScala.toList
//      println(kys.head)
//      println(ctx.keys().asScala.toList)
//      println("---------------------------------------------------------")

      import io.opentelemetry.api.trace.{Span => JSpan}

      Tracer[F].joinOrRoot(ctx) {
        Tracer[F].span("Work.DoWork", Attribute("custom_tag", "aa")).use { span =>

          println(s"jctx: ${JSpan.current().getSpanContext}") // get a span from a ThreadLocal
          println(s"otel4s: ${span.context}")
//          println(s"traceid: ${span.context.traceId}")

          for {
            r <- validateRequestId(request)
            //res <- service.getBalance(r.id.get.value.get)
            //        _ <- F.pure{println(s"yeeeee ${Try{r.id.get.value.get}}")}
            res <- service.getBalance(r.id.value)
            //        res <- service.getBalance("r.id.value")
          } yield commands.Balance(res.value)

        }
      }

    }
