package arch

object WalletEvents:
   // this generates the protobuf and the mapping using chimney
   // the result goes to two string fields in WalletEvents object
   // @akkaEvents
   sealed trait Event                       extends CborSerializable
   final case class CreditAdded(value: Int) extends Event
   final case class DebitAdded(value: Int)  extends Event
   final case class WalletCreated()         extends Event
