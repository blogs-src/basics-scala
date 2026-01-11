package arch
package rest

import smithy4s.http4s.ServerEndpointMiddleware
import cats.data.*
import org.http4s.HttpRoutes
import org.typelevel.otel4s.trace.Tracer
import org.typelevel.ci.CIString
import cats.effect.*
import cats.implicits.*
import org.http4s.implicits.*
import org.http4s.*
import smithy4s.Hints

// https://github.com/http4s/http4s/tree/series/0.23/server/shared/src/main/scala/org/http4s/server/middleware
object RequestInfoMiddleware {

   def appToRoutes(app: HttpApp[IO]): HttpRoutes[IO] = Kleisli {
        req => OptionT.liftF(app(req))
   }

   def routesToApp(routes: HttpRoutes[IO]): HttpApp[IO] = routes.orNotFound

   def apply(
     local:  IOLocal[Option[domain.RequestInfo[Result]]],
     tracer: Tracer[Result],
   ): ServerEndpointMiddleware[IO] =

     new ServerEndpointMiddleware.Simple[IO] {

        def prepareWithHints(
          serviceHints:  Hints,
          endpointHints: Hints,
        ): HttpApp[IO] => HttpApp[IO] = {
          inputApp =>
             val routes = appToRoutes(inputApp)
             val nroutes = HttpRoutes[IO] {
                  request =>
                     println("withRequestInfo2 <<<...............................>>>")
                     val hnames = request.headers.headers.map(_.name.toString)
                     val hvals = hnames.map(
                       key => (key, request.headers.get(CIString(key)).map(_.head.value).get))
                     val hvals2 = Map.from(hvals)
                     val userId = request.attributes.lookup(Attrs.UserId)
                     val requestInfo = Some(domain.RequestInfo[Result](hvals2, tracer, userId))
                     OptionT.liftF(local.set(requestInfo)) *> routes(request)
             }
             routesToApp(nroutes)
        }
     }
}
