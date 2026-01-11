package arch
package rest

import smithy_rest.utils
import smithy4s.http4s.ServerEndpointMiddleware
import ErrorsBuilder.*
import arch.security.JWTErrors
import cats.effect.*
import org.http4s.*
import smithy4s.Hints
import org.http4s.headers.Authorization

case class ApiToken(value: String)

import org.typelevel.vault.Key

object Attrs {
   val UserId: Key[String] = Key.newKey[SyncIO, String].unsafeRunSync()
}

object AuthMiddleware {

   private def middleware(
     roles:     List[String],
     validator: security.SecurityValidator[IO],
   ): HttpApp[IO] => HttpApp[IO] =
     inputApp =>
       HttpApp[IO] {
            request =>

               println("AuthMiddleware...............................")

               val maybeKey = request.headers
                 .get[`Authorization`]
                 .collect {
                    case Authorization(
                          Credentials.Token(AuthScheme.Bearer, value)) =>
                      value
               }
                 .map {
                    ApiToken.apply
               }

               val defaultResponse: Either[List[(JWTErrors, String)], String] = Left(List((JWTErrors.NotToken, "Token missing")))
               val isAuthorized: IO[Either[List[(JWTErrors, String)], String]] = maybeKey
                 .map {
                    key =>
                       IO.pure(validator.validate(key.value, roles.toSet))
               }
                 .getOrElse(IO.pure(defaultResponse))

               isAuthorized.flatMap {
                 case Left(list) =>
                   val msg = list.map(_._2).mkString(",")
                   IO.raiseError(unauthorizedError(msg))
                 case Right(userId) =>
                   val nA = request.attributes.insert(Attrs.UserId, userId)
                   val newRequest = request
                     .withAttributes(attributes = nA)
                   inputApp(newRequest)

               }
       }

   def apply(validator: security.SecurityValidator[IO]): ServerEndpointMiddleware[IO] =
     new ServerEndpointMiddleware.Simple[IO] {
        private def mid(roles: List[String]): HttpApp[IO] => HttpApp[IO] = middleware(roles, validator)

        def prepareWithHints(
          serviceHints:  Hints,
          endpointHints: Hints,
        ): HttpApp[IO] => HttpApp[IO] =
          serviceHints.get[smithy.api.HttpBearerAuth] match {
            case Some(_) =>
              endpointHints.get[utils.AuthToken] match {
                case Some(auths) if auths.roles.isEmpty => identity
                case Some(auths)                        => mid(auths.roles)
                case None                               => identity
              }
            case None    => identity
          }
     }
}
