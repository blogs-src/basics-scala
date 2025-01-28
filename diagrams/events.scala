final case class CreditAdded(value: Int) extends Event
final case class DebitAdded(value: Int)  extends Event
final case class WalletCreated()         extends Event
case class Other()         extends Event
