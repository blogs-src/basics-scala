package arch

object WalletEvents:
   sealed trait Event                       extends CborSerializable
   final case class CreditAdded(value: Int) extends Event
   final case class DebitAdded(value: Int)  extends Event
   final case class WalletCreated()         extends Event
