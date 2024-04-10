package no.nav.paw.arbeidssokerregisteret.app.helse

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.metrics.micrometer.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.kafka.KafkaStreamsMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.prometheus.PrometheusMeterRegistry

val avroSchemaInfo = getModuleInfo("avro-schema")
private val objectMapper = jacksonObjectMapper()

fun initKtor(
    kafkaStreamsMetrics: KafkaStreamsMetrics,
    prometheusRegistry: PrometheusMeterRegistry,
    helse: Helse
): ApplicationEngine {
    return embeddedServer(Netty, port = 8080) {
        install(MicrometerMetrics) {
            registry = prometheusRegistry
            meterBinders = listOf(
                JvmMemoryMetrics(),
                JvmGcMetrics(),
                ProcessorMetrics(),
                kafkaStreamsMetrics
            )
        }
        routing {
            get("/isReady") {
                val status = helse.ready()
                call.respond(status.code, status.message)
            }
            get("/isAlive") {
                val alive = helse.alive()
                call.respond(alive.code, alive.message)
            }
            get("/metrics") {
                call.respond(prometheusRegistry.scrape())
            }
            get("/moduleInfo") {
                call.respondText(
                    text = objectMapper.writeValueAsString(avroSchemaInfo),
                    contentType = ContentType.Application.Json
                )
            }
        }
    }
}
