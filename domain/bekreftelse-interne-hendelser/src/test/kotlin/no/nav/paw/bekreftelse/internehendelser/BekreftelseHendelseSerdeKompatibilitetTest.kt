package no.nav.paw.bekreftelse.internehendelser

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.equality.shouldBeEqualUsingFields
import no.nav.paw.bekreftelse.internehendelser.vo.Bruker
import no.nav.paw.bekreftelse.internehendelser.vo.BrukerType
import java.time.Duration
import java.time.Instant
import java.util.UUID

/**
 * Verifiserer at bekreftelsehendelser skrevet med Jackson 2 kan leses av gjeldende
 * BekreftelseHendelseSerde. JSON-filene er generert med Jackson 2, se [lagretJson].
 */
class BekreftelseHendelseSerdeKompatibilitetTest : FreeSpec({
    val serde = BekreftelseHendelseSerde()

    eksempler.forEach { (navn, forventet) ->
        "$navn skrevet med Jackson 2 kan leses" {
            val json = lagretJson("bekreftelse-interne-hendelser/$navn") {
                serde.serializer().serialize("topic", forventet)!!
            }
            serde.deserializer().deserialize("topic", json).shouldBeEqualUsingFields(forventet)
        }
    }

    "tidspunkt med nanosekunder skrevet med Jackson 2 leses med under 1 µs avvik" {
        // Deserializeren går via readTree, som leser tidspunktet som double.
        val forventet = Instant.parse("2025-03-14T09:26:53.589793238Z")
        val hendelse = PeriodeAvsluttet(
            hendelseId = UUID.fromString("4e5f6a7b-8c9d-4e0f-8a1b-2c3d4e5f6a7b"),
            periodeId = PERIODE_ID,
            arbeidssoekerId = ARBEIDSSOEKER_ID,
            hendelseTidspunkt = forventet
        )
        val json = lagretJson("bekreftelse-interne-hendelser/periode-avsluttet-nanosekunder") {
            serde.serializer().serialize("topic", hendelse)!!
        }
        val faktisk = serde.deserializer().deserialize("topic", json).hendelseTidspunkt
        Duration.between(forventet, faktisk).abs() shouldBeLessThan Duration.ofNanos(1_000)
    }
})

private val PERIODE_ID = UUID.fromString("3a1b2c3d-4e5f-4a6b-8c7d-9e0f1a2b3c4d")
private val BEKREFTELSE_ID = UUID.fromString("5f6a7b8c-9d0e-4f1a-8b2c-3d4e5f6a7b8c")
private const val ARBEIDSSOEKER_ID = 1001L
private val TIDSPUNKT = Instant.parse("2025-03-14T09:26:53.589Z")

private val eksempler: List<Pair<String, BekreftelseHendelse>> = listOf(
    "leveringsfrist-utloept" to LeveringsfristUtloept(
        hendelseId = UUID.fromString("00000000-0000-4000-8000-000000000001"),
        periodeId = PERIODE_ID,
        arbeidssoekerId = ARBEIDSSOEKER_ID,
        hendelseTidspunkt = TIDSPUNKT,
        bekreftelseId = BEKREFTELSE_ID,
        leveringsfrist = Instant.parse("2025-03-28T23:00:00Z")
    ),
    "ekstern-grace-periode-utloept" to EksternGracePeriodeUtloept(
        hendelseId = UUID.fromString("00000000-0000-4000-8000-000000000002"),
        periodeId = PERIODE_ID,
        arbeidssoekerId = ARBEIDSSOEKER_ID,
        hendelseTidspunkt = TIDSPUNKT,
        paaVegneAvNamespace = "teamdagpenger",
        paaVegneAvId = "dp-rapportering"
    ),
    "register-grace-periode-utloept" to RegisterGracePeriodeUtloept(
        hendelseId = UUID.fromString("00000000-0000-4000-8000-000000000003"),
        periodeId = PERIODE_ID,
        arbeidssoekerId = ARBEIDSSOEKER_ID,
        hendelseTidspunkt = TIDSPUNKT,
        bekreftelseId = BEKREFTELSE_ID,
        kilde = "test-kilde"
    ),
    "bekreftelse-tilgjengelig" to BekreftelseTilgjengelig(
        hendelseId = UUID.fromString("00000000-0000-4000-8000-000000000004"),
        periodeId = PERIODE_ID,
        arbeidssoekerId = ARBEIDSSOEKER_ID,
        hendelseTidspunkt = TIDSPUNKT,
        bekreftelseId = BEKREFTELSE_ID,
        gjelderFra = Instant.parse("2025-03-02T23:00:00Z"),
        gjelderTil = Instant.parse("2025-03-16T23:00:00Z")
    ),
    "bekreftelse-melding-mottatt" to BekreftelseMeldingMottatt(
        hendelseId = UUID.fromString("00000000-0000-4000-8000-000000000005"),
        periodeId = PERIODE_ID,
        arbeidssoekerId = ARBEIDSSOEKER_ID,
        hendelseTidspunkt = TIDSPUNKT,
        bekreftelseId = BEKREFTELSE_ID
    ),
    "periode-avsluttet" to PeriodeAvsluttet(
        hendelseId = UUID.fromString("00000000-0000-4000-8000-000000000006"),
        periodeId = PERIODE_ID,
        arbeidssoekerId = ARBEIDSSOEKER_ID,
        hendelseTidspunkt = TIDSPUNKT
    ),
    "register-grace-periode-gjenstaaende-tid" to RegisterGracePeriodeGjenstaaendeTid(
        hendelseId = UUID.fromString("00000000-0000-4000-8000-000000000007"),
        periodeId = PERIODE_ID,
        arbeidssoekerId = ARBEIDSSOEKER_ID,
        hendelseTidspunkt = TIDSPUNKT,
        bekreftelseId = BEKREFTELSE_ID,
        gjenstaandeTid = Duration.ofDays(3).plusHours(4).plusMillis(250)
    ),
    "ba-om-aa-avslutte-periode" to BaOmAaAvsluttePeriode(
        hendelseId = UUID.fromString("00000000-0000-4000-8000-000000000008"),
        periodeId = PERIODE_ID,
        arbeidssoekerId = ARBEIDSSOEKER_ID,
        hendelseTidspunkt = TIDSPUNKT,
        utfoertAv = Bruker(type = BrukerType.SLUTTBRUKER, id = "12345678901", sikkerhetsnivaa = "idporten-loa-high"),
        kilde = "test-kilde"
    ),
    "bekreftelse-paa-vegne-av-startet" to BekreftelsePaaVegneAvStartet(
        hendelseId = UUID.fromString("00000000-0000-4000-8000-000000000009"),
        periodeId = PERIODE_ID,
        arbeidssoekerId = ARBEIDSSOEKER_ID,
        hendelseTidspunkt = TIDSPUNKT
    ),
    "register-grace-periode-utloept-etter-ekstern-innsamling" to RegisterGracePeriodeUtloeptEtterEksternInnsamling(
        hendelseId = UUID.fromString("00000000-0000-4000-8000-000000000010"),
        periodeId = PERIODE_ID,
        arbeidssoekerId = ARBEIDSSOEKER_ID,
        hendelseTidspunkt = TIDSPUNKT,
        kilde = "test-kilde"
    )
)
