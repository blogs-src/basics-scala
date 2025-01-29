package arch
package components
package command_handlers

import WalletCommands.*
import WalletEvents.*
import StateWallet.*

import components.wallet.WalletContainer as obj

val ReadHandler = obj.CommandHandler {
  case (state: State, CommandsReadADT.GetBalanceCmd) =>
    println(s"GetBalanceCmd, with state: $state")
    (EffectType.None, Domain.Balance(state.balance))
}
