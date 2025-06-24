package arch
package rest

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
import smithy4s.service_control.*
import smithy4s.http4s.*
import cats.effect.*
import cats.implicits.*
import org.http4s.implicits.*
import org.http4s.*
import com.comcast.ip4s.*
import org.http4s.client.*
import smithy4s.Hints
import org.http4s.headers.Authorization

case class ApiToken(value: String)

object AuthMiddleware {

  private def middleware
  (
    roles: List[String],
    // authChecker: AuthChecker
  ): HttpApp[IO] => HttpApp[IO] = {
    inputApp =>
       HttpApp[IO] { request =>
         val maybeKey = request.headers
         .get[`Authorization`]
         .collect {
           case Authorization(
                 Credentials.Token(AuthScheme.Bearer, value)
               ) =>
             value
         }
         .map { ApiToken.apply }

         val isAuthorized = maybeKey
                                   .map { key =>
                          //           authChecker.isAuthorized(key)
                                        IO.pure{true}
                                        IO.pure {false}
                                   }
                                   .getOrElse(IO.pure(false))

         isAuthorized.ifM(
           ifTrue = inputApp(request),
           ifFalse = IO.raiseError(unauthorizedError("Not authorized!"))
         )
     }

  }

  def apply
  (
    // authChecker: AuthChecker
  ): ServerEndpointMiddleware[IO] =
    new ServerEndpointMiddleware.Simple[IO] {
      private def mid(roles: List[String]): HttpApp[IO] => HttpApp[IO] = middleware(roles)

      def prepareWithHints
      (
        serviceHints: Hints,
        endpointHints: Hints,
      ): HttpApp[IO] => HttpApp[IO] = {
        serviceHints.get[smithy.api.HttpBearerAuth] match {
          case Some(_) =>
            endpointHints.get[utils.AuthToken] match {
              case Some(auths) if auths.roles.isEmpty => identity
              case Some(auths) => mid(auths.roles)
              case None => identity
            }
          case None => identity
        }
      }
    }

}


class ControlServiceImpl extends ControlService[IO] {
  def reloadJWKS(): IO[Unit] = IO.pure {
    ()
  }
}

class SmithyResource:
   private val controlRoutes: Resource[IO, HttpRoutes[IO]] =
    SimpleRestJsonBuilder.routes(new ControlServiceImpl).resource

   private def routes_combined(
                     local:  IOLocal[Option[domain.RequestInfo[Result]]],
                     tracer: Tracer[Result],
                     s:      WalletService[Result],
                   ): Resource[IO, HttpRoutes[IO]] = {
     for{
       r1 <- serviceRoutes(local, tracer, s)
       r2 <- controlRoutes
     } yield r1 <+> r2
   }

   private def translateMessage(message: String): String =
      val i = message.indexOf(", offset:")
      if i == -1 then
         message
      else
         message.substring(0, i)

   private def serviceRoutes(
     local:  IOLocal[Option[domain.RequestInfo[Result]]],
     tracer: Tracer[Result],
     s:      WalletService[Result],
   ): Resource[IO, HttpRoutes[IO]] =

      val getRequestInfo: Result[domain.RequestInfo[Result]] = EitherT.right(local.get.flatMap {
        case Some(value) => IO.pure(value)
        case None        => IO.raiseError(new IllegalAccessException("Tried to access the value outside of the lifecycle of an http request"))
      })

      SimpleRestJsonBuilder.routes(
        new WalletOpsImpl[Result](s, getRequestInfo)
          .transform(
            Converter.toIO))
        .mapErrors {
          case HttpPayloadError(_, expected, message) =>
            // logger.foreach(_.error(s"${message}"))
            val e = ErrorsBuilder.badRequestError(s"Related to $expected, comment: ${translateMessage(message)}")
            BadRequestError(e.code, e.title, e.message)

          case err: Throwable =>
            val e = internalServerError(err.getMessage)
            InternalServerError(e.code, e.title, e.message)

        }
        .middleware(AuthMiddleware())
        .resource.map {
          routes =>
            Middleware.withRequestInfo(routes, local, tracer)
        }

   def all(
     local:  IOLocal[Option[domain.RequestInfo[Result]]],
     tracer: Tracer[Result],
     s:      WalletService[Result],
   ): Resource[IO, HttpRoutes[IO]] = routes_combined(local, tracer, s)
