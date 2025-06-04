package arch

object WalletCommands:

   import Domain.*

   // this generates the trait Service  and the implementation for access the entity
   // @entityService("wallet") // "wallet" is the EntityTypeKey in use
   enum CommandsADT extends ProtoSerializable:
      case CreateWalletCmd
      case CreditCmd(value: Credit)
      case DebitCmd(value: Debit)
      case GetBalanceCmd
      case StopCmd
