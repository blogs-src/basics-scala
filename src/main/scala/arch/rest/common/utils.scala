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
import smithy4s.{Endpoint, Hints}
import smithy4s.http4s.ServerEndpointMiddleware
import smithy4s.http4s.SimpleRestJsonBuilder
import smithy4s.kinds.PolyFunction
import smithy4s.http.HttpPayloadError
import cats.effect.kernel.Resource
import cats.data.*
import org.http4s.HttpRoutes
import cats.syntax.all.*
import org.http4s.headers.{`Content-Type`, `User-Agent`}
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

case class ApiToken(value: String)

import org.typelevel.vault.Key

object Attrs {
  val UserId: Key[String] = Key.newKey[SyncIO, String].unsafeRunSync()
}

object AuthMiddleware {

  private def middleware
  (
    roles: List[String],
    validator: security.SecurityValidator[IO],
  ): HttpApp[IO] => HttpApp[IO] = {
    inputApp =>
      HttpApp[IO] { request =>

        println("AuthMiddleware...............................")

        val maybeKey = request.headers
          .get[`Authorization`]
          .collect {
            case Authorization(
            Credentials.Token(AuthScheme.Bearer, value)
            ) =>
              value
          }
          .map {
            ApiToken.apply
          }

        val defaultResponse: Either[List[(JWTErrors, String)], String] = Left(List((JWTErrors.NotToken, "Token missing")))
        val isAuthorized: IO[Either[List[(JWTErrors, String)], String]] = maybeKey
          .map { key =>
            IO.pure(validator.validate(key.value, roles.toSet))
          }
          .getOrElse(IO.pure(defaultResponse))

        isAuthorized.flatMap(auth =>
          auth match {
            case Left(list) =>
              val msg = list.map(_._2).mkString(",")
              IO.raiseError(unauthorizedError(msg))
            case Right(userId) =>

              request.attributes.lookup(Attrs.UserId).foreach(id => println(s"******** UserId: $id"))

              val newRequest = request
//                .withAttributes(attributes=nA)
//              println(s"Attributes size 0: ${nA.size}")

              inputApp(newRequest)

          }
        )

      }

  }

  def apply
  (
    validator: security.SecurityValidator[IO]
  ): ServerEndpointMiddleware[IO] =
    new ServerEndpointMiddleware.Simple[IO] {
      private def mid(roles: List[String]): HttpApp[IO] => HttpApp[IO] = middleware(roles, validator)

      def prepareWithHints
      (
        serviceHints: Hints,
        endpointHints: Hints,
      ): HttpApp[IO] => HttpApp[IO] = {
        serviceHints.get[smithy.api.HttpBearerAuth] match {
          case Some(_) =>
            endpointHints.get[utils.AuthToken] match {
              case Some(auths) if auths.roles.isEmpty => identity
              case Some(auths) =>
                mid(auths.roles)
              case None => identity
            }
          case None => identity
        }
      }
    }

}


object Middleware:

   def withRequestInfo(
     routes: HttpRoutes[IO],
     local:  IOLocal[Option[domain.RequestInfo[Result]]],
     tracer: Tracer[Result],
   ): HttpRoutes[IO] = HttpRoutes[IO] {
     request =>
        println("withRequestInfo2 <<<...............................>>>")
        val hnames = request.headers.headers.map(_.name.toString)
        val hvals = hnames.map(
          key => (key, request.headers.get(CIString(key)).map(_.head.value).get))
//        println(hvals)

        val nA = request.attributes.insert(Attrs.UserId, "userId")
        val newRequest = request
         .withAttributes(attributes = nA)

        println(s"Attributes size: ${request.attributes.size}")
        val hvals2 = Map.from(hvals)
        val userId = hvals2.get("internal-user-id")
        val requestInfo = Some(domain.RequestInfo[Result](hvals2, tracer, userId))
        OptionT.liftF(local.set(requestInfo)) *> routes(newRequest)
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
