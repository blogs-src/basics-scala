package arch
package auditing

import cats.data.EitherT
import cats.effect.*

import _root_.io.opentelemetry.sdk.OpenTelemetrySdk
import _root_.io.opentelemetry.sdk.trace.SdkTracerProvider
import _root_.io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator
import _root_.io.opentelemetry.context.propagation.ContextPropagators

import io.opentelemetry.api.common.Attributes
import io.opentelemetry.sdk.trace.`export`.BatchSpanProcessor
import org.typelevel.otel4s.oteljava.OtelJava
import cats.effect.kernel.Resource

import org.typelevel.otel4s.trace.Tracer

object Tracer:

   def makeOtel(appName: String): Resource[Result, Tracer[Result]] =
//      val jaegerEndpoint = "http://localhost:4317"
//      val jaegerEndpoint = "http://jaeger1.dev.me:4317"
      val jaegerEndpoint = "http://jaeger1.dev.me:4317"
      import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter
      import java.util.concurrent.TimeUnit

      import org.typelevel.otel4s.oteljava.context.IOLocalContextStorage
      import org.typelevel.otel4s.oteljava.context.Context
      import org.typelevel.otel4s.context.LocalProvider

      given LocalProvider[Result, Context] = IOLocalContextStorage.localProvider[Result]

      val serviceNameResource = io.opentelemetry.sdk.resources.Resource.create(
        Attributes.of(io.opentelemetry.semconv.ServiceAttributes.SERVICE_NAME,
                      appName))
      val jaegerOtlpExporter = OtlpGrpcSpanExporter.builder.setEndpoint(jaegerEndpoint).setTimeout(30, TimeUnit.SECONDS).build
      val sdkTracerProvider = SdkTracerProvider.builder()
        .addSpanProcessor(BatchSpanProcessor.builder(jaegerOtlpExporter).build)
        .setResource(io.opentelemetry.sdk.resources.Resource.getDefault.merge(serviceNameResource))
        .build()
      val sdk: OpenTelemetrySdk = OpenTelemetrySdk.builder()
        .setTracerProvider(sdkTracerProvider)
        .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
        .build()
      val z: Result[OpenTelemetrySdk] = EitherT.right(IO.pure(sdk))
      OtelJava.resource(z).evalMap(_.tracerProvider.get("Example"))
      //    OtelJava.autoConfigured[IO]().evalMap(_.tracerProvider.get("Example"))
