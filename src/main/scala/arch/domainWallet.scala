package arch

import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.Effect
import akka.persistence.typed.scaladsl.EventSourcedBehavior
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import akka.cluster.sharding.typed.scaladsl.ClusterSharding

import akka.persistence.typed.scaladsl.Effect

object Domain:

   sealed trait Model extends ProtoSerializable
   final case class Balance(value: Long) extends Model
   final case class Credit(amount: Int) extends Model
   final case class Debit(amount: Int) extends Model

object StateWallet:
   case class State(balance: Long = 0) extends CborSerializable
