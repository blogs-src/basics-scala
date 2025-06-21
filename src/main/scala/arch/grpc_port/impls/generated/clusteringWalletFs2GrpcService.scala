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

import io.opentelemetry.api.trace.{Span => JSpan}
import logstage.LogIO

class ClusteringWalletFs2GrpcServiceImpl[G: ExceptionGenerator](service: ClusteringWalletGrpcService[Result], transformers: MyTransformers[G])
    extends WalletCommandRpcServiceFs2Grpc[cats.effect.IO, Metadata] {
  import transformers.othersTransformers

  def getBalance(request: GetBalanceRequest, ctx: Metadata) = service.getBalance(request, ctx).transformInto[IO[commands.Balance]]

}


class ClusteringWalletGrpcServiceImpl[F[_]: Tracer, G: ExceptionGenerator](service: ClusteringWalletGrpcService2[F])(using transformers: MyTransformers[G])(using F: Async[F], FR: Raise[F, ServiceError], M: Monad[F], MT: MonadThrow[F])
  extends ClusteringWalletGrpcService[F]:

    import io.scalaland.chimney.partial
    import io.scalaland.chimney.protobufs.*
    import transformers.given

    import org.typelevel.otel4s.context.propagation.*

    def getBalance(request: GetBalanceRequest, ctx: Metadata): F[commands.Balance] = {

//      val key = Metadata.Key.of("tracestate", Metadata.ASCII_STRING_MARSHALLER)
//      val kys = ctx.getAll(key).asScala.toList
//      println(kys.head)
//      println(ctx.keys().asScala.toList)
      val ks = ctx.keys().asScala.toList.map{
        k =>{
          val key = Metadata.Key.of(k, Metadata.ASCII_STRING_MARSHALLER)
          (k, ctx.getAll(key).asScala.toList.head)
        }
      }
//      println("---------------------------------------------------------")

      Tracer[F].joinOrRoot(ks.toMap) {
        Tracer[F].span("Work.DoWork", Attribute("custom_tag", "aa")).use { span =>

          println(s"jctx: ${JSpan.current().getSpanContext}") // get a span from a ThreadLocal
          println(s"otel4s: ${span.context}")
//          println(s"traceid: ${span.context.traceId}")

          val log = auditing.Logger.getLogger("logs-grpc").withCustomContext(
              "traceId" -> span.context.traceIdHex,
            )
          for{
            _ <- log.info("calling service.getBalance")
            res <- service.getBalance(request, ctx)(using span, log)
            _ <- span.addAttribute(Attribute("traceId", span.context.traceIdHex))
          } yield res

        }
      }

    }
