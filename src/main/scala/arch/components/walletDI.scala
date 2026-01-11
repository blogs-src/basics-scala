package arch
package components
package wallet

import scala.reflect.ClassTag
import izumi.distage.plugins.PluginDef

import WalletCommands.*
import WalletEvents.*
import StateWallet.*

import command_handlers.*
import event_handlers.*

import components.wallet.WalletContainer as obj

object WalletModule extends PluginDef {

   many[obj.CommandHandler]
     .add(
       ReadHandler)

   many[obj.CommandHandler]
     .add(
       OperationsHandler)

   many[obj.EventHandler]
     .add(
       CreatedHandler)

   many[obj.EventHandler]
     .add(
       MoneyMovementHandler)

   make[String].from("wallet")
   make[obj.AppCommands].from[obj.AppCommands.Impl]
   make[obj.AppEvents].from[obj.AppEvents.Impl]

   makeTrait[obj.Handler]
   makeTrait[obj.EntityConfig]

   make[(Option[State], Event) => Set[String]].from {
        obj.tagger
   }

   make[CommandsADT.CreateWalletCmd.type => Either[ResultError, (Event, OkResponse)]].from {
        obj.firstCommandHandler
   }

   make[Event => Option[State]].from {
        obj.firstEventHandler
   }

   make[CommandsADT => Option[CommandsADT.CreateWalletCmd.type]].from {
        obj.check_if_C_is_FC
   }

   make[ClassTag[CommandsADT]].from {
        ClassTag(classOf[CommandsADT])
   }

   make[ClassTag[Event]].from {
        ClassTag(classOf[Event])
   }
}
