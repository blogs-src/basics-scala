package arch

import arch.ClusterWallet.WalletSharding
import arch.TypeKeyWallet

object ServicesWalletImpl:

   class WalletServiceImpl(
     entitySharding: WalletSharding,
   )(
     using sys: ActorSystem[Nothing]) extends ServicesWallet.Service:

      import WalletCommands.*

      given ec: ExecutionContextExecutor = sys.executionContext

      given timeout: Timeout = demo.timeout

      def createWallet(id: String): Future[OkResponse | ResultError] = entitySharding
        .entityRefFor(TypeKeyWallet.key, id)
        .ask(FrameWorkCommands.CmdInst(CommandsADT.CreateWalletCmd, List(id), _))
        .mapTo[OkResponse | ResultError]

      def addCredit(id: String, value: domain.Credit): Future[
        OkResponse | ResultError] = entitySharding
        .entityRefFor(TypeKeyWallet.key, id)
        .ask(FrameWorkCommands.CmdInst(CommandsADT.CreditCmd(value), List(id), _))
        .mapTo[OkResponse | ResultError]

      def addDebit(id: String, value: domain.Debit): Future[
        OkResponse | ResultError] = ???

      def getBalance(id: String): Future[domain.Balance | ResultError] =
         println(f"Asking the balance: ${id}")
         entitySharding
           .entityRefFor(TypeKeyWallet.key, id)
           .ask(
             FrameWorkCommands.CmdInst(CommandsReadADT.GetBalanceCmd, List(id), _))
           .mapTo[domain.Balance | ResultError]
