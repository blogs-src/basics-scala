package arch
package components
package command_handlers

import WalletCommands.*
import WalletEvents.*
import StateWallet.*
import Domain.*

import components.wallet.WalletContainer as obj

val OperationsHandler = obj.CommandHandler {
  case (s: State, (CommandsADT.CreditCmd(Credit(amount)), ctx)) => (CreditAdded(amount), OkResponse())
  case (s: State, (CommandsADT.DebitCmd(Debit(amount)), ctx))   => (DebitAdded(amount), OkResponse())
}
