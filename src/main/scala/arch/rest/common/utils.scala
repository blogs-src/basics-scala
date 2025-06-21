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
import cats.data.*
import org.http4s.HttpRoutes
import cats.syntax.all.*
import org.http4s.headers.{ `Content-Type`, `User-Agent` }
import org.typelevel.otel4s.trace.Tracer

import org.http4s.*
import org.http4s.client.Client
import org.http4s.syntax.literals.*
import org.typelevel.ci.CIString

import org.typelevel.otel4s.Attribute

import io.opentelemetry.api.trace.Span as JSpan

import ErrorsBuilder.*

object Middleware:

   def withRequestInfo(
     routes: HttpRoutes[IO],
     local:  IOLocal[Option[domain.RequestInfo[Result]]],
     tracer: Tracer[Result],
   ): HttpRoutes[IO] = HttpRoutes[IO] {
     request =>
        val hnames = request.headers.headers.map(_.name.toString)
        val hvals = hnames.map(
          key => (key, request.headers.get(CIString(key)).map(_.head.value).get))
        val hvals2 = Map.from(hvals)
        val requestInfo = Some(domain.RequestInfo[Result](hvals2, tracer))
        OptionT.liftF(local.set(requestInfo)) *> routes(request)
   }

object Converter:

   val toIO: PolyFunction[Result, IO] =
     new PolyFunction[Result, IO]:

        def apply[A](result: Result[A]): IO[A] = result.foldF(
          error =>
            IO.raiseError(
              error match
                case e: ServiceUnavailable => ServiceUnavailableError(e.code, e.title, e.message)
                case e: Conflict           => ConflictError(e.code, e.title, e.message)
                case e: BadRequest         =>
                  //                  println("BadRequest")
                  BadRequestError(e.code, e.title, e.message)
                case e: NotFound           => NotFoundError(e.code, e.title, e.message)
                case e: InternalServer     =>
                  //                  println("InternalServer")
                  InternalServerError(e.code, e.title, e.message)
                case e: Unauthorized       => UnauthorizedError(e.code, e.title, e.message)
                case e: Forbidden          => ForbiddenError(e.code, e.title, e.message)),
          value =>
            IO {
              value
            })
