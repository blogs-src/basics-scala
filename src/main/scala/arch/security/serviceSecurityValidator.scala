package arch
package security

import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jwt.SignedJWT
import com.nimbusds.jose.jwk.JWKSet

import java.util.Date
import cats.Semigroup
import cats.data.Validated
import cats.*
import cats.effect.*
import cats.implicits.*
import cats.instances.list.*
import com.nimbusds.jose.Payload
import org.http4s.Method.*
import org.http4s.Request
import org.http4s.Uri
import org.http4s.client.Client

import scala.jdk.CollectionConverters.*

enum JWTErrors:
   case NotJWKS, SignatureNotValid, JWTExpired, JWTNotBefore, RolesInvalid, KidNotPresentInJWKS, NotToken

trait ValidatorSource[A]:
   extension (a: A) def jwksUrl: String
   extension (a: A) def id(payload: Payload): String
   extension (a: A) def roles(payload: Payload): Set[String]

trait SecurityValidator[F[_]]:
   def updateJWKS(): F[Unit]
   def validate(jwtString: String, rolesToCheck: Set[String]): Either[List[(JWTErrors, String)], String]

class ServiceSecurityValidator[A: ValidatorSource](conf: A, client: Client[IO]) extends SecurityValidator[IO]:
   var jwkSet: Option[Map[String, JWK]] = None

   given Semigroup[String] = Semigroup.instance[String](
     (a, b) => a)

   //  val request = Request[IO](GET, Uri.unsafeFromString("http://localhost:8087/realms/trinity/protocol/openid-connect/certs"))
   val request = Request[IO](GET, Uri.unsafeFromString(conf.jwksUrl))

   def updateJWKS(): IO[Unit] =
      val res2 =
        for {
          body <- client.expect[String](request)
//      _ <- IO.println(body)
          _ <-
             val res = JWKSet.parse(body)
             println(res)
             val keys = res.getKeys.asScala // java.util.List[JWK]
             val kmap: Map[String, JWK] =
               keys.map {
                 k =>
                   k.getKeyID -> k
               }.toMap
             jwkSet = Some(kmap)
             IO(())
        } yield ()

      res2.handleErrorWith:
           error =>
              IO.println(s"===> (not JWKS data) ${error.getMessage}").void

   def validate(jwtString: String, rolesToCheck: Set[String]): Either[List[(JWTErrors, String)], String] =
      val valid =
        jwkSet match
          case Some(kmap) =>
            val signedJWT = SignedJWT.parse(jwtString)
            val kid = signedJWT.getHeader.getKeyID
            val sset = kmap.keys.toSet
            if (sset.contains(kid))
               val publicKey: JWK = kmap(kid)
               val verifier = new com.nimbusds.jose.crypto.RSASSAVerifier(publicKey.toRSAKey.toRSAPublicKey)
               val isValid = signedJWT.verify(verifier)
               val claims = signedJWT.getJWTClaimsSet
               val now = new Date().getTime / 1000 // current time in seconds
               val exp = Option(claims.getExpirationTime).map(_.getTime / 1000)
               val nbf = Option(claims.getNotBeforeTime).map(_.getTime / 1000)
               val iat = Option(claims.getIssueTime).map(_.getTime / 1000)
               val isNotExpired = exp.forall(_ > now)
               val isNotBefore = nbf.forall(_ <= now)
               val id: String = conf.id(signedJWT.getPayload)
               val generalChecks = Validated.cond(isValid, id, List((JWTErrors.SignatureNotValid, "JWT signature is not valid")))
                 .combine(Validated.cond(isNotExpired, id, List((JWTErrors.JWTExpired, "JWT expired"))))
                 .combine(Validated.cond(isNotBefore, id, List((JWTErrors.JWTNotBefore, "JWT not before"))))
               if (isValid)
                  val roles = conf.roles(signedJWT.getPayload)
                  val hasRole = rolesToCheck.subsetOf(roles)
                  generalChecks.combine(Validated.cond(hasRole,
                                                       id,
                                                       List((
                                                         JWTErrors.RolesInvalid,
                                                         s"JWT roles invalid: roles '${rolesToCheck.mkString("{", ", ", "}")}' not in '${
                                                                                                                                          roles.mkString("{", ", ", "}")
                                                                                                                                        }'"))))
               else
                  generalChecks
            else
               Validated.invalid(List((JWTErrors.KidNotPresentInJWKS, s"Key id '$kid' not match anyone of the present in the JWKS"))) // "left" value
          case None       => Validated.invalid(List((JWTErrors.NotJWKS, "Not JWKS"))) // "left" value
      valid.toEither

   def initialize(): Unit =
      import cats.effect.unsafe.implicits.global
      updateJWKS().unsafeRunSync()

   initialize()
