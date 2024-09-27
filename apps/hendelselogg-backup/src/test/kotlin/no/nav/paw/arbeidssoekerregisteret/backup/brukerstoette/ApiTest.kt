package no.nav.paw.arbeidssoekerregisteret.backup.brukerstoette

import arrow.core.right
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.testing.*
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.paw.arbeidssoekerregisteret.backup.api.brukerstoette.models.*
import no.nav.paw.arbeidssoekerregisteret.backup.api.oppslagsapi.models.*
import no.nav.paw.arbeidssoekerregisteret.backup.configureBrukerstoetteRoutes
import no.nav.paw.arbeidssoekerregisteret.backup.configureHTTP
import no.nav.paw.arbeidssoekerregisteret.backup.database.txContext
import no.nav.paw.arbeidssoekerregisteret.backup.database.writeRecord
import no.nav.paw.arbeidssoekerregisteret.backup.initDbContainer
import no.nav.paw.arbeidssoekerregisteret.backup.vo.ApplicationContext
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.arbeidssokerregisteret.intern.v1.HendelseDeserializer
import no.nav.paw.arbeidssokerregisteret.intern.v1.HendelseSerde
import no.nav.paw.arbeidssokerregisteret.intern.v1.Startet
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Bruker
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.BrukerType
import no.nav.paw.config.hoplite.loadNaisOrLocalConfiguration
import no.nav.paw.kafkakeygenerator.client.inMemoryKafkaKeysMock
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

class ApiTest : FreeSpec({
    println("Arg!!")
    "Test av brukerstøtte API" {
        val logger = LoggerFactory.getLogger(ApiTest::class.java)
        logger.info("Starter test")
        initDbContainer()
        logger.info("Db cntainer startet")
        val applicationContext = ApplicationContext(
            consumerVersion = 1,
            logger = logger,
            meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
            shutdownCalled = AtomicBoolean(false),
            azureConfig = loadNaisOrLocalConfiguration("azure.toml"),
        )
        val kafkaKeysClient = inMemoryKafkaKeysMock()
        val oppslagsApi: OppslagApiClient = mockk()
        every { runBlocking { oppslagsApi.perioder(any()) } } returns listOf(
            ArbeidssoekerperiodeResponse(
                periodeId = UUID.randomUUID(),
                startet = MetadataResponse(
                    tidspunkt = Instant.now().truncatedTo(ChronoUnit.MILLIS),
                    utfoertAv = BrukerResponse(
                        type = no.nav.paw.arbeidssoekerregisteret.backup.api.oppslagsapi.models.BrukerType.SYSTEM,
                        id = "system"
                    ),
                    kilde = "system",
                    aarsak = "test"

                ),
                avsluttet = null
            )
        ).right()
        every {
            runBlocking {
                oppslagsApi.opplysninger(
                    any(),
                    any()
                )
            }
        } returns emptyList<OpplysningerOmArbeidssoekerResponse>().right()
        every {
            runBlocking {
                oppslagsApi.profileringer(
                    any(),
                    any()
                )
            }
        } returns emptyList<ProfileringResponse>().right()
        val service = BrukerstoetteService(
            oppslagAPI = oppslagsApi,
            kafkaKeysClient = kafkaKeysClient,
            applicationContext = applicationContext,
            hendelseDeserializer = HendelseDeserializer()
        )
        logger.info("Service opprettet")
        testApplication {
            application {
                configureHTTP(emptyList(), applicationContext.meterRegistry)
            }
            routing {
                configureBrukerstoetteRoutes(service)
            }
            val client = createClient {
                install(ContentNegotiation) {
                    jackson {
                        registerKotlinModule()
                        registerModule(JavaTimeModule())
                    }
                }
            }
            val response = client.post("/api/v1/arbeidssoeker/detaljer") {
                headers {
                    append("Content-Type", "application/json")
                }
                setBody(DetaljerRequest("12345678901"))
            }
            response.status shouldBe HttpStatusCode.NotFound
            val responseBody = response.body<Feil>()
            responseBody.melding shouldBe "Ingen hendelser for bruker"
            responseBody.feilKode shouldBe "ikke funnet"
            val testRecord = ConsumerRecord<Long, Hendelse>(
                "topic",
                3,
                157,
                kafkaKeysClient.getIdAndKeyOrNull("12345678901")!!.key,
                Startet(
                    hendelseId = UUID.randomUUID(),
                    id = kafkaKeysClient.getIdAndKeyOrNull("12345678901")!!.id,
                    identitetsnummer = "12345678901",
                    metadata = no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Metadata(
                        tidspunkt = Instant.now().truncatedTo(ChronoUnit.MILLIS),
                        utfoertAv = Bruker(
                            type = BrukerType.SYSTEM,
                            id = "system"
                        ),
                        kilde = "system",
                        aarsak = "test"
                    )
                )
            )
            transaction {
                txContext(applicationContext)().writeRecord(HendelseSerde().serializer(), testRecord)
            }
            val response2 = client.post("/api/v1/arbeidssoeker/detaljer") {
                headers {
                    append("Content-Type", "application/json")
                }
                setBody(DetaljerRequest("12345678901"))
            }
            response2.status shouldBe HttpStatusCode.OK
            val detaljer: DetaljerResponse = response2.body()
            detaljer.arbeidssoekerId shouldBe testRecord.value().id
            detaljer.kafkaPartition shouldBe 3
            detaljer.recordKey shouldBe testRecord.key()
            detaljer.gjeldeneTilstand shouldBe Tilstand(
                harAktivePeriode = true,
                startet = testRecord.value().metadata.tidspunkt,
                harOpplysningerMottattHendelse = false,
                avsluttet = null,
                apiKall = TilstandApiKall(
                    harPeriode = false,
                    harOpplysning = false,
                    harProfilering = false
                ),
                periodeId = testRecord.value().hendelseId,
                gjeldeneOpplysningsId = null
            )
        }
    }
})
