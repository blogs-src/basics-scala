package arch

import cats.data.EitherT
import cats.effect.*
import cats.implicits.*
import com.google.rpc.Code
import com.wallet.demo.clustering.rpc.admin.*
import com.wallet.proto.messages.commands as commands

//import fs2.concurrent.Channel
import io.scalaland.chimney.Transformer
import io.scalaland.chimney.dsl.*
import io.scalaland.chimney.partial.syntax.*

import akka.grpc.GrpcServiceException

trait ExceptionGenerator[F]:
  def generateException(msg: String): Throwable

object ExceptionGenerator:
  def apply[F](using obj: ExceptionGenerator[F]): ExceptionGenerator[F] = obj

class MyTransformers[G: ExceptionGenerator]:

  import scala.reflect.*

  transparent inline given TransformerConfiguration[?] =
    TransformerConfiguration.default
      .enableDefaultValues
      //      .enableMacrosLogging
      .enableImplicitConversions
      //      .disablePartialUnwrapsOption
      .enableInheritedAccessors //.enablePartialUnwrapsOption

  implicit def eitherToResultTransformers[A: ClassTag]: Transformer[IO[Either[Throwable, A]], Result[A]] =
    new Transformer[IO[Either[Throwable, A]], Result[A]]:
      def transform(result: IO[Either[Throwable, A]]): Result[A] = {
        EitherT(result.map {
          case Right(value) => Right(value)
          case Left(error) => Left(ErrorsBuilder.internalServerError(error.getMessage))
        })
      }


  implicit def othersTransformers[A: ClassTag]: Transformer[Result[A], IO[A]] =
    new Transformer[Result[A], IO[A]]:
      def transform(result: Result[A]): IO[A] = {

        result.foldF(
          error =>
            IO.raiseError(
              error match {

                case e: ServiceUnavailable =>
                  val error = ServiceUnavailableError(e.code, e.title, e.message)
                  GrpcServiceException(Code.INTERNAL, e.message, Seq(error))

                case e: Conflict =>
                  val error = ServiceUnavailableError(e.code, e.title, e.message)
                  GrpcServiceException(Code.INTERNAL, e.message, Seq(error))

                case e: BadRequest =>
                  // val trailers = new Metadata()
                  // TODO: put the proto message in the trailers
                  // https://github.com/grpc/grpc-java/blob/master/examples/src/main/java/io/grpc/examples/errorhandling/DetailErrorSample.java#L82
                  // Status.INVALID_ARGUMENT.withDescription(e.message).asRuntimeException(trailers)
                  val error = BadRequestError(e.code, e.title, e.message)
                  GrpcServiceException(Code.INVALID_ARGUMENT, e.message, Seq(error))

                case e: NotFound =>
                  val error = NotFoundError(e.code, e.title, e.message)
                  GrpcServiceException(Code.NOT_FOUND, e.message, Seq(error))

                case e: InternalServer =>
                  val error = InternalServerError(e.code, e.title, e.message)
                  GrpcServiceException(Code.INTERNAL, e.message, Seq(error))

                case e: Unauthorized =>
                  val error = UnauthorizedError(e.code, e.title, e.message)
                  GrpcServiceException(Code.UNAUTHENTICATED, e.message, Seq(error))

                case e: Forbidden =>
                  val error = ForbiddenError(e.code, e.title, e.message)
                  GrpcServiceException(Code.PERMISSION_DENIED, e.message, Seq(error))
              }
            ),
          value => IO {
            value
          }
        )
      }

trait WalletServiceIO2[F[_]]:
  def getBalance(id: String): F[Domain.Balance]

import cats.mtl.*
import cats.*

object UtilsRPC:
  def reportError[F[_], A](code: TransportError, message: String)(using FR: Raise[F, ServiceError]): F[A] =
    code match {
      case TransportError.NotFound => FR.raise(ErrorsBuilder.notFoundError(message))
      case _ => FR.raise(ErrorsBuilder.internalServerError(message))
    }


class WalletServiceIOImpl2[F[_]]
(
  wService: ServicesWallet.Service,
  )(using ec: ExecutionContextExecutor, F: Async[F], FR: Raise[F, ServiceError], M: Monad[F], MT: MonadThrow[F]) extends WalletServiceIO2[F]:

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

import io.grpc.Metadata

trait ClusteringWalletGrpcService[F[_]] {
  def getBalance(request: GetBalanceRequest, ctx: Metadata): F[commands.Balance]
}

case class BalanceRequest(id: RequestId) //{
//  require(id.nonEmpty, "id cannot be empty")
//}

case class RequestId(value: String)
//{
//  require(value.nonEmpty, "id cannot be empty")
//}
class ClusteringWalletGrpcServiceImpl[F[_], G: ExceptionGenerator]
(service: WalletServiceIO2[F])(using transformers: MyTransformers[G])(using F: Async[F], FR: Raise[F, ServiceError], M: Monad[F], MT: MonadThrow[F])
  extends ClusteringWalletGrpcService[F]:

    import io.scalaland.chimney.partial
    import io.scalaland.chimney.protobufs.*
    import transformers.given

    private def validateRequestId(request: GetBalanceRequest): F[BalanceRequest] =
      val res = request.transformIntoPartial[BalanceRequest].asEither.asResult.asEitherErrorPathMessageStrings
      res match
        case Right(r) => r.pure[F]
        case Left(e) =>
              val (key, value) = e.toList.head
              FR.raise(ErrorsBuilder.badRequestError(s"$key: $value"))

    def getBalance(request: GetBalanceRequest, ctx: Metadata): F[commands.Balance] = {
      commands.Balance(100).pure[F]
      // MT.raiseError(ErrorsBuilder.notFoundError("Not found"))
      // FR.raise(ErrorsBuilder.badRequestError("bad request"))
      for {
        r <- validateRequestId(request)
        //res <- service.getBalance(r.id.get.value.get)
//        _ <- F.pure{println(s"yeeeee ${Try{r.id.get.value.get}}")}
        res <- service.getBalance(r.id.value)
//        res <- service.getBalance("r.id.value")
      } yield commands.Balance(res.value)

    }
//    serviceP match {
//      case service: WalletEventSourcing.WalletServiceIO[F] =>
//        for {
//          r <- validateNonNullId(request)
//          res <- service.getBalance(r.id)
//
//        } yield BalanceResponse(res.value)
//      case service: WalletEventSourcing.WalletServiceIO2[F] =>
//        for {
//          r <- validateNonNullId(request)
//          res <- service.getBalance(r.id)
//
//        } yield BalanceResponse(res.value)
//    }
