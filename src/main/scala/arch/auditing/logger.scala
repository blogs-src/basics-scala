package arch
package auditing

//import izumi.logstage.api.routing.StaticLogRouter
import logstage.{LogIO,ConsoleSink, IzLogger, Trace}
import izumi.logstage.sink.file.{FileSink, FileServiceImpl}
import logstage.circe.LogstageCirceRenderingPolicy
import izumi.logstage.sink.file.models.LogFile
import izumi.logstage.sink.file.models.FileSinkConfig
import izumi.logstage.sink.file.models.FileRotation
import izumi.logstage.sink.file.FileServiceImpl.RealFile
import izumi.logstage.api.rendering.RenderingPolicy
import izumi.logstage.sink.file.FileService
import scala.collection.mutable.ListBuffer

import cats.effect.*


object Logger:
  class FileSinkBrokenImpl[F2 <: LogFile](
                                           override val renderingPolicy: RenderingPolicy,
                                           override val fileService: FileService[F2],
                                           override val rotation: FileRotation,
                                           override val config: FileSinkConfig,
                                         ) extends FileSink[F2](renderingPolicy, fileService, rotation, config) {

    val recoveredMessages: ListBuffer[String] = ListBuffer.empty[String]

    override def recoverOnFail(e: String): Unit = {
      recoveredMessages += e
    }
  }

  def getLogger[F[_]](path: String)(using F: Async[F]) = {
    val textSink = ConsoleSink.text(colored = true)
    val fileService: FileService[RealFile] = new FileServiceImpl(path)
    //    val lf: LogFile = fileService.createFileWithName("log2.log")
    val config: FileSinkConfig = FileSinkConfig.soft(500)
    val rotation: FileRotation = FileRotation.DisabledRotation
    val renderingPolicy: RenderingPolicy = LogstageCirceRenderingPolicy(prettyPrint = true)

    val fs: FileSink[RealFile] = new FileSinkBrokenImpl(renderingPolicy, fileService, rotation, config)
    val jsonSink = ConsoleSink(LogstageCirceRenderingPolicy(prettyPrint = true))
    val sinks = List(jsonSink, textSink, fs)
    val logger: IzLogger = IzLogger(Trace, sinks)
    LogIO.fromLogger[F](logger)
  }
