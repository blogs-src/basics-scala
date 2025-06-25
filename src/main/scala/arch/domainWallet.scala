package arch

object Domain:

   sealed trait Model                    extends ProtoSerializable
   final case class Balance(value: Long) extends Model
   final case class Credit(amount: Int)  extends Model
   final case class Debit(amount: Int)   extends Model

object StateWallet:
   case class State(balance: Long = 0) extends CborSerializable
