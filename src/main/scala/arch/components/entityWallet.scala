package arch
package components
package wallet

import WalletCommands.*
import WalletEvents.*
import StateWallet.*

object WalletContainer extends Container[CommandsADT, CommandsADT.CreateWalletCmd.type, Event, State, OkResponse]:

   val tagger: (Option[State], Event) => Set[String] =
      case (state, _: WalletCreated) => Set("wallet-created", "UPSERT")
      case (state, _: CreditAdded)   => Set("credit-added", "UPSERT")
      case (state, _: DebitAdded)    => Set("debit-added", "UPSERT")

   val firstCommandHandler: CommandsADT => Either[ResultError, (Event, OkResponse)] =
      case CommandsADT.CreateWalletCmd => Right((WalletCreated(), OkResponse()))
      case _                           => Left(ResultError(TransportError.NotFound, "Wallet does not exists"))

   val firstEventHandler: Event => Option[State] =
      case WalletCreated() => Some(State())
      case _               => None

   val check_if_C_is_FC: CommandsADT => Option[CommandsADT.CreateWalletCmd.type] =
      case CommandsADT.CreateWalletCmd => Some(CommandsADT.CreateWalletCmd)
      case _                           => None
