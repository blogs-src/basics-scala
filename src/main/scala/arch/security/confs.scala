package arch
package security

import scala.jdk.CollectionConverters.*
//import scala.concurrent.ExecutionContext.global
import org.http4s.blaze.client.BlazeClientBuilder

import com.nimbusds.jose.Payload
import scala.concurrent.duration.*


class Keycloak(basePath: String, realm: String) {
  def jwksUrl: String = s"${basePath}/realms/$realm/protocol/openid-connect/certs"
}

class Krakend(val jwks_url: String)


object KeycloakConfs {
  given ValidatorSource[Keycloak] with
    extension (self: Keycloak) def jwksUrl: String = self.jwksUrl
    extension (self: Keycloak) def id(payload: Payload): String = {
      payload.toJSONObject.get("sid").asInstanceOf[String]
    }
    extension (self: Keycloak) def roles(payload: Payload): Set[String] = {
      val realm_access = payload.toJSONObject.get("realm_access")
      realm_access.asInstanceOf[java.util.Map[String, java.lang.Object]].get("roles").asInstanceOf[java.util.List[java.lang.String]].asScala.toSet
    }

}

object KrakendConfs {
  given ValidatorSource[Krakend] with
    extension (self: Krakend) def jwksUrl: String = self.jwks_url
    extension (self: Krakend) def id(payload: Payload): String = {
      payload.toJSONObject.get("sub").asInstanceOf[String]
    }
    extension (self: Krakend) def roles(payload: Payload): Set[String] = {
      payload.toJSONObject.get("roles").asInstanceOf[java.util.List[java.lang.String]].asScala.toSet
    }

}
