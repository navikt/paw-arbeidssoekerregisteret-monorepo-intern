package no.nav.paw.kafkakeygenerator

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.http.content.*
import kotlinx.coroutines.runBlocking
import no.nav.paw.kafkakeygenerator.pdl.PdlIdentitesTjeneste
import no.nav.paw.kafkakeygenerator.vo.Identitetsnummer
import no.nav.paw.kafkakeygenerator.vo.CallId
import no.nav.paw.pdl.PdlClient
import org.jetbrains.exposed.sql.Database
import java.util.*

class ApplikasjonsTest : StringSpec({
    val dataSource = initTestDatabase()
    val pdlKlient = PdlClient(
        url = "http://mock",
        tema = "tema",
        HttpClient(MockEngine {
            genererResponse(it)
        })
    ) { "fake token" }
    val app = Applikasjon(
        kafkaKeys = KafkaKeys(Database.connect(dataSource)),
        identitetsTjeneste = PdlIdentitesTjeneste(pdlKlient)
    )
    fun hentEllerOpprett(identitetsnummer: String): Long? = runBlocking {
        app.hentEllerOpprett(CallId(UUID.randomUUID().toString()), Identitetsnummer(identitetsnummer))
    }
    "alle identer for person1 skal gi samme nøkkel" {
        val person1KafkaNøkler = listOf(
            person1_dnummer,
            person1_fødselsnummer,
            person1_aktor_id,
            person1_annen_ident,
            person1_dnummer
        ).map(::hentEllerOpprett)
        person1KafkaNøkler shouldNotContain null
        person1KafkaNøkler.distinct().size shouldBe 1
    }
    "alle identer for person2 skal gi samme nøkkel" {
        val person2KafkaNøkler = listOf(
            person2_fødselsnummer,
            person2_aktor_id,
            person2_fødselsnummer
        ).map(::hentEllerOpprett)
        person2KafkaNøkler shouldNotContain null
        person2KafkaNøkler.distinct().size shouldBe 1
    }
    "person1 og person2 skal ha forskjellig nøkkel" {
        hentEllerOpprett(person1_fødselsnummer) shouldNotBe hentEllerOpprett(person2_aktor_id)
    }
})

