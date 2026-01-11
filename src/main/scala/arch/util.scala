package arch

case class ResultError(code: TransportError, message: String)
    extends CborSerializable

enum TransportError extends CborSerializable {
   case NotFound, BadRequest, InternalServerError, Unauthorized, Forbidden, Unknown, Maintenance
}

enum EffectType extends CborSerializable {
   case Stop, None
}

import java.io.Serializable
case class OkResponse() extends Serializable, ProtoSerializable
