package arch

import cats.effect.*
import cats.implicits.*
import cats.mtl.*
//import fs2.concurrent.Channel

import cats.*

object UtilsRPC {

   def reportError[F[_], A](
     code:     TransportError,
     message:  String,
   )(
     using FR: Raise[F, ServiceError],
   ): F[A] =
     code match {
       case TransportError.NotFound => FR.raise(ErrorsBuilder.notFoundError(message))
       case _                       => FR.raise(ErrorsBuilder.internalServerError(message))
     }
}

class WalletServiceIOImpl[F[_]](
  wService: WalletServices.Service,
)(
  using ec: ExecutionContextExecutor,
  F:        Async[F],
  FR:       Raise[F, ServiceError],
  M:        Monad[F],
  MT: MonadThrow[F]) extends WalletServiceIO[F] {

   import UtilsRPC.*

   def getBalance(
     id:             String,
   )(
     using metadata: Map[String, String] = Map.empty,
   ): F[Domain.Balance] =
     for {
        res <- F.fromFuture(wService.getBalance(id).pure[F])
        balance <-
          res match {
            case b: Domain.Balance          => F.pure(b)
            case ResultError(code, message) => reportError(code, message)
          }
     }
     yield balance
}
