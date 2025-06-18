package arch

import akka.cluster.sharding.typed.scaladsl.EntityContext
import arch.ClusterWallet.WalletSharding
import arch.FrameWorkCommands.*
import arch.WalletServices.Service
import arch.WalletServicesImpl.WalletServiceImpl
import com.typesafe.config.Config
import arch.TypeKeys
import cats.data.EitherT
import org.typelevel.otel4s.oteljava.context.LocalContextProvider
import cats.effect.kernel.Resource
import cats.~>
import cats.arrow.FunctionK
import cats.syntax.all.*

import cats.data.EitherT
import cats.effect.*
import fs2.grpc.syntax.all.*
import io.grpc.*
import cats.mtl.*


object WalletEventSourcing:

   import akka.persistence.typed.PersistenceId
   import akka.management.scaladsl.AkkaManagement
   import akka.actor.typed.ActorRef
   import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps

   import arch.EntityWallet.Entity as WalletEntity
   import akka.cluster.typed.*
   import akka.actor.ActorSystem as UntypedActorSystem
   import akka.cluster.ClusterEvent.*

   import com.typesafe.config.Config

   import akka.cluster.sharding.typed.scaladsl.Entity

   import akka.event.Logging

   object Root:
      trait Command extends CborSerializable
      object Start  extends Command

      object StartGrpcServer extends Command

      object StopGrpcServer extends Command

      var grpcServerControl: Option[cats.effect.Deferred[cats.effect.IO, Boolean]] = None

      case class CreateWallet(id: String) extends Command

      case class GetBalance(id: String) extends Command

      case class AddCredit(id: String, value: Int) extends Command
      // object StartProjections extends Command

//      val h = org.example.Hello()

      def interactive(
                       config: Config,
                       ws: Service,
                       grpcApi: GrpcServerResource,
                     ): Behavior[Command] = Behaviors.setup[Command]:
           (ctx: ActorContext[Command]) =>
              given ec: ExecutionContextExecutor = ctx.system.executionContext
              val log = Logging(ctx.system.toClassic, classOf[Command])

              Behaviors.receiveMessage[Command] {
                case Start            =>
                  println("Handler started")
                  Behaviors.same
                case GetBalance(id)   =>

//                  val h = org.example.Hello()
//                  h.run()

//                  val c = Class.forName("io.grpc.netty.shaded.io.grpc.netty.NettyServerHandler")
//                  println(c.getCanonicalName)
//                  for (field <- c.getDeclaredFields) {
//                    println(field.getName)
//                  }

                  val res = ws.getBalance(id)
                  res.onComplete {
                    case Success(r) => println(s"The balance is: $r")
                    case Failure(t) => t.printStackTrace()
                  }
                  Behaviors.same
                case CreateWallet(id) =>
                  val res = ws.createWallet(id)
                  res.onComplete {
                    case Success(r) => println(s"Wallet created: $r")
                    case Failure(t) => t.printStackTrace()
                  }
                  Behaviors.same
                case AddCredit(id, v) =>
                  val res = ws.credit(id, Domain.Credit(v))
                  res.onComplete {
                    case Success(r) => println(r)
                    case Failure(t) => t.printStackTrace()
                  }
                  Behaviors.same

                case StopGrpcServer =>
                  import cats.effect.unsafe.implicits.global
                  println("Stoping servers")
                  grpcServerControl.foreach(
                    ser => {
                      Future {
                        val r = Try { ser.complete(true).unsafeRunSync() }
                        println(s"Grpc Server Control completed: $r")
                      }
                    }
                  ) // shutdown the server
                  grpcServerControl = None
                  Behaviors.same

                case StartGrpcServer =>
                  println("Starting Grpc Server")
                  log.info("Starting Grpc Server in logs")
                  ctx.log.info("Starting Grpc Server in ctx")
                  import cats.effect.unsafe.implicits.global

                  import org.typelevel.log4cats.slf4j.Slf4jLogger
                  import org.typelevel.log4cats.Logger
                  import com.google.rpc.Code
                  import cats.effect.*
                  import cats.implicits.*

                  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

                  val grpcIO = cats.effect.Deferred[cats.effect.IO, Boolean].flatMap {
                    shutdown =>
                      grpcServerControl = Some(shutdown)

                      import akka.grpc.GrpcServiceException
                      import com.wallet.demo.clustering.rpc.admin.BadRequestError

                      given generator: ExceptionGenerator[GrpcServiceException] with
                        def generateException(msg: String): Throwable =
                          val e = ErrorsBuilder.badRequestError(msg)
                          val error = BadRequestError(e.code, e.title, e.message)
                          GrpcServiceException(Code.INVALID_ARGUMENT, msg, Seq(error))

//                      val wServiceIO = WalletServiceIOImpl2[Result](ws)

                      val rpcResource: Resource[IO, (io.grpc.Server, Option[Boolean])] =
                        for {
                          serverDefinition <- grpcApi.createService[GrpcServiceException](WalletServiceIOImpl[Result](ws))
                          server <- grpcApi.createIO[IO](serverDefinition._1)
                        } yield (server, None)

                      val runingRpcIO = rpcResource.evalMap(
                          res => {
                            (IO.pure(res._1.start()), IO.pure{()}).mapN(
                              (a, c) => ()
                            )
                          }
                        ).useForever.handleErrorWith{error =>
                          println(s"===> ${error.getMessage}")
                          IO.raiseError(error)
                         }

                      IO.race(shutdown.get, runingRpcIO)
                  }

                  Future {
                    grpcIO.evalOn(ctx.system.executionContext).unsafeRunSync()
                  }
                  Behaviors.same
              }

      def apply(config: Config): Behavior[Command] = Behaviors.setup[Command]:
           (ctx: ActorContext[Command]) =>
              ctx.log.info("Starting Wallet Operations")
              given typedActorSystem: ActorSystem[Nothing] = ctx.system
              given UntypedActorSystem = typedActorSystem.toClassic
              given ExecutionContextExecutor = ctx.system.executionContext

              infrastructure.Serializers.register(typedActorSystem)

              val cluster = Cluster(typedActorSystem)
              ctx.log.info("Started [" + ctx.system + "], cluster.selfAddress = " + cluster.selfMember.address + ")")

              if config.getBoolean("application.local.config.first") then
                 cluster.manager ! Join(cluster.selfMember.address)
                 val management = AkkaManagement(typedActorSystem).start()
                 management.onComplete:
                      case Failure(exception) => println(s"Akka Management failed to start: $exception")
                      case Success(value)     => println(s"Akka Management started at: $value")

              // val subscriber = ctx.spawnAnonymous(ClusterStateChanges())
              // cluster.subscriptions ! Subscribe(subscriber, classOf[MemberEvent])

              val walletSharding = WalletSharding()

              def mkEntity(entityContext: EntityContext[CmdInst]): Behavior[CmdInst] = WalletEntity(
                PersistenceId(
                  TypeKeys.wallet.name,
                  entityContext.entityId))

              walletSharding.init(
                Entity(TypeKeys.wallet)(createBehavior =
                  (entityContext: EntityContext[CmdInst]) =>
                    di.mkEntity(entityContext)))

              val w: WalletServices.Service = new WalletServiceImpl(walletSharding, demo.timeout)
              val grpcApi: GrpcServerResource = GrpcServerResource()
//                , summon[ExecutionContextExecutor]
              ctx.delegate(interactive(config, w, grpcApi), Root.Start)

object WalletOperations:

   import WalletEventSourcing.*
   import com.typesafe.config.ConfigFactory

   val confFile = "application-clustering.conf"
   val actorSystemName = "system"

   var sys1: Option[ActorSystem[Root.Command]] = None
   var sys2: Option[ActorSystem[Root.Command]] = None
   var sys3: Option[ActorSystem[Root.Command]] = None

   def g = sys1.foreach:
        sys =>
           sys ! Root.GetBalance("a")

   def grpc = sys1.foreach:
      sys =>
        sys ! Root.StartGrpcServer

   def getBalance(id: String) = sys1.foreach:
        sys =>
           sys ! Root.GetBalance(id)

   def createWallet(id: String) = sys1.foreach:
        sys =>
           sys ! Root.CreateWallet(id)

   def addCredit(id: String, a: Int) = sys1.foreach:
        sys =>
           sys ! Root.AddCredit(id, a)

   def start1 =
      // akka.loglevel = "DEBUG"
      val conf: Config = ConfigFactory.parseString(
        """
             application.local.config.first = true
             akka.remote.artery.canonical.port = 2551
          """)
        .withFallback(
          ConfigFactory.load(confFile))
      val sys: ActorSystem[Root.Command] = ActorSystem(Root(conf), actorSystemName, conf)
      sys ! Root.StartGrpcServer
      sys1 = Some(sys)

   def start2 =
      val conf = ConfigFactory.parseString(
        s"""
            application.local.config.first = false
            akka.remote.artery.canonical.port = 2552
            akka.cluster.seed-nodes = [
                "akka://${actorSystemName}@0.0.0.0:2551"
            ]
            """)
        .withFallback(
          ConfigFactory.load(confFile))

      val sys: ActorSystem[Root.Command] = ActorSystem(Root(conf), actorSystemName, conf)
      sys2 = Some(sys)

   def start3 =
      // "akka://${actorSystemName}@0.0.0.0:2552"
      val conf = ConfigFactory.parseString(
        s"""
            application.local.config.first = false
            akka.remote.artery.canonical.port = 2553
            akka.cluster.seed-nodes = [
                "akka://${actorSystemName}@0.0.0.0:2551"
            ]
            """)
        .withFallback(
          ConfigFactory.load(
            confFile))
      val sys: ActorSystem[Root.Command] = ActorSystem(Root(conf), actorSystemName, conf)
      sys3 = Some(sys)

   def s =
      sys1.foreach(
        aSys => {
          aSys ! Root.StopGrpcServer
          given ec: ExecutionContextExecutor = aSys.executionContext
          aSys.terminate()
          aSys.whenTerminated.onComplete(
            _ =>
              println("Actor system 1 was stopped"))
        })
      sys2.foreach(
        aSys => {
          aSys.terminate()
          given ec: ExecutionContextExecutor = aSys.executionContext
          aSys.whenTerminated.onComplete(
            _ =>
              println("Actor system 2 was stopped"))
        })
      sys3.foreach(
        aSys => {
          aSys.terminate()
          given ec: ExecutionContextExecutor = aSys.executionContext
          aSys.whenTerminated.onComplete(
            _ =>
              println("Actor system 3 was stopped"))
        })
      sys1 = None
      sys2 = None
      sys3 = None

   def init =
      start1
      Thread.sleep(3000)
      start2
      start3
