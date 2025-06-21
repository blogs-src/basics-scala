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

import ErrorsBuilder.*

class SmithyResource {

  private def translateMessage(message: String): String = {
    val i = message.indexOf(", offset:")
    if (i == -1)
      message
    else
      message.substring(0, i)
  }

  private def example(local: IOLocal[Option[domain.RequestInfo[Result]]], tracer: Tracer[Result], s: WalletService[Result]): Resource[IO, HttpRoutes[IO]] =

    val getRequestInfo: Result[domain.RequestInfo[Result]] = EitherT.right(local.get.flatMap {
      case Some(value) => IO.pure(value)
      case None => IO.raiseError(new IllegalAccessException("Tried to access the value outside of the lifecycle of an http request"))
    })

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

  def all(local: IOLocal[Option[domain.RequestInfo[Result]]], tracer: Tracer[Result], s: WalletService[Result]): Resource[IO, HttpRoutes[IO]] = example(local, tracer, s)

}
