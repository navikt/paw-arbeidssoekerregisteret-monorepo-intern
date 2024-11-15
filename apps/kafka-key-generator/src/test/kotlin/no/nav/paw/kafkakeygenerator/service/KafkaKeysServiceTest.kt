package no.nav.paw.kafkakeygenerator.service

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import kotlinx.coroutines.runBlocking
import no.nav.paw.kafkakeygenerator.api.v2.hentLokaleAlias
import no.nav.paw.kafkakeygenerator.plugin.custom.flywayMigrate
import no.nav.paw.kafkakeygenerator.repository.KafkaKeysRepository
import no.nav.paw.kafkakeygenerator.test.genererResponse
import no.nav.paw.kafkakeygenerator.test.initTestDatabase
import no.nav.paw.kafkakeygenerator.test.person1_aktor_id
import no.nav.paw.kafkakeygenerator.test.person1_annen_ident
import no.nav.paw.kafkakeygenerator.test.person1_dnummer
import no.nav.paw.kafkakeygenerator.test.person1_fødselsnummer
import no.nav.paw.kafkakeygenerator.test.person2_aktor_id
import no.nav.paw.kafkakeygenerator.test.person2_fødselsnummer
import no.nav.paw.kafkakeygenerator.test.person3_fødselsnummer
import no.nav.paw.kafkakeygenerator.vo.ArbeidssoekerId
import no.nav.paw.kafkakeygenerator.vo.CallId
import no.nav.paw.kafkakeygenerator.vo.Either
import no.nav.paw.kafkakeygenerator.vo.Failure
import no.nav.paw.kafkakeygenerator.vo.FailureCode
import no.nav.paw.kafkakeygenerator.vo.Identitetsnummer
import no.nav.paw.kafkakeygenerator.vo.Left
import no.nav.paw.kafkakeygenerator.vo.Right
import no.nav.paw.pdl.PdlClient
import org.jetbrains.exposed.sql.Database
import org.junit.jupiter.api.fail
import java.util.*

class KafkaKeysServiceTest : StringSpec({
    val dataSource = initTestDatabase()
    dataSource.flywayMigrate()
    val pdlKlient = PdlClient(
        url = "http://mock",
        tema = "tema",
        HttpClient(MockEngine {
            genererResponse(it)
        })
    ) { "fake token" }
    val kafkaKeysService = KafkaKeysService(
        kafkaKeysRepository = KafkaKeysRepository(Database.connect(dataSource)),
        pdlService = PdlService(pdlKlient)
    )

    fun hentEllerOpprett(identitetsnummer: String): Either<Failure, ArbeidssoekerId> = runBlocking {
        kafkaKeysService.hentEllerOpprett(
            callId = CallId(UUID.randomUUID().toString()),
            identitet = Identitetsnummer(identitetsnummer)
        )
    }
    "alle identer for person1 skal gi samme nøkkel" {
        val person1KafkaNøkler = listOf(
            person1_dnummer,
            person1_fødselsnummer,
            person1_aktor_id,
            person1_annen_ident,
            person1_dnummer
        ).map(::hentEllerOpprett)
        person1KafkaNøkler.filterIsInstance<Left<Failure>>().size shouldBe 0
        person1KafkaNøkler.filterIsInstance<Right<ArbeidssoekerId>>()
            .map { it.right }
            .distinct().size shouldBe 1
        kafkaKeysService.hentLokaleAlias(2, listOf(person1_dnummer)).onLeft { fail { "Uventet feil: $it" } }
            .onRight { res ->
                res.flatMap { it.koblinger }.map { it.identitetsnummer }.shouldContainExactlyInAnyOrder(
                    person1_dnummer, person1_fødselsnummer, person1_aktor_id, person1_annen_ident
                )
            }
        val lokaleAlias = kafkaKeysService.hentLokaleAlias(2, Identitetsnummer(person1_dnummer))
        hentEllerOpprett(person3_fødselsnummer).shouldBeInstanceOf<Right<ArbeidssoekerId>>()
        lokaleAlias.onLeft { fail { "Uventet feil: $it" } }.onRight { alias ->
            alias.identitetsnummer shouldBe person1_dnummer
            alias.koblinger.size shouldBe 4
            alias.koblinger.any { it.identitetsnummer == person1_fødselsnummer } shouldBe true
            alias.koblinger.any { it.identitetsnummer == person1_dnummer } shouldBe true
            alias.koblinger.any { it.identitetsnummer == person1_aktor_id } shouldBe true
            alias.koblinger.any { it.identitetsnummer == person1_annen_ident } shouldBe true
        }
    }
    "alle identer for person2 skal gi samme nøkkel" {
        val person2KafkaNøkler = listOf(
            person2_fødselsnummer,
            person2_aktor_id,
            person2_fødselsnummer
        ).map(::hentEllerOpprett)
        person2KafkaNøkler.filterIsInstance<Left<Failure>>().size shouldBe 0
        person2KafkaNøkler.filterIsInstance<Right<ArbeidssoekerId>>()
            .map { it.right }
            .distinct().size shouldBe 1
    }
    "person1 og person2 skal ha forskjellig nøkkel" {
        hentEllerOpprett(person1_fødselsnummer) shouldNotBe hentEllerOpprett(person2_aktor_id)
    }
    "ingen treff i PDL skal feile med ${FailureCode.PDL_NOT_FOUND}" {
        val person3KafkaNøkler = listOf(
            "13579864201",
            "13579864202"
        ).map(::hentEllerOpprett)
        person3KafkaNøkler.filterIsInstance<Left<Failure>>().size shouldBe 2
        person3KafkaNøkler.filterIsInstance<Right<Long>>().size shouldBe 0
        person3KafkaNøkler.forEach {
            it.shouldBeInstanceOf<Left<Failure>>()
            it.left.code shouldBe FailureCode.PDL_NOT_FOUND
        }
    }
})
