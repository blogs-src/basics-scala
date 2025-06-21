package arch
package di

import distage.Injector
import distage.ModuleDef
import distage.plugins.PluginConfig
import distage.plugins.PluginLoader
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import akka.cluster.sharding.typed.scaladsl.EntityContext
import arch.ClusterWallet.WalletSharding
import arch.FrameWorkCommands.{ Cmd, CmdInst }
import arch.WalletServices.Service
import arch.WalletServicesImpl.WalletServiceImpl
import com.typesafe.config.Config
import arch.TypeKeys
import akka.persistence.typed.PersistenceId
import akka.management.scaladsl.AkkaManagement
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.cluster.typed.*
import akka.actor.ActorSystem as UntypedActorSystem
import akka.cluster.ClusterEvent.*
import com.typesafe.config.Config
import akka.cluster.sharding.typed.scaladsl.Entity

import scala.util.*
import akka.event.Logging
import arch.components.wallet.WalletContainer as obj

def mkEntity(entityContext: EntityContext[CmdInst]): Behavior[CmdInst] =
   def ctxModule =
     new ModuleDef {
       make[obj.EntityConfig].from {
         val pluginConfig = PluginConfig.cached(packagesEnabled = Seq("arch.components.wallet"))
         val appModules = PluginLoader().load(pluginConfig)
         val module = appModules.result.merge
         val entity = Injector().produceGet[obj.EntityConfig](module).unsafeGet()
         entity
       }
     }

   val res: Try[Behavior[CmdInst]] = Try {
     given logger: Logger = LoggerFactory.getLogger("di")
     Injector().produceRun(ctxModule) {
       (
         entity: obj.EntityConfig
       ) =>
         entity.apply(
           PersistenceId(
             TypeKeys.wallet.name,
             entityContext.entityId))
     }
   }

   res match {
     case Success(value)     => value
     case Failure(exception) =>
       exception.printStackTrace()
       Behaviors.empty
   }
