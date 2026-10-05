package no.nav.paw.identitet.internehendelser

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import no.nav.paw.identitet.internehendelser.vo.Identitet
import no.nav.paw.identitet.internehendelser.vo.IdentitetType
import java.time.Instant
import java.util.UUID

/**
 * Verifiserer at identitetshendelser skrevet med Jackson 2 kan leses av gjeldende
 * IdentitetHendelseSerde. JSON-filene er generert med Jackson 2, se [lagretJson].
 */
class IdentitetHendelseSerdeKompatibilitetTest : FreeSpec({
    val serde = IdentitetHendelseSerde()

    eksempler.forEach { (navn, forventet) ->
        "$navn skrevet med Jackson 2 kan leses" {
            val json = lagretJson("identitet-interne-hendelser/$navn") {
                serde.serializer().serialize("topic", forventet)!!
            }
            serde.deserializer().deserialize("topic", json) shouldBe forventet
        }
    }
})

private val TIDSPUNKT = Instant.parse("2025-03-14T09:26:53.589Z")

private val identiteter = listOf(
    Identitet(identitet = "12345678901", type = IdentitetType.FOLKEREGISTERIDENT, gjeldende = true),
    Identitet(identitet = "2649500819544", type = IdentitetType.AKTORID, gjeldende = true),
    Identitet(identitet = "1001", type = IdentitetType.ARBEIDSSOEKERID, gjeldende = true)
)

private val tidligereIdentiteter = listOf(
    Identitet(identitet = "10987654321", type = IdentitetType.FOLKEREGISTERIDENT, gjeldende = false),
    Identitet(identitet = "01234567890", type = IdentitetType.NPID, gjeldende = false)
)

private val eksempler: List<Pair<String, IdentitetHendelse>> = listOf(
    "identiteter-endret" to IdentiteterEndretHendelse(
        identiteter = identiteter,
        tidligereIdentiteter = tidligereIdentiteter,
        hendelseId = UUID.fromString("00000000-0000-4000-8000-000000000101"),
        hendelseTidspunkt = TIDSPUNKT
    ),
    "identiteter-merget" to IdentiteterMergetHendelse(
        identiteter = identiteter,
        tidligereIdentiteter = tidligereIdentiteter,
        hendelseId = UUID.fromString("00000000-0000-4000-8000-000000000102"),
        hendelseTidspunkt = TIDSPUNKT
    ),
    "identiteter-splittet" to IdentiteterSplittetHendelse(
        identiteter = identiteter,
        tidligereIdentiteter = emptyList(),
        hendelseId = UUID.fromString("00000000-0000-4000-8000-000000000103"),
        hendelseTidspunkt = TIDSPUNKT
    ),
    "identiteter-slettet" to IdentiteterSlettetHendelse(
        tidligereIdentiteter = tidligereIdentiteter,
        hendelseId = UUID.fromString("00000000-0000-4000-8000-000000000104"),
        hendelseTidspunkt = TIDSPUNKT
    )
)
