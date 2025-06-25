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
import org.typelevel.vault.Key

object Middleware:

   def appToRoutes(app: HttpApp[IO]): HttpRoutes[IO] = Kleisli:
     req => OptionT.liftF(app(req))

   def routesToApp(routes: HttpRoutes[IO]): HttpApp[IO] = routes.orNotFound

   def apply(
     local:  IOLocal[Option[domain.RequestInfo[Result]]],
     tracer: Tracer[Result],
   ): ServerEndpointMiddleware[IO] =

     new ServerEndpointMiddleware.Simple[IO]:

       def prepareWithHints(
         serviceHints:  Hints,
         endpointHints: Hints,
       ): HttpApp[IO] => HttpApp[IO] =
         inputApp =>
            val routes = appToRoutes(inputApp)
            val nroute = HttpRoutes[IO]:
              request =>
                 println("withRequestInfo2 <<<...............................>>>")
                 val hnames = request.headers.headers.map(_.name.toString)
                 val hvals = hnames.map(
                   key => (key, request.headers.get(CIString(key)).map(_.head.value).get))
                 val hvals2 = Map.from(hvals)
                 val userId = request.attributes.lookup(Attrs.UserId)
                 val requestInfo = Some(domain.RequestInfo[Result](hvals2, tracer, userId))
                 OptionT.liftF(local.set(requestInfo)) *> routes(request)
            routesToApp(nroute)
