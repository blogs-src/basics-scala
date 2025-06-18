package arch

import cats.data.EitherT
import cats.effect.*
import cats.implicits.*
import com.google.rpc.Code
import com.wallet.demo.clustering.rpc.admin.*
import com.wallet.proto.messages.commands
import cats.mtl.*
import org.typelevel.otel4s.Attribute
//import fs2.concurrent.Channel
import io.scalaland.chimney.Transformer
import io.scalaland.chimney.dsl.*
import io.scalaland.chimney.partial.syntax.*

import cats.*

import akka.grpc.GrpcServiceException

import io.grpc.Metadata

import org.typelevel.otel4s.trace.Tracer

import scala.jdk.CollectionConverters.*

object UtilsRPC:
  def reportError[F[_], A](code: TransportError, message: String)(using FR: Raise[F, ServiceError]): F[A] =
    code match {
      case TransportError.NotFound => FR.raise(ErrorsBuilder.notFoundError(message))
      case _ => FR.raise(ErrorsBuilder.internalServerError(message))
    }


class WalletServiceIOImpl[F[_]]
(
  wService: WalletServices.Service,
  )(using ec: ExecutionContextExecutor, F: Async[F], FR: Raise[F, ServiceError], M: Monad[F], MT: MonadThrow[F]) extends WalletServiceIO[F]:

  import UtilsRPC.*

  def getBalance(id: String): F[Domain.Balance] =
    for {
      res <- F.fromFuture(wService.getBalance(id).pure[F])
      balance <-
        res match {
          case b: Domain.Balance => F.pure(b)
          case ResultError(code, message) => reportError(code, message)
        }
    } yield balance

