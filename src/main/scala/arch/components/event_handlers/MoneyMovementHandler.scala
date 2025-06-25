package arch
package components
package event_handlers

import WalletEvents.*
import StateWallet.*

import components.wallet.WalletContainer as obj

val MoneyMovementHandler = obj.EventHandler:
     case (s: State, CreditAdded(amount)) =>
//    println("Credit added")
       s.copy(balance = s.balance + amount)
     case (s: State, DebitAdded(amount))  =>
//    println("Debit added")
       s.copy(balance = s.balance - amount)
