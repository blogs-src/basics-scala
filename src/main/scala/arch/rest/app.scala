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

object Main extends IOApp.Simple:
  val run = {
    IOLocal(Option.empty[domain.RequestInfo[Result]]).flatMap { local =>

      val channel: GrpcClientToWritesideResource = GrpcClientToWritesideResource(9999)
      val t = channel.resource.flatMap{ ch =>
        auditing.Tracer.makeOtel.flatMap { (tracer: Tracer[Result]) =>
          val res: Resource[IO, HttpRoutes[IO]] = (new SmithyResource)
            .all(local, tracer, ch)
          monadConversions.convertResource(res, monadConversions.ioToResult).map(x => (x, tracer, ch))
        }
      }

      val t1 = monadConversions.convertResource(t, monadConversions.resultToIO)
      
      t1.flatMap { (routes, _, _) =>
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

