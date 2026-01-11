package arch
package rest

import org.http4s.ember.server.*
import com.comcast.ip4s.*

import cats.data.EitherT
import com.wallet.demo.clustering.rpc.admin as padmin

import cats.*
import cats.effect.*
//import cats.mtl.*
//import cats.FlatMap
//import cats.syntax.flatMap.*
//import cats.syntax.functor.*

object Main extends IOApp.Simple {

   //    val clientOptions = ClientOptions.default
   def mkMetadata(headers: Map[String, String]): Result[io.grpc.Metadata] = {
      val metadata = new io.grpc.Metadata()
      for (k, v) <- headers do {
         val key = io.grpc.Metadata.Key.of(k, io.grpc.Metadata.ASCII_STRING_MARSHALLER)
         metadata.put(key, v)
      }
      EitherT.right(IO.pure(metadata))
   }

   val run = IOLocal(Option.empty[domain.RequestInfo[Result]]).flatMap {
        local =>

           val grpcTargetPort = 9999
           val httpServerPort = 9001
           val channel: GrpcClientToWritesideResource = GrpcClientToWritesideResource(grpcTargetPort)
           val t =
             for {
                ch <- channel.resource
                client <- padmin.WalletCommandRpcServiceFs2Grpc.mkClientResource[Result, Map[String, String]](ch, mkMetadata)
                s = new WalletServiceImpl[Result](client)
                tracer <- auditing.Tracer.makeOtel("otel-rest-app")
                z <- monadConversions.convertResource((new SmithyResource).all(local, tracer, s), monadConversions.ioToResult)
             }
             yield (z, tracer, ch, client)

           val t1 = monadConversions.convertResource(t, monadConversions.resultToIO)

           val t2 = t1.flatMap {
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
           t2.use {
                _ =>
                   IO.never
           }
             .handleErrorWith {
                error =>
                   println(s"===> ${error.getMessage}")
                   IO.raiseError(error)
           }
   }
}
