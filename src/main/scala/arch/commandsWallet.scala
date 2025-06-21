package arch

object WalletCommands:

   import Domain.*

   // this generates the trait Service  and the implementation for access the entity
   // @entityService("wallet") // "wallet" is the EntityTypeKey in use
   // modules/grpc-api/src/main/protobuf/commands.proto
   enum CommandsADT extends ProtoSerializable:
      case CreateWalletCmd
      case CreditCmd(value: Credit)
      case DebitCmd(value: Debit)
      case GetBalanceCmd
      case StopCmd

   val responseTypes = List(
     (CommandsADT.CreateWalletCmd, OkResponse),
     (CommandsADT.CreditCmd, OkResponse),
     (CommandsADT.DebitCmd, OkResponse),
     (CommandsADT.DebitCmd, Balance),
     (CommandsADT.DebitCmd, OkResponse),
   )

