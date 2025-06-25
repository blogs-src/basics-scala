package arch

import cats.data.EitherT
import cats.effect.*
import com.google.rpc.Code
import com.wallet.demo.clustering.rpc.admin.*
//import fs2.concurrent.Channel
import io.scalaland.chimney.Transformer
import io.scalaland.chimney.dsl.*

import cats.*

import akka.grpc.GrpcServiceException

class MyTransformers[G: ExceptionGenerator]:

   import scala.reflect.*

   transparent inline given TransformerConfiguration[?] =
     TransformerConfiguration.default
       .enableDefaultValues
       //      .enableMacrosLogging
       .enableImplicitConversions
       //      .disablePartialUnwrapsOption
       .enableInheritedAccessors // .enablePartialUnwrapsOption

   implicit def eitherToResultTransformers[A: ClassTag]: Transformer[IO[Either[Throwable, A]], Result[A]] =
     new Transformer[IO[Either[Throwable, A]], Result[A]]:
        def transform(result: IO[Either[Throwable, A]]): Result[A] = EitherT(result.map {
          case Right(value) => Right(value)
          case Left(error)  => Left(ErrorsBuilder.internalServerError(error.getMessage))
        })

   implicit def othersTransformers[A: ClassTag]: Transformer[Result[A], IO[A]] =
     new Transformer[Result[A], IO[A]]:
        def transform(result: Result[A]): IO[A] =

          result.foldF(
            error =>
              IO.raiseError(
                error match

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
                    GrpcServiceException(Code.PERMISSION_DENIED, e.message, Seq(error))),
            value =>
              IO {
                value
              })
