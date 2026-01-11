package arch
package security

import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jwt.SignedJWT
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jose.jwk.JWKSet

import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.{JWSAlgorithm, JWSHeader}
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

enum SignerError {
   case NotJWKS, KidNotPresentInJWKS
}

trait SecuritySigner[F[_]] {
  def updateJWKS(): F[Unit]
  def generateToken(kid: String, claims: JWTClaimsSet): Either[List[(SignerError, String)], String]
}

class ServiceSecuritySigner(jwksUrl: String, client: Client[IO]) extends SecuritySigner[IO] {
   var jwkSet: Option[Map[String, RSAKey]] = None

   given Semigroup[String] = Semigroup.instance[String](
     (a, b) => a)

   val request = Request[IO](GET, Uri.unsafeFromString(jwksUrl))

   def updateJWKS(): IO[Unit] = {
      val res2 =
        for {
           body <- client.expect[String](request)
//      _ <- IO.println(body)
           _ <- {
              val res = JWKSet.parse(body)
              println(res)
              val keys = res.getKeys.asScala
              val kmap: Map[String, RSAKey] =
                keys.map {
                  k =>
                    k.getKeyID -> k.toRSAKey
                }.toMap
              jwkSet = Some(kmap)
              IO(())
           }
        }
        yield ()

      res2.handleErrorWith {
           error =>
              IO.println(s"===> (not JWKS data) ${error.getMessage}").void
      }
   }


   def generateToken(kid: String, claims: JWTClaimsSet): Either[List[(SignerError, String)], String] =
    for {
      kmap <- jwkSet.toRight(List((SignerError.NotJWKS, "Not JWKS")))
      rsaKey <- kmap.get(kid)
        .toRight(List((SignerError.KidNotPresentInJWKS, s"Key id '$kid' not match anyone of the present in the JWKS")))
    } yield {
      val header = new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(rsaKey.getKeyID).build()
      val signedJWT = new SignedJWT(header, claims)
      // 4. Sign the JWT
      val signer = new RSASSASigner(rsaKey)
      signedJWT.sign(signer)
      signedJWT.serialize()
    }

   def initialize(): Unit = {
      import cats.effect.unsafe.implicits.global
      updateJWKS().unsafeRunSync()
   }

   initialize()
}
