package no.nav.paw.arbeidssokerregisteret

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.nimbusds.jwt.SignedJWT
import io.kotest.assertions.fail
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.headers
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.append
import io.ktor.serialization.jackson.jackson
import io.mockk.coEvery
import no.nav.paw.arbeidssoekerregisteret.api.startstopp.models.ApiV2ArbeidssokerPeriodePutRequest
import no.nav.paw.arbeidssoekerregisteret.api.startstopp.models.Feilretting
import no.nav.paw.arbeidssokerregisteret.domain.Identitetsnummer
import no.nav.paw.arbeidssokerregisteret.domain.NavAnsatt
import no.nav.paw.arbeidssokerregisteret.intern.v1.HarOpplysninger
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.BrukerType
import no.nav.paw.arbeidssokerregisteret.services.AutorisasjonService
import no.nav.paw.arbeidssokerregisteret.services.PersonInfoService
import no.nav.paw.arbeidssokerregisteret.testdata.mustBe
import no.nav.paw.pdl.graphql.generated.enums.Oppholdstillatelse
import no.nav.paw.pdl.graphql.generated.hentperson.Bostedsadresse
import no.nav.paw.pdl.graphql.generated.hentperson.Folkeregistermetadata
import no.nav.paw.pdl.graphql.generated.hentperson.Folkeregisterpersonstatus
import no.nav.paw.pdl.graphql.generated.hentperson.InnflyttingTilNorge
import no.nav.paw.pdl.graphql.generated.hentperson.Matrikkeladresse
import no.nav.paw.pdl.graphql.generated.hentperson.Metadata
import no.nav.paw.pdl.graphql.generated.hentperson.Opphold
import no.nav.paw.pdl.graphql.generated.hentperson.Person
import no.nav.paw.pdl.graphql.generated.hentperson.Statsborgerskap
import no.nav.paw.pdl.graphql.generated.hentperson.UtenlandskAdresse
import no.nav.paw.pdl.graphql.generated.hentperson.UtflyttingFraNorge
import no.nav.paw.pdl.graphql.generated.hentperson.Vegadresse
import no.nav.security.mock.oauth2.MockOAuth2Server
import org.apache.kafka.clients.producer.ProducerRecord


fun HttpClientConfig<out io.ktor.client.engine.HttpClientEngineConfig>.defaultConfig() {
    install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
        jackson {
            registerKotlinModule()
            registerModule(JavaTimeModule())
        }
    }
}


fun MockOAuth2Server.personToken(id: String, acr: String = "idporten-loa-high"): Pair<Map<String, Any>, SignedJWT> =
    mapOf(
    "acr" to acr,
    "pid" to id
).let { it.plus("issuer" to "tokenx") to issueToken(claims = it) }

fun MockOAuth2Server.ansattToken(navAnsatt: NavAnsatt): Pair<Map<String, Any>, SignedJWT> =
    mapOf(
        "oid" to navAnsatt.azureId,
        "NAVident" to navAnsatt.ident
    ).let { it.plus("issuer" to "azure") to issueToken(claims = it) }


fun verify(
    actual: ProducerRecord<Long, Hendelse>?,
    expected: ProducerRecord<Long, out Hendelse>,
    brukerAuth: Map<String, Any>?
) {
    if (actual == null) {
        fail("Forventet at melding skulle bli produsert, men ingen melding ble funnet")
    }
    actual.key() shouldBe expected.key()
    val actualValue = actual.value()
    val expectedValue = expected.value()
    actualValue::class shouldBe expectedValue::class
    actualValue.id shouldBe expectedValue.id
    actualValue.identitetsnummer shouldBe expectedValue.identitetsnummer
    actualValue.metadata.utfoertAv.id shouldBe expectedValue.metadata.utfoertAv.id
    if (brukerAuth == null) {
        actualValue.metadata.utfoertAv.sikkerhetsnivaa shouldBe null
    } else {
        if (brukerAuth["issuer"] == "azure") {
            actualValue.metadata.utfoertAv.type shouldBe BrukerType.VEILEDER
        } else {
            actualValue.metadata.utfoertAv.type shouldBe BrukerType.SLUTTBRUKER
        }
        actualValue.metadata.utfoertAv.sikkerhetsnivaa shouldBe "${brukerAuth["issuer"]}:${brukerAuth["acr"] ?: "undefined"}"
    }
    actualValue.metadata.utfoertAv.type shouldBe expectedValue.metadata.utfoertAv.type
    actualValue.metadata.tidspunktFraKilde?.avviksType shouldBe expectedValue.metadata.tidspunktFraKilde?.avviksType
    actualValue.metadata.tidspunktFraKilde?.tidspunkt mustBe expectedValue.metadata.tidspunktFraKilde?.tidspunkt
    if (expectedValue is HarOpplysninger) {
        actualValue.shouldBeInstanceOf<HarOpplysninger>()
        actualValue.opplysninger shouldContainExactlyInAnyOrder expectedValue.opplysninger
    }
}

const val bosatt = "bosattEtterFolkeregisterloven"
const val ikkeBosatt = "ikkeBosatt"
const val doed = "doedIFolkeregisteret"
const val forsvunnet = "forsvunnet"
const val opphoert = "opphoert"
const val dNummer = "dNummer"

val emptyMetadat = Metadata(emptyList())

fun String?.innflytting(): List<InnflyttingTilNorge> = list().map {
    InnflyttingTilNorge(
        folkeregistermetadata = Folkeregistermetadata(
            gyldighetstidspunkt = it,
            ajourholdstidspunkt = it
        )
    )
}

fun String?.utflytting(): List<UtflyttingFraNorge> = list().map {
    UtflyttingFraNorge(
        utflyttingsdato = it,
        folkeregistermetadata = Folkeregistermetadata(
            gyldighetstidspunkt = it,
            ajourholdstidspunkt = it
        )
    )
}

fun bostedsadresse(
    vegadresse: Vegadresse? = null,
    matrikkeladresse: Matrikkeladresse? = null,
    utenlandskAdresse: UtenlandskAdresse? = null
): List<Bostedsadresse> = listOfNotNull(
    if (vegadresse == null && matrikkeladresse == null && utenlandskAdresse == null) null
    else {
        Bostedsadresse(
            vegadresse = vegadresse,
            matrikkeladresse = matrikkeladresse,
            utenlandskAdresse = utenlandskAdresse,
        )
    }
)

fun String.folkeregisterpersonstatus(): List<Folkeregisterpersonstatus> =
    list().map { Folkeregisterpersonstatus(it, emptyMetadat) }

fun folkeregisterpersonstatus(status: String, vararg statuser: String): List<Folkeregisterpersonstatus> =
    (status.list() + statuser).map { Folkeregisterpersonstatus(it, emptyMetadat) }

fun statsborgerskap(cc: String, vararg ccs: String): List<Statsborgerskap> =
    (cc.list() + ccs).map { Statsborgerskap(it, emptyMetadat) }

fun String.statsborgerskap(): List<Statsborgerskap> = list().map { Statsborgerskap(it, emptyMetadat) }

fun <A : Any?> A.list(): List<A> = listOfNotNull(this)

fun Pair<String, String?>?.opphold(): List<Opphold> = list()
    .filterNotNull()
    .map {
        Opphold(
            oppholdFra = it.first,
            oppholdTil = it.second,
            type = if (it.second == null) Oppholdstillatelse.PERMANENT else Oppholdstillatelse.MIDLERTIDIG,
            metadata = emptyMetadat
        )
    }

fun AutorisasjonService.setHarTilgangTilBruker(ansatt: NavAnsatt, bruker: String, tilgang: Boolean) {
    coEvery { verifiserVeilederTilgangTilBruker(ansatt, Identitetsnummer(bruker)) } returns tilgang
}

fun PersonInfoService.setPersonInfo(identitetsnummer: String, person: Person?) {
    coEvery {
        hentPersonInfo(any(), identitetsnummer)
    } returns person
}

suspend fun HttpClient.startStoppPeriode(
    periodeTilstand: ApiV2ArbeidssokerPeriodePutRequest.PeriodeTilstand,
    identitetsnummer: String,
    token: SignedJWT?,
    godkjent: Boolean = false,
    feilretting: Feilretting? = null
): HttpResponse =
    put("/api/v2/arbeidssoker/periode") {
        token?.also {
            bearerAuth(token.serialize())
        }
        headers {
            append(HttpHeaders.ContentType, ContentType.Application.Json)
        }
        setBody(
            ApiV2ArbeidssokerPeriodePutRequest(
                identitetsnummer = identitetsnummer,
                periodeTilstand = periodeTilstand,
                registreringForhaandsGodkjentAvAnsatt = godkjent,
                feilretting = feilretting
            )
        )
    }

