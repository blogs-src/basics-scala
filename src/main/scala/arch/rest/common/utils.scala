package arch
package rest

import smithy_rest.wallet_ops.*
import smithy4s.kinds.PolyFunction
import cats.effect.*

object Converter:

   val toIO: PolyFunction[Result, IO] =
     new PolyFunction[Result, IO]:

        def apply[A](result: Result[A]): IO[A] = result.foldF(
          error =>
            IO.raiseError(
              error match
                case e: ServiceUnavailable => ServiceUnavailableError(e.code, e.title, e.message)
                case e: Conflict           => ConflictError(e.code, e.title, e.message)
                case e: BadRequest         =>
                  //                  println("BadRequest")
                  BadRequestError(e.code, e.title, e.message)
                case e: NotFound           => NotFoundError(e.code, e.title, e.message)
                case e: InternalServer     =>
                  //                  println("InternalServer")
                  InternalServerError(e.code, e.title, e.message)
                case e: Unauthorized       => UnauthorizedError(e.code, e.title, e.message)
                case e: Forbidden          => ForbiddenError(e.code, e.title, e.message)),
          value =>
            IO {
              value
            })
