package arch
package rest

import org.typelevel.otel4s.trace.Tracer

object domain {

   case class RequestInfo[F[_]](
     headers: Map[String, String],
     tracer:  Tracer[F],
     userId:  Option[String])
}
