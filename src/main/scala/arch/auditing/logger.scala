package arch
package auditing


import logstage.{IzLogger, LogIO}
import izumi.logstage.api.routing.StaticLogRouter
import logstage.{ConsoleSink, IzLogger, Trace}
import izumi.logstage.sink.file.{FileSink, FileServiceImpl}
import logstage.circe.LogstageCirceRenderingPolicy
import izumi.logstage.sink.file.models.LogFile
import izumi.logstage.sink.file.models.FileSinkConfig
import izumi.logstage.sink.file.models.FileRotation
import izumi.logstage.sink.file.FileServiceImpl.RealFile
import izumi.logstage.api.rendering.RenderingPolicy
import izumi.logstage.sink.file.FileService
import scala.collection.mutable.ListBuffer
import scala.util.{Random, Try}

import cats.mtl.*
import cats.effect.*
import cats.implicits.*
import cats.*


object logger:
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

  def getLogger[F[_]]()(using F: Async[F]) = {
    val textSink = ConsoleSink.text(colored = true)
    val fileService: FileService[RealFile] = new FileServiceImpl("logs")
    //    val lf: LogFile = fileService.createFileWithName("log2.log")
    val fsc: FileSinkConfig = FileSinkConfig.soft(500)
    val fr: FileRotation = FileRotation.DisabledRotation
    val renderingPolicy: RenderingPolicy = LogstageCirceRenderingPolicy(prettyPrint = true)

    val fs/*: FileSink*/ = new FileSinkBrokenImpl(renderingPolicy, fileService, fr, fsc)
    val jsonSink = ConsoleSink(LogstageCirceRenderingPolicy(prettyPrint = true))
    val sinks = List(jsonSink, textSink, fs)
    val logger2: IzLogger = IzLogger(Trace, sinks)
    LogIO.fromLogger[F](logger2)
  }
