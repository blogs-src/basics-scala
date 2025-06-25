package arch
package rest

//import smithy_rest.wallet_ops.*
//import cats.effect.*
//import cats.effect._
//import cats.implicits._
//import org.http4s.implicits.*
import org.http4s.blaze.client.BlazeClientBuilder
import scala.jdk.CollectionConverters.*
//import scala.concurrent.ExecutionContext.global
import org.http4s.blaze.client.BlazeClientBuilder

import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jwt.SignedJWT
import com.nimbusds.jose.jwk.JWKSet

import com.nimbusds.jose.Payload
import org.http4s.ember.server.*
//import org.http4s.*
import com.comcast.ip4s.*
//import smithy4s.http4s.SimpleRestJsonBuilder

//import org.typelevel.otel4s.trace.Tracer

import cats.data.EitherT
import com.wallet.demo.clustering.rpc.admin as padmin
//import cats.mtl.*
//import fs2.grpc.client.ClientOptions
//import cats.effect.std.Dispatcher

import cats.*
import cats.effect.*
//import cats.mtl.*
//import cats.FlatMap
//import cats.syntax.flatMap.*
//import cats.syntax.functor.*

object Main extends IOApp.Simple:

   //    val clientOptions = ClientOptions.default
   def mkMetadata(headers: Map[String, String]): Result[io.grpc.Metadata] =
      val metadata = new io.grpc.Metadata()
      for (k, v) <- headers do
         val key = io.grpc.Metadata.Key.of(k, io.grpc.Metadata.ASCII_STRING_MARSHALLER)
         metadata.put(key, v)
      EitherT.right(IO.pure(metadata))

   val run = IOLocal(Option.empty[domain.RequestInfo[Result]]).flatMap {
     local =>

        val grpcTargetPort = 9999
        val httpServerPort = 9001

        val channel: GrpcClientToWritesideResource = GrpcClientToWritesideResource(grpcTargetPort)
        val t =
          for
             ch <- channel.resource
             client <- padmin.WalletCommandRpcServiceFs2Grpc.mkClientResource[Result, Map[String, String]](ch, mkMetadata)
             s = new WalletServiceImpl[Result](client)
             tracer <- auditing.Tracer.makeOtel("otel-rest-app")
             z <- monadConversions.convertResource((new SmithyResource).all(local, tracer, s), monadConversions.ioToResult)
          yield (z, tracer, ch, client)

        val t1 = monadConversions.convertResource(t, monadConversions.resultToIO)

        t1.flatMap {
          (
            routes,
            _,
            _,
            _,
          ) =>
              EmberServerBuilder
                .default[IO]
                .withPort(Port.fromInt(httpServerPort).get)
                .withHost(host"0.0.0.0")
                .withHttpApp(routes.orNotFound)
                .build
        }
          .use(
            _ =>
            IO.never,
          )
          .handleErrorWith {
            error =>
               println(s"===> ${error.getMessage}")
               IO.raiseError(error)
          }

   }
