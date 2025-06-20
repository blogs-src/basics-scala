package arch
package components
package command_handlers

import WalletCommands.*
import WalletEvents.*
import StateWallet.*

import components.wallet.WalletContainer as obj

val ReadHandler = obj.CommandHandler {
  case (state: State, (CommandsADT.GetBalanceCmd, ctx)) =>
    println(s"GetBalanceCmd, with state: $state, and context => $ctx")
    (EffectType.None, Domain.Balance(state.balance))
}
