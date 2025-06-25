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

import cats.data.EitherT
import cats.effect.*
import smithy_rest.wallet_ops.*
import smithy_rest.utils
import org.http4s.*
import smithy4s.{ Endpoint, Hints }
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
import arch.security.JWTErrors
import smithy4s.service_control.*
import smithy4s.http4s.*
import cats.effect.*
import cats.implicits.*
import org.http4s.implicits.*
import org.http4s.*
import com.comcast.ip4s.*
import org.http4s.blaze.client.BlazeClientBuilder
import org.http4s.client.*
import smithy4s.Hints
import org.http4s.headers.Authorization

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
