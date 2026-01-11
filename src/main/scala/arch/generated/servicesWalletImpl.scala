package arch

import arch.ClusterWallet.WalletSharding
import arch.FrameWorkCommands.CmdInst

object WalletServicesImpl {

   class WalletServiceImpl(entitySharding: WalletSharding, timeout: Timeout) extends WalletServices.Service {
//      import WalletCommands.*

      def createWallet(
        id:             String,
      )(
        using metadata: Map[String, String] = Map.empty,
      ): Future[OkResponse | ResultError] = {
         // val command: ActorRef[ProtoSerializable | ResultError] => CmdInst = FrameWorkCommands.CmdInst(CommandsADT.CreateWalletCmd, List(id), _)
         // val command: ActorRef[ProtoSerializable | ResultError] => CmdInst = (arg: ActorRef[ProtoSerializable | ResultError]) => FrameWorkCommands.CmdInst(CommandsADT.CreateWalletCmd, List(id), arg)
         def command(arg: ActorRef[ProtoSerializable | ResultError]): CmdInst = FrameWorkCommands.CmdInst(WalletCommands.CommandsADT.CreateWalletCmd,
                                                                                                          metadata,
                                                                                                          arg)
         entitySharding
           .entityRefFor(TypeKeys.wallet, id)
           .ask(command)(
             using timeout)
           .mapTo[OkResponse | ResultError]
      }

      def credit(
        id:             String,
        value:          Domain.Credit,
      )(
        using metadata: Map[String, String] = Map.empty,
      ): Future[
        OkResponse | ResultError] = entitySharding
        .entityRefFor(TypeKeys.wallet, id)
        .ask(FrameWorkCommands.CmdInst(WalletCommands.CommandsADT.CreditCmd(value), metadata, _))(
          using timeout)
        .mapTo[OkResponse | ResultError]

      def debit(
        id:             String,
        value:          Domain.Debit,
      )(
        using metadata: Map[String, String] = Map.empty,
      ): Future[
        OkResponse | ResultError] = ???

      def getBalance(
        id:             String,
      )(
        using metadata: Map[String, String] = Map.empty,
      ): Future[Domain.Balance | ResultError] = {
         println(f"Asking the balance: ${id}")
         println(f"metadata: ${metadata}")
         entitySharding
           .entityRefFor(TypeKeys.wallet, id)
           .ask(
             FrameWorkCommands.CmdInst(WalletCommands.CommandsADT.GetBalanceCmd, metadata, _))(
             using timeout)
           .mapTo[Domain.Balance | ResultError]
      }
   }
}
