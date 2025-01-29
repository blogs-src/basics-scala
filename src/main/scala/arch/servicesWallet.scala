package arch

object ServicesWallet:

   trait Service:
      def createWallet(id: String): Future[OkResponse | ResultError]
      def addCredit(id: String, value: Domain.Credit): Future[OkResponse | ResultError]
      def addDebit(id: String, value: Domain.Debit): Future[OkResponse | ResultError]
      def getBalance(id: String): Future[Domain.Balance | ResultError]
