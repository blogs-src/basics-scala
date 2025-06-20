package arch
package rest

import cats.data.EitherT
import cats.effect.*

import smithy_rest.wallet_ops.*
import smithy_rest.utils

import org.http4s.*
import smithy4s.Hints
import smithy4s.codecs.*
import smithy4s.http4s.ServerEndpointMiddleware
import smithy4s.http4s.SimpleRestJsonBuilder
import smithy4s.kinds.PolyFunction
import smithy4s.http.HttpPayloadError
import cats.effect.kernel.Resource
import cats.data._
import org.http4s.HttpRoutes
import cats.syntax.all._
import org.http4s.headers.{`Content-Type`, `User-Agent`}
import org.typelevel.otel4s.trace.Tracer

import org.http4s._
import org.http4s.client.Client
import org.http4s.syntax.literals._
import org.typelevel.ci.CIString

import org.typelevel.otel4s.Attribute

import io.opentelemetry.api.trace.{Span => JSpan}

object Middleware {
  def withRequestInfo( routes: HttpRoutes[IO],
                       local: IOLocal[Option[domain.RequestInfo[Result]]],
                       tracer: Tracer[Result]): HttpRoutes[IO] =
    HttpRoutes[IO] { request =>
//      val requestInfo = for {
//        contentType <- request.headers.get[`Content-Type`].map(ct => s"${ct.mediaType.mainType}/${ct.mediaType.subType}")
//        userAgent <- request.headers.get[`User-Agent`].map(_.product.toString)
//      } yield RequestInfo(
//        contentType,
//        userAgent
//      )

//      tracer.span("Work.DoWork2").use { span =>
//        val requestInfo = Some(domain.RequestInfo[Result](Map("a" -> "b"), tracer, span))
//        OptionT.liftF(local.set(requestInfo)) *> routes(request)
//      }

      val hnames = request.headers.headers.map(_.name.toString)
      val hvals = hnames.map(key => (key, request.headers.get(CIString(key)).map(_.head.value).get))
      val hvals2 = Map.from(hvals)
      val requestInfo = Some(domain.RequestInfo[Result](hvals2, tracer))
      OptionT.liftF(local.set(requestInfo)) *> routes(request)

    }

}

object Converter:

  val toIO: PolyFunction[Result, IO] =
    new PolyFunction[Result, IO] {

      def apply[A](result: Result[A]): IO[A] = {
        result.foldF(
          error =>
            IO.raiseError(
              error match {
                case e: ServiceUnavailable => ServiceUnavailableError(e.code, e.title, e.message)
                case e: Conflict => ConflictError(e.code, e.title, e.message)
                case e: BadRequest =>
//                  println("BadRequest")
                  BadRequestError(e.code, e.title, e.message)
                case e: NotFound => NotFoundError(e.code, e.title, e.message)
                case e: InternalServer =>
//                  println("InternalServer")
                  InternalServerError(e.code, e.title, e.message)
                case e: Unauthorized => UnauthorizedError(e.code, e.title, e.message)
                case e: Forbidden => ForbiddenError(e.code, e.title, e.message)
              }
            ),
          value => IO {
            value
          }
        )
      }

    }

import ErrorsBuilder.*

class WalletOpsImpl[F[_]](using F: Async[F])
(
  ser: WalletService[F],
  info: F[domain.RequestInfo[F]]
) extends WalletOpsService[F] {

  def getBalance(id: RequestId): F[Balance] =
    info.flatMap{ rinfo =>
      rinfo.tracer.joinOrRoot(rinfo.headers) {
        rinfo.tracer.span("Work.DoWork2", Attribute("custom_tag", "aa")).use { span =>
          println(s"jctx: ${JSpan.current().getSpanContext}") // get a span from a ThreadLocal
          println(s"otel4s: ${span.context}")


          val log = auditing.Logger.getLogger("logs-rest").withCustomContext(
            "traceId" -> span.context.traceIdHex,
          )

          for{
            _ <- log.info("calling rest_service.getBalance")
            res <- ser.getBalance(id)(span, log, rinfo.tracer)
            _ <- span.addAttribute(Attribute("traceId", span.context.traceIdHex))
          } yield res
          
          
        }
    }
    }

  def healthCheck(): F[Unit] = {
    val res: F[Unit] = for {
      d <- info
      _ <- F.pure{println(s"headers: $d")}
    } yield ()
    res
  }

}


class SmithyResource {

  private def translateMessage(message: String): String = {
    val i = message.indexOf(", offset:")
    if (i == -1)
      message
    else
      message.substring(0, i)
  }

  private def example(local: IOLocal[Option[domain.RequestInfo[Result]]], tracer: Tracer[Result], channel: io.grpc.ManagedChannel): Resource[IO, HttpRoutes[IO]] =

    val getRequestInfo: Result[domain.RequestInfo[Result]] = EitherT.right(local.get.flatMap {
      case Some(value) => IO.pure(value)
      case None => IO.raiseError(new IllegalAccessException("Tried to access the value outside of the lifecycle of an http request"))
    })

    val s = new WalletServiceImpl[Result](channel)
    SimpleRestJsonBuilder.routes(
        new WalletOpsImpl[Result](s, getRequestInfo)
          .transform(
            Converter.toIO
          )
      )
      .mapErrors {
        case HttpPayloadError(_, expected, message) =>
          // logger.foreach(_.error(s"${message}"))
          val e = ErrorsBuilder.badRequestError(s"Related to $expected, comment: ${translateMessage(message)}")
          BadRequestError(e.code, e.title, e.message)

        case err: Throwable =>
          val e = internalServerError(err.getMessage)
          InternalServerError(e.code, e.title, e.message)

      }
      .resource.map { routes =>
        Middleware.withRequestInfo(routes, local, tracer)
      }

  def all(local: IOLocal[Option[domain.RequestInfo[Result]]], tracer: Tracer[Result], channel: io.grpc.ManagedChannel): Resource[IO, HttpRoutes[IO]] = example(local, tracer, channel)

}
