package arch

import arch.ClusterWallet.WalletSharding
import arch.FrameWorkCommands.CmdInst

object WalletServicesImpl:

   class WalletServiceImpl(entitySharding: WalletSharding, timeout: Timeout) extends WalletServices.Service:
//      import WalletCommands.*

      def createWallet(id: String): Future[OkResponse | ResultError] =
         // val command: ActorRef[ProtoSerializable | ResultError] => CmdInst = FrameWorkCommands.CmdInst(CommandsADT.CreateWalletCmd, List(id), _)
         // val command: ActorRef[ProtoSerializable | ResultError] => CmdInst = (arg: ActorRef[ProtoSerializable | ResultError]) => FrameWorkCommands.CmdInst(CommandsADT.CreateWalletCmd, List(id), arg)
         def command(arg: ActorRef[ProtoSerializable | ResultError]): CmdInst = {
           FrameWorkCommands.CmdInst(WalletCommands.CommandsADT.CreateWalletCmd, List(id), arg)
         }
         entitySharding
           .entityRefFor(TypeKeys.wallet, id)
           .ask(command)(using timeout)
           .mapTo[OkResponse | ResultError]

      def credit(id: String, value: Domain.Credit): Future[
        OkResponse | ResultError] = entitySharding
        .entityRefFor(TypeKeys.wallet, id)
        .ask(FrameWorkCommands.CmdInst(WalletCommands.CommandsADT.CreditCmd(value), List(id), _))(using timeout)
        .mapTo[OkResponse | ResultError]

      def debit(id: String, value: Domain.Debit): Future[
        OkResponse | ResultError] = ???

      def getBalance(id: String): Future[Domain.Balance | ResultError] =
         println(f"Asking the balance: ${id}")
         entitySharding
           .entityRefFor(TypeKeys.wallet, id)
           .ask(
             FrameWorkCommands.CmdInst(WalletCommands.CommandsADT.GetBalanceCmd, List(id), _))(using timeout)
           .mapTo[Domain.Balance | ResultError]
