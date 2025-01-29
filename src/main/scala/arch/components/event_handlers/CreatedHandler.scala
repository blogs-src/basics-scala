package arch
package components
package event_handlers

import WalletCommands.*
import WalletEvents.*
import StateWallet.*
import Domain.*

import components.wallet.WalletContainer as obj

val CreatedHandler = obj.EventHandler {
  case (s: State, WalletCreated()) =>
    println("Wallet created")
    s
}
