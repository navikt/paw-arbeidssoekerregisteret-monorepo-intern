package no.nav.paw.bekreftelsetjeneste.kompatibilitet

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import no.nav.paw.bekreftelsetjeneste.paavegneav.BekreftelsePaaVegneAvSerde
import no.nav.paw.bekreftelsetjeneste.paavegneav.InternPaaVegneAv
import no.nav.paw.bekreftelsetjeneste.paavegneav.Loesning
import no.nav.paw.bekreftelsetjeneste.paavegneav.PaaVegneAvTilstand
import no.nav.paw.bekreftelsetjeneste.tilstand.Bekreftelse
import no.nav.paw.bekreftelsetjeneste.tilstand.BekreftelseTilstand
import no.nav.paw.bekreftelsetjeneste.tilstand.BekreftelseTilstandsLogg
import no.nav.paw.bekreftelsetjeneste.tilstand.GracePeriodeUtloept
import no.nav.paw.bekreftelsetjeneste.tilstand.GracePeriodeVarselet
import no.nav.paw.bekreftelsetjeneste.tilstand.IkkeKlarForUtfylling
import no.nav.paw.bekreftelsetjeneste.tilstand.InternBekreftelsePaaVegneAvStartet
import no.nav.paw.bekreftelsetjeneste.tilstand.InternTilstandSerde
import no.nav.paw.bekreftelsetjeneste.tilstand.KlarForUtfylling
import no.nav.paw.bekreftelsetjeneste.tilstand.Levert
import no.nav.paw.bekreftelsetjeneste.tilstand.PeriodeInfo
import no.nav.paw.bekreftelsetjeneste.tilstand.VenterSvar
import java.time.Duration
import java.time.Instant
import java.util.UUID

/**
 * Verifiserer at tilstand i state stores skrevet med Jackson 2 kan leses av gjeldende
 * InternTilstandSerde og BekreftelsePaaVegneAvSerde. JSON-filene er generert med
 * Jackson 2, se [lagretJson].
 */
class StateStoreSerdeKompatibilitetTest : FreeSpec({
    val tilstandSerde = InternTilstandSerde()
    val paaVegneAvSerde = BekreftelsePaaVegneAvSerde()

    bekreftelseTilstander.forEach { (navn, forventet) ->
        "BekreftelseTilstand $navn skrevet med Jackson 2 kan leses" {
            val json = lagretJson("bekreftelse-tjeneste/bekreftelse-tilstand-$navn") {
                tilstandSerde.serializer().serialize("topic", forventet)
            }
            tilstandSerde.deserializer().deserialize("topic", json) shouldBe forventet
        }
    }

    paaVegneAvTilstander.forEach { (navn, forventet) ->
        "PaaVegneAvTilstand $navn skrevet med Jackson 2 kan leses" {
            val json = lagretJson("bekreftelse-tjeneste/paa-vegne-av-tilstand-$navn") {
                paaVegneAvSerde.serializer().serialize("topic", forventet)
            }
            paaVegneAvSerde.deserializer().deserialize("topic", json) shouldBe forventet
        }
    }
})

private val PERIODE_ID = UUID.fromString("3a1b2c3d-4e5f-4a6b-8c7d-9e0f1a2b3c4d")

private fun tidspunkt(verdi: String) = Instant.parse(verdi)

private val bekreftelseTilstander: List<Pair<String, BekreftelseTilstand>> = listOf(
    "med-bekreftelser" to BekreftelseTilstand(
        kafkaPartition = 3,
        periode = PeriodeInfo(
            periodeId = PERIODE_ID,
            identitetsnummer = "12345678901",
            arbeidsoekerId = 1001L,
            recordKey = -1001L,
            startet = tidspunkt("2025-01-06T09:26:53.589Z"),
            avsluttet = null
        ),
        bekreftelser = listOf(
            Bekreftelse(
                tilstandsLogg = BekreftelseTilstandsLogg(
                    siste = Levert(tidspunkt("2025-01-20T10:00:00.001Z")),
                    tidligere = listOf(
                        IkkeKlarForUtfylling(tidspunkt("2025-01-06T09:26:53.589Z")),
                        KlarForUtfylling(tidspunkt("2025-01-17T23:00:00Z")),
                        VenterSvar(tidspunkt("2025-01-19T23:00:00Z")),
                        GracePeriodeVarselet(tidspunkt("2025-01-20T06:00:00Z"))
                    )
                ),
                bekreftelseId = UUID.fromString("5f6a7b8c-9d0e-4f1a-8b2c-3d4e5f6a7b8c"),
                gjelderFra = tidspunkt("2025-01-06T09:26:53.589Z"),
                gjelderTil = tidspunkt("2025-01-19T23:00:00Z")
            ),
            Bekreftelse(
                tilstandsLogg = BekreftelseTilstandsLogg(
                    siste = GracePeriodeUtloept(tidspunkt("2025-02-10T23:00:00Z")),
                    tidligere = listOf(
                        InternBekreftelsePaaVegneAvStartet(tidspunkt("2025-01-25T12:00:00Z"))
                    )
                ),
                bekreftelseId = UUID.fromString("6a7b8c9d-0e1f-4a2b-8c3d-4e5f6a7b8c9d"),
                gjelderFra = tidspunkt("2025-01-19T23:00:00Z"),
                gjelderTil = tidspunkt("2025-02-02T23:00:00Z"),
                dummy = true
            )
        )
    ),
    "avsluttet-uten-bekreftelser" to BekreftelseTilstand(
        kafkaPartition = 0,
        periode = PeriodeInfo(
            periodeId = PERIODE_ID,
            identitetsnummer = "12345678901",
            arbeidsoekerId = 1002L,
            recordKey = -1002L,
            startet = tidspunkt("2024-01-01T08:00:00Z"),
            avsluttet = tidspunkt("2024-06-01T08:00:00.5Z")
        ),
        bekreftelser = emptyList()
    )
)

private val paaVegneAvTilstander: List<Pair<String, PaaVegneAvTilstand>> = listOf(
    "med-loesninger" to PaaVegneAvTilstand(
        periodeId = PERIODE_ID,
        paaVegneAvList = listOf(
            InternPaaVegneAv(
                loesning = Loesning.DAGPENGER,
                intervall = Duration.ofDays(14),
                gracePeriode = Duration.ofDays(7).plusMillis(500)
            ),
            InternPaaVegneAv(
                loesning = Loesning.FRISKMELDT_TIL_ARBEIDSFORMIDLING,
                intervall = Duration.ofDays(14),
                gracePeriode = Duration.ofHours(36)
            )
        )
    ),
    "tom" to PaaVegneAvTilstand(periodeId = PERIODE_ID, paaVegneAvList = emptyList())
)
