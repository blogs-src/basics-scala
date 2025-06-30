package arch
package rest

import cats.data.*
import cats.effect.*
import cats.implicits.*
import org.http4s.*
import org.http4s.implicits.*
import org.typelevel.ci.CIString
import org.typelevel.otel4s.trace.Tracer
import smithy4s.Hints
import smithy4s.http4s.ServerEndpointMiddleware
import org.http4s.*
import org.http4s.dsl.io.*
import org.http4s.implicits.*
import org.http4s.EntityDecoder
import cats.effect.IO
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.{JWSAlgorithm, JWSHeader}
import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jwt.{JWTClaimsSet, SignedJWT}
import smithy4s.Blob
import smithy4s.json.Json
import smithy_rest.utils
import smithy_rest.wallet_ops.*
import smithy4s.Document

import java.util.Date

import scala.jdk.CollectionConverters.*

val responseDec = Json.payloadCodecs.decoders.fromSchema(AccessTokenPayload.schema)
val responseEnc = Json.payloadCodecs.encoders.fromSchema(LoginResponse.schema)

object LoginMiddleware:

  private def middleware(
                          sg: security.ServiceSecuritySigner,
                          signer: utils.AuthSign,
                        ): HttpApp[IO] => HttpApp[IO] =
    inputApp =>
      HttpApp[IO]{
        (request: Request[IO]) => {
          println("Login Middleware...............................")
          inputApp(request).flatMap {
            case r@Response(Status(200), _, _, _, _) =>
              val xxx: IO[Response[IO]] = r.as[String].flatMap { body =>
                val maybeReq = responseDec.decode(Blob(body))
                val out = maybeReq match {
                  case Right(AccessTokenPayload(aud, iss, sub, jti, roles, exp, iat, nbf)) =>
                    val claims = new JWTClaimsSet.Builder()
                      .subject(sub)
                      .issuer(iss)
                      .expirationTime(new java.util.Date(exp))
                      .notBeforeTime(new java.util.Date(iat))
                      .issueTime(new java.util.Date(nbf))
                      .jwtID(jti)
                      .audience(aud.asJava)
                      .claim("roles", roles.asJava)
                      // add more claims as needed
                      .build()
                    val serializeEither = sg.generateToken("SA2hmEnGOyeTOjfR_qpJaG_hEINDnhnJCO51OguWCa8", claims)
                    serializeEither match{
                      case Right(value) =>
                        val k = LoginResponse(value)
                        responseEnc.encode(k).toUTF8String
                      case Left(errors) =>
                        errors.map(_._2).mkString(", ")
                    }
                  case Left(_) =>
                    println(s"========================================")
                    "error"
                }
                Ok(out)
              }
              xxx
            case resp: Response[IO] =>
              println(s"default ======================================= ${resp.status == Status.Successful}")
              IO{resp}
          }
        }
     }

  def apply(
           sg: security.ServiceSecuritySigner,
           ): ServerEndpointMiddleware[IO] =
    new ServerEndpointMiddleware.Simple[IO]:
      private def mid(
                       sg: security.ServiceSecuritySigner,
                       signer: utils.AuthSign,
                     ): HttpApp[IO] => HttpApp[IO] = middleware(sg, signer)

      def prepareWithHints(
                            serviceHints: Hints,
                            endpointHints: Hints,
                          ): HttpApp[IO] => HttpApp[IO] =
            endpointHints.get[utils.AuthSign] match
              case Some(signer) => mid(sg, signer)
              case None =>
                identity
