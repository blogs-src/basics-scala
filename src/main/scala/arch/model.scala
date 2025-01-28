package arch

import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.Effect
import akka.persistence.typed.scaladsl.EventSourcedBehavior
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import akka.cluster.sharding.typed.scaladsl.ClusterSharding

import akka.persistence.typed.scaladsl.Effect

object domain:

   sealed trait Model extends ProtoSerializable

   final case class Balance(value: Long) extends Model

   final case class Credit(amount: Int) extends Model

   final case class Debit(amount: Int) extends Model

object infra:

   object WalletCommands:

      import domain.*

      enum CommandsADT extends ProtoSerializable:
         case CreateWalletCmd

         case CreditCmd(value: Credit)

         case DebitCmd(value: Debit)

         case StopCmd

      enum CommandsReadADT extends ProtoSerializable:
         case GetBalanceCmd

   object FrameWorkCommands:

      sealed trait Cmd extends ProtoSerializable:

         def replyTo: ActorRef[ResultError]
      case class CmdInst(payload: ProtoSerializable, params: List[String ], replyTo: ActorRef[ProtoSerializable | ResultError ]) extends Cmd

   object WalletEvents:
      sealed trait Event extends CborSerializable

      final case class CreditAdded(value: Int) extends Event

      final case class DebitAdded(value: Int) extends Event

      final case class WalletCreated() extends Event

   type ReplyEffect = akka.persistence.typed.scaladsl.ReplyEffect[WalletEvents.Event, Option[State ] ]

   case class State(balance: Long = 0) extends CborSerializable, CommandsHandler, EventsHandler

   trait CommandsHandler:
      this: State =>

      import WalletCommands.*
      import domain.*
      import WalletEvents.*
      import FrameWorkCommands.*

      def apCmd(state: State, cmd: ProtoSerializable, logger: Logger): (WalletEvents.Event | EffectType, ProtoSerializable | ResultError ) =
        cmd match {
          case CommandsADT.StopCmd           => (
              EffectType.Stop,
              OkResponse(),
            )
          case CommandsADT.CreditCmd(Credit(
                  amount,
                ),
              ) => (
              CreditAdded(amount,
              ),
              OkResponse(),
            )
          case CommandsADT.DebitCmd(Debit(amount,
                ),
              ) =>
            (DebitAdded(amount,
              ),
              OkResponse(),
            )
          case CommandsReadADT.GetBalanceCmd =>
            println(f"Balance response: ${state.balance}")
            logger.error("Getting balance")
            (EffectType.None, Balance(
                state.balance,
              ),
            )
          case CommandsADT.CreateWalletCmd   => (
              EffectType.None, ResultError(
                TransportError.BadRequest,
                "Wallet already exists",
              ),
            )
        }

      def applyCommand(cmd: Cmd,
        )
        (using logger: Logger,
        ): ReplyEffect =
        cmd match
          case CmdInst(cmd, _, replyTo) =>
            apCmd(
              this,
              cmd,
              logger,
            ) match
              case (event: WalletEvents.Event, response ) =>
                Effect.persist(event,
                ).thenReply(replyTo)(
                  _ => response,
                )
              case (EffectType.None, response,
                  ) =>
                Effect.reply(replyTo,
                )(response,
                )
              case (EffectType.Stop, response,
                  ) =>
                Effect.stop().thenReply(replyTo,
                )(_ => response,
                )

   trait EventsHandler:
      this: State =>

      import WalletEvents.*

      def applyEvent(
          event: WalletEvents.Event): State =
        event match
          case CreditAdded(amount) =>
            copy(balance = balance + amount,
            )
          case DebitAdded(
                amount,
              ) =>
            copy(balance =
              balance - amount,
            )
          case WalletCreated() => this

   object WalletEntity:

      given logger: Logger = LoggerFactory.getLogger(
        getClass,
      )

      export domain.*
      export WalletCommands.*
      export WalletEvents.*
      import FrameWorkCommands.*

      val typeKey: EntityTypeKey[Cmd ] =
        EntityTypeKey[Cmd ]("wallet",
        )

      def onFirstCommand(cmd: Cmd,
        ): ReplyEffect =
        cmd match
          case CmdInst(CommandsADT.CreateWalletCmd, _, replyTo,
              ) => Effect.persist(WalletCreated(
              ),
            ).thenReply(replyTo)(
                _ => OkResponse(),
              )
          case default =>
            Effect
              .none
              .thenReply(default.replyTo,
              )(_ =>
                  ResultError(TransportError.NotFound, "Wallet does not exists",
                  ),
              )

      def onFirstEvent(event: Event): State =
        event match
          case WalletCreated() => State()
          case _               =>
            throw new IllegalStateException(
              s"unexpected event [$event] in empty state",
            )

      def apply(
          persistenceId: PersistenceId): Behavior[Cmd ] = Behaviors.setup[Cmd]:
           context =>
              EventSourcedBehavior.withEnforcedReplies[Cmd, Event, Option[State ] ](persistenceId,
                None,
                (state, cmd) =>
                  state match {
                    case None =>
                      onFirstCommand(cmd)
                    case Some(
                          state,
                        ) =>
                      state.applyCommand(cmd)
                  },
                (state, event) =>
                  state match {
                    case None =>
                      Some(onFirstEvent(event),
                      )
                    case Some(state) =>
                      Some(state.applyEvent(event))
                  },
              )
                .withTaggerForState:
                   case (state, _: WalletCreated ) =>
                     Set("wallet-created", "UPSERT")
                   case (state, _: CreditAdded ) =>
                     Set("credit-added", "UPSERT")
                   case (state, _: DebitAdded ) =>
                     Set("debit-added", "UPSERT")

   trait WalletService:

      def createWallet(id: String): Future[OkResponse | ResultError ]

      def addCredit(id: String, value: domain.Credit): Future[OkResponse | ResultError ]

      def addDebit(id: String, value: domain.Debit): Future[OkResponse | ResultError ]

      def getBalance(id: String): Future[domain.Balance | ResultError ]

   class WalletSharding(using sys: ActorSystem[Nothing ]):

      val sharding: ClusterSharding = ClusterSharding(sys)

      export sharding.*

   class WalletServiceImpl(entitySharding: WalletSharding)(
        using sys: ActorSystem[Nothing]) extends WalletService:
      import WalletCommands.*

      given ec: ExecutionContextExecutor = sys.executionContext
      given timeout: Timeout = demo.timeout

      def createWallet(id: String): Future[OkResponse | ResultError ] = entitySharding
        .entityRefFor(WalletEntity.typeKey, id)
        .ask(FrameWorkCommands.CmdInst(CommandsADT.CreateWalletCmd, List(id), _))
        .mapTo[OkResponse | ResultError ]

      def addCredit(id: String, value: domain.Credit): Future[OkResponse | ResultError ] = entitySharding
        .entityRefFor(WalletEntity.typeKey, id)
        .ask(FrameWorkCommands.CmdInst(CommandsADT.CreditCmd(value), List(id), _))
        .mapTo[OkResponse | ResultError ]

      def addDebit(id: String, value: domain.Debit): Future[OkResponse | ResultError ] = ???

      def getBalance(id: String): Future[domain.Balance | ResultError ] =
         println(f"Asking the balance: ${id}")
         entitySharding
           .entityRefFor(WalletEntity.typeKey, id)
           .ask(
             FrameWorkCommands.CmdInst(CommandsReadADT.GetBalanceCmd, List(id), _))
           .mapTo[domain.Balance | ResultError ]
