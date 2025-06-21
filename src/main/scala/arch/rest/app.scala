package arch
package rest

import smithy_rest.wallet_ops.*
import cats.effect.*
import cats.effect._
import cats.implicits._
import org.http4s.implicits._
import org.http4s.ember.server._
import org.http4s._
import com.comcast.ip4s._
import smithy4s.http4s.SimpleRestJsonBuilder

import org.typelevel.otel4s.trace.Tracer

import cats.data.EitherT
import com.wallet.demo.clustering.rpc.admin as padmin
import cats.mtl.*
import fs2.grpc.client.ClientOptions
import cats.effect.std.Dispatcher

object Main extends IOApp.Simple:
  val run = {
    IOLocal(Option.empty[domain.RequestInfo[Result]]).flatMap { local =>

      val channel: GrpcClientToWritesideResource = GrpcClientToWritesideResource(9999)
      val t = channel.resource.flatMap{ ch =>

        //    val clientOptions = ClientOptions.default
        def mkMetadata(headers: Map[String, String]): Result[io.grpc.Metadata] = {
          val metadata = new io.grpc.Metadata()
          for (k, v) <- headers do
            val key = io.grpc.Metadata.Key.of(k, io.grpc.Metadata.ASCII_STRING_MARSHALLER)
            metadata.put(key, v)
          EitherT.right(IO.pure(metadata))
        }

        val clientResource = padmin.WalletCommandRpcServiceFs2Grpc.mkClientResource[Result, Map[String, String]](ch, mkMetadata)
        clientResource.flatMap{ client =>
          auditing.Tracer.makeOtel("otel-rest-app").flatMap { (tracer: Tracer[Result]) =>
            val res: Resource[IO, HttpRoutes[IO]] = (new SmithyResource)
              .all(local, tracer, client)
            monadConversions.convertResource(res, monadConversions.ioToResult).map(x => (x, tracer, ch, client))
          }

        }

      }

      val t1 = monadConversions.convertResource(t, monadConversions.resultToIO)

      t1.flatMap { (routes, _, _, _) =>
        EmberServerBuilder
          .default[IO]
          .withPort(port"9000")
          .withHost(host"0.0.0.0")
          .withHttpApp(routes.orNotFound)
          .build
      }.use(_ => IO.never)
        .handleErrorWith { error =>
          println(s"===> ${error.getMessage}")
          IO.raiseError(error)
        }

    }
  }

