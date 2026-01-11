package arch

import akka.cluster.sharding.typed.scaladsl.ClusterSharding

object ClusterWallet {

   class WalletSharding(
     using sys: ActorSystem[Nothing]) {

      val sharding: ClusterSharding = ClusterSharding(sys)

      export sharding.*
   }
}

object TypeKeys {

   import FrameWorkCommands.*

   val wallet: EntityTypeKey[Cmd] = EntityTypeKey[Cmd]("wallet")
}
