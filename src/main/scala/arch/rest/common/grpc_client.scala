package arch
package rest

import io.grpc.ManagedChannel
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder
import cats.*
import cats.effect.*
import fs2.grpc.syntax.all.*

class GrpcClientToWritesideResource(port: Int) {

   def resource: Resource[Result, ManagedChannel] = NettyChannelBuilder
     .forAddress("0.0.0.0", port)
     .usePlaintext()
     .resource[Result]
}
