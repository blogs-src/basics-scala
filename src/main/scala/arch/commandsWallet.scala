package arch

object WalletCommands:

   import Domain.*

   enum CommandsADT extends ProtoSerializable:
      case CreateWalletCmd
      case CreditCmd(value: Credit)
      case DebitCmd(value: Debit)
      case GetBalanceCmd
      case StopCmd

