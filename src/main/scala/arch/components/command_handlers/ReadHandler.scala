package arch
package components
package command_handlers

import WalletCommands.*
import WalletEvents.*
import StateWallet.*
import Domain.*

import components.wallet.WalletContainer as obj

val ReadHandler = obj.CommandHandler {
  case (state: State, CommandsReadADT.GetBalanceCmd) => (EffectType.None, Balance(state.balance))
}
