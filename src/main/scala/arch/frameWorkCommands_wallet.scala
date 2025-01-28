package arch

object FrameWorkCommands:

   sealed trait Cmd extends ProtoSerializable:

      def replyTo: ActorRef[ResultError]

   case class CmdInst(
     payload: ProtoSerializable,
     params:  List[String],
     replyTo: ActorRef[ProtoSerializable | ResultError]) extends Cmd
