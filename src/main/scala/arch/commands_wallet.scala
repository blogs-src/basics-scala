package arch

object WalletCommands:

   import domain.*

   enum CommandsADT extends ProtoSerializable:
      case CreateWalletCmd

      case CreditCmd(value: Credit)

      case DebitCmd(value: Debit)

      case StopCmd

   enum CommandsReadADT extends ProtoSerializable:
      case GetBalanceCmd
