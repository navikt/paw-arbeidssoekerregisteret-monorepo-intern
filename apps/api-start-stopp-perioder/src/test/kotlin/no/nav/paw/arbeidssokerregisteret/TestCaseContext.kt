package no.nav.paw.arbeidssokerregisteret

import io.mockk.mockk
import no.nav.paw.arbeidssokerregisteret.application.Regler
import no.nav.paw.arbeidssokerregisteret.application.RequestValidator
import no.nav.paw.arbeidssokerregisteret.application.StartStoppRequestHandler
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.arbeidssokerregisteret.services.AutorisasjonService
import no.nav.paw.arbeidssokerregisteret.services.PersonInfoService
import no.nav.paw.kafkakeygenerator.client.KafkaKeysClient
import no.nav.paw.kafkakeygenerator.client.inMemoryKafkaKeysMock

fun String.readable(): String =
    map { letter -> if (letter.isUpperCase()) " ${letter.lowercase()}" else "$letter" }
        .joinToString("")
        .replace("oe", "ø")
        .replace("aa", "å")

data class TestCaseContext(
    val autorisasjonService: AutorisasjonService,
    val personInfoService: PersonInfoService,
    val producer: ProducerMock<Long, Hendelse>,
    val kafkaKeys: KafkaKeysClient,
    val startStoppRequestHandler: StartStoppRequestHandler
)

fun initTestCaseContext(regler: Regler): TestCaseContext {
    val autorisasjonService = mockk<AutorisasjonService>()
    val personInfoService = mockk<PersonInfoService>()
    val producer: ProducerMock<Long, Hendelse> = ProducerMock()
    val kafkaKeys = inMemoryKafkaKeysMock()
    val startStoppRequestHandler = StartStoppRequestHandler(
        hendelseTopic = "any",
        requestValidator = RequestValidator(
            autorisasjonService = autorisasjonService,
            personInfoService = personInfoService,
            regler = regler
        ),
        producer = producer,
        kafkaKeysClient = kafkaKeys
    )
    return TestCaseContext(
        autorisasjonService = autorisasjonService,
        personInfoService = personInfoService,
        producer = producer,
        kafkaKeys = kafkaKeys,
        startStoppRequestHandler = startStoppRequestHandler
    )
}