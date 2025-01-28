package arch

import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.Effect
import akka.persistence.typed.scaladsl.EventSourcedBehavior
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import akka.persistence.typed.scaladsl.Effect

object TypeKeyWallet:

   import FrameWorkCommands.*

   val key: EntityTypeKey[Cmd] = EntityTypeKey[Cmd]("wallet")

object EntityWallet:

   import StateWallet.State

   type ReplyEffect = akka.persistence.typed.scaladsl.ReplyEffect[WalletEvents.Event, Option[State]]

   object Entity extends HandlersWallet.CommandsHandler, HandlersWallet.EventsHandler:

      given logger: Logger = LoggerFactory.getLogger(
        getClass)

      export domain.*
      export WalletCommands.*
      export WalletEvents.*
      import FrameWorkCommands.*

      def onFirstCommand(cmd: Cmd): ReplyEffect =
        cmd match
          case CmdInst(CommandsADT.CreateWalletCmd, _, replyTo) =>
            Effect.persist(WalletCreated()).thenReply(replyTo)(
              _ => OkResponse())
          case default                                          =>
            Effect
              .none
              .thenReply(default.replyTo)(
                _ =>
                  ResultError(TransportError.NotFound, "Wallet does not exists"))

      def onFirstEvent(event: Event): State =
        event match
          case WalletCreated() => State()
          case _               =>
            throw new IllegalStateException(
              s"unexpected event [$event] in empty state")

      def apply(persistenceId: PersistenceId): Behavior[
        Cmd] = Behaviors.setup[Cmd]:
           context =>
              EventSourcedBehavior.withEnforcedReplies[Cmd, Event, Option[State]](
                persistenceId,
                None,
                (state, cmd) =>
                  state match {
                    case None => onFirstCommand(cmd)
                    case Some(
                          state) =>
                      applyCommand(state, cmd)
                  },
                (state, event) =>
                  state match {
                    case None        => Some(onFirstEvent(event))
                    case Some(state) => Some(applyEvent(state, event))
                  })
                .withTaggerForState:
                   case (state, _: WalletCreated) => Set("wallet-created", "UPSERT")
                   case (state, _: CreditAdded)   => Set("credit-added", "UPSERT")
                   case (state, _: DebitAdded)    => Set("debit-added", "UPSERT")
