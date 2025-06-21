package arch

import cats.data.EitherT
import cats.effect.*
import fs2.grpc.syntax.all.*
import io.grpc.*
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder
//import io.grpc.okhttp.OkHttpServerBuilder
import io.grpc.protobuf.services.ProtoReflectionServiceV1
// import io.scalaland.chimney.*

import cats.effect.kernel.Resource
import cats.~>
import cats.arrow.FunctionK
import cats.syntax.all.*

import cats.mtl.*
import org.typelevel.otel4s.trace.Tracer

import com.wallet.demo.clustering.rpc.admin.*

object monadConversions:

   //  def ioToResult[T](io: IO[T]): Result[T] = ???
   def convertResource[F[_]: MonadCancelThrow, G[_]: MonadCancelThrow, A](resource: Resource[F, A], nt: F ~> G): Resource[G, A] = resource.mapK(nt)

   //  val optionToList: Option ~> List = [A] => (a: Option[A]) => a.toList

   val optionToList: Option ~> List =
     new FunctionK[Option, List]:
        def apply[A](fa: Option[A]): List[A] = ???

   val ioToResult: IO ~> Result =
     new FunctionK[IO, Result]:
        def apply[A](fa: IO[A]): Result[A] = EitherT.right(fa)

   val resultToIO: Result ~> IO =
     new FunctionK[Result, IO]:
        def apply[A](fa: Result[A]): IO[A] = fa.foldF(
          error => IO.raiseError(error),
          value =>
            IO {
              value
            })

class GrpcServerResource:

   //    def run[F[_]: Async](service: ServerServiceDefinition): Resource[F, Server] =
   def createIO[F[_]: Async](service: ServerServiceDefinition): Resource[F, Server] =
     //      val creds = TlsServerCredentials.create(certChainFile, privateKeyFile)
     //      val creds = InsecureServerCredentials.create()
     NettyServerBuilder
       .forPort(9999)
       //      OkHttpServerBuilder
       //      .forPort(8090, creds)
       .addService(service)
       .addService(ProtoReflectionServiceV1.newInstance())
       .resource[F]
