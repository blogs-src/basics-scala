package arch

object ServicesWallet:

   trait Service:

      def createWallet(id: String): Future[OkResponse | ResultError]

      def addCredit(id: String, value: domain.Credit): Future[OkResponse | ResultError]

      def addDebit(id: String, value: domain.Debit): Future[OkResponse | ResultError]

      def getBalance(id: String): Future[domain.Balance | ResultError]
