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

class ControlServiceImpl(validator: security.SecurityValidator[IO]) extends ControlService[IO] {
  def reloadJWKS(): IO[Unit] =
    validator.updateJWKS()
}

class SmithyResource:
   private def controlRoutes(validator: security.SecurityValidator[IO]): Resource[IO, HttpRoutes[IO]] =
    SimpleRestJsonBuilder.routes(new ControlServiceImpl(validator)).resource

   private def routes_combined(
                     local:  IOLocal[Option[domain.RequestInfo[Result]]],
                     tracer: Tracer[Result],
                     s:      WalletService[Result],
                   ): Resource[IO, HttpRoutes[IO]] = {
     for{
       (r1, r3) <- serviceRoutes(local, tracer, s)
       r2 <- controlRoutes(r3)
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
   ): Resource[IO, (HttpRoutes[IO], security.SecurityValidator[IO])] =

      val getRequestInfo: Result[domain.RequestInfo[Result]] = EitherT.right(local.get.flatMap {
        case Some(value) => IO.pure(value)
        case None        => IO.raiseError(new IllegalAccessException("Tried to access the value outside of the lifecycle of an http request"))
      })


        import security.KrakendConfs.given

        val conf = security.Krakend("http://localhost:9000/store/jwks-pub.json")

        for {
          restClient <- BlazeClientBuilder[IO].resource
          validator = new security.ServiceSecurityValidator(conf, restClient)
          resource <- SimpleRestJsonBuilder.routes(
              new WalletOpsImpl[Result](s, getRequestInfo)
                .transform(
                  Converter.toIO,
                )
            )
            .mapErrors {
              case HttpPayloadError(_, expected, message) =>
                val e = ErrorsBuilder.badRequestError(s"Related to $expected, comment: ${translateMessage(message)}")
                BadRequestError(e.code, e.title, e.message)

              case e: arch.Unauthorized =>
                UnauthorizedError(e.code, e.title, e.message)

              case err: Throwable =>
                println(err.getClass.getName)
                err.printStackTrace()
                val e = internalServerError(err.getMessage)
                InternalServerError(e.code, e.title, e.message)

            }
            .middleware(AuthMiddleware(validator))
            .resource.map {
              routes =>
                Middleware.withRequestInfo(routes, local, tracer)
            }
        } yield (resource, validator)

   def all(
     local:  IOLocal[Option[domain.RequestInfo[Result]]],
     tracer: Tracer[Result],
     s:      WalletService[Result],
   ): Resource[IO, HttpRoutes[IO]] = routes_combined(local, tracer, s)
