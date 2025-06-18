package arch

object WalletServices:

   import Domain.*

   // this generates the protobuf and the mapping using chimney
   // the result goes to two string fields in WalletEvents object
   // esto NO ES el EntityService
   // @protoService
   // modules/grpc-api/src/main/protobuf/service-clustering.proto
   trait Service:
      def createWallet(id: String): Future[OkResponse | ResultError]
      def credit(id: String, value: Credit): Future[OkResponse | ResultError]
      def debit(id: String, value: Debit): Future[OkResponse | ResultError]
      def getBalance(id: String): Future[Balance | ResultError]
