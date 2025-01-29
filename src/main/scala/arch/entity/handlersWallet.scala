package arch

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import akka.persistence.typed.scaladsl.Effect

object HandlersWallet:

   import StateWallet.State
   import EntityWallet.ReplyEffect

   trait CommandsHandler:

      import WalletCommands.*
      import Domain.*
      import WalletEvents.*
      import FrameWorkCommands.*

      def apCmd(
        state:  State,
        cmd:    ProtoSerializable,
        logger: Logger,
      ): (WalletEvents.Event | EffectType, ProtoSerializable | ResultError) =
        cmd match {
          case CommandsADT.StopCmd                   => (EffectType.Stop, OkResponse())
          case CommandsADT.CreditCmd(Credit(amount)) => (CreditAdded(amount), OkResponse())
          case CommandsADT.DebitCmd(Debit(amount))   => (DebitAdded(amount), OkResponse())
          case CommandsADT.GetBalanceCmd         =>
            println(f"Balance response: ${state.balance}")
            logger.error("Getting balance")
            (EffectType.None, Balance(state.balance))
          case CommandsADT.CreateWalletCmd           => (EffectType.None, ResultError(TransportError.BadRequest, "Wallet already exists"))
        }

      def applyCommand(
        state:        State,
        cmd:          Cmd,
      )(
        using logger: Logger,
      ): ReplyEffect =
        cmd match
          case CmdInst(cmd, _, replyTo) =>
            apCmd(
              state,
              cmd,
              logger) match
              case (event: WalletEvents.Event, response) =>
                Effect.persist(event).thenReply(replyTo)(
                  _ => response)
              case (EffectType.None, response)           => Effect.reply(replyTo)(response)
              case (EffectType.Stop, response)           =>
                Effect.stop().thenReply(replyTo)(
                  _ => response)

   trait EventsHandler:

      import WalletEvents.*

      def applyEvent(state: State, event: WalletEvents.Event): State =
        event match
          case CreditAdded(amount) => state.copy(balance = state.balance + amount)
          case DebitAdded(amount)  => state.copy(balance = state.balance - amount)
          case WalletCreated()     => state
