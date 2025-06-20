package arch
package rest

import org.typelevel.otel4s.trace.Tracer
import org.typelevel.otel4s.trace.Span

object domain:
  case class RequestInfo[F[_]](headers: Map[String, String]
                               , tracer: Tracer[F]
                              )
  
