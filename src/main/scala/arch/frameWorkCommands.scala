package arch

object FrameWorkCommands:

   sealed trait Cmd extends ProtoSerializable:
      def replyTo: ActorRef[ResultError]
//      def payload: ProtoSerializable

   case class CmdInst(
     payload: ProtoSerializable,
     params:  Map[String, String],
     replyTo: ActorRef[ProtoSerializable | ResultError]) extends Cmd
