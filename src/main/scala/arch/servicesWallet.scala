package arch

object ServicesWallet:

   // this generates the protobuf and the mapping using chimney
   // the result goes to two string fields in WalletEvents object
   // esto NO ES el EntityService
   // @protoService
   trait Service:
      def createWallet(id: String): Future[OkResponse | ResultError]
      def addCredit(id: String, value: Domain.Credit): Future[OkResponse | ResultError]
      def addDebit(id: String, value: Domain.Debit): Future[OkResponse | ResultError]
      def getBalance(id: String): Future[Domain.Balance | ResultError]
