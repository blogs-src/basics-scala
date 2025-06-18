package arch

object WalletServices:

   import Domain.*

   // this generates the protobuf and the mapping using chimney
   // the result goes to two string fields in WalletEvents object
   // esto NO ES el EntityService
   // @protoService
   // modules/grpc-api/src/main/protobuf/service-clustering.proto
   trait Service:
      def createWallet(id: String)(using metadata: Map[String, String]=Map.empty): Future[OkResponse | ResultError]
      def credit(id: String, value: Credit)(using metadata: Map[String, String]=Map.empty): Future[OkResponse | ResultError]
      def debit(id: String, value: Debit)(using metadata: Map[String, String]=Map.empty): Future[OkResponse | ResultError]
      def getBalance(id: String)(using metadata: Map[String, String]=Map.empty): Future[Balance | ResultError]
