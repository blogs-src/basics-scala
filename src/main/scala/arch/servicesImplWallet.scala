package arch

import arch.ClusterWallet.WalletSharding
import arch.TypeKeys

object ServicesWalletImpl:

   class WalletServiceImpl(
     entitySharding: WalletSharding,
   )(
     using sys: ActorSystem[Nothing]) extends ServicesWallet.Service:
      import WalletCommands.*
      given ec: ExecutionContextExecutor = sys.executionContext
      given timeout: Timeout = demo.timeout

      def createWallet(id: String): Future[OkResponse | ResultError] = entitySharding
        .entityRefFor(TypeKeys.wallet, id)
        .ask(FrameWorkCommands.CmdInst(CommandsADT.CreateWalletCmd, List(id), _))
        .mapTo[OkResponse | ResultError]

      def addCredit(id: String, value: Domain.Credit): Future[
        OkResponse | ResultError] = entitySharding
        .entityRefFor(TypeKeys.wallet, id)
        .ask(FrameWorkCommands.CmdInst(CommandsADT.CreditCmd(value), List(id), _))
        .mapTo[OkResponse | ResultError]

      def addDebit(id: String, value: Domain.Debit): Future[
        OkResponse | ResultError] = ???

      def getBalance(id: String): Future[Domain.Balance | ResultError] =
         println(f"Asking the balance: ${id}")
         entitySharding
           .entityRefFor(TypeKeys.wallet, id)
           .ask(
             FrameWorkCommands.CmdInst(CommandsADT.GetBalanceCmd, List(id), _))
           .mapTo[Domain.Balance | ResultError]
