package arch

import akka.cluster.sharding.typed.scaladsl.ClusterSharding

object ClusterWallet:

   class WalletSharding(
     using sys: ActorSystem[Nothing]):

      val sharding: ClusterSharding = ClusterSharding(sys)

      export sharding.*

import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.Effect
import akka.persistence.typed.scaladsl.EventSourcedBehavior
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import akka.persistence.typed.scaladsl.Effect

object TypeKeys:

   import FrameWorkCommands.*

   val wallet: EntityTypeKey[Cmd] = EntityTypeKey[Cmd]("wallet")
