package arch

import akka.cluster.sharding.typed.scaladsl.EntityContext
import arch.ClusterWallet.WalletSharding
import arch.FrameWorkCommands.*
import arch.ServicesWallet.Service
import arch.ServicesWalletImpl.WalletServiceImpl
import com.typesafe.config.Config
import arch.TypeKeys

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

      case class CreateWallet(id: String) extends Command

      case class GetBalance(id: String) extends Command

      case class AddCredit(id: String, value: Int) extends Command
      // object StartProjections extends Command

      def interactive(config: Config, ws: Service): Behavior[Command] = Behaviors.setup[Command]:
           (ctx: ActorContext[Command]) =>
              given ec: ExecutionContextExecutor = ctx.system.executionContext
              val log = Logging(ctx.system.toClassic, classOf[Command])

              Behaviors.receiveMessage[Command] {
                case Start            =>
                  println("Handler started")
                  Behaviors.same
                case GetBalance(id)   =>
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
                  val res = ws.addCredit(id, Domain.Credit(v))
                  res.onComplete {
                    case Success(r) => println(r)
                    case Failure(t) => t.printStackTrace()
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

              val w: ServicesWallet.Service = new WalletServiceImpl(walletSharding)
              ctx.delegate(interactive(config, w), Root.Start)

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
