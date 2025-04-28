package no.nav.paw.arbeidssoekerregisteret.utgang.pdl.metrics

import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Tags
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.arbeidssoekerregisteret.utgang.pdl.kafka.serdes.Endring
import no.nav.paw.arbeidssoekerregisteret.utgang.pdl.kafka.serdes.HendelseState
import no.nav.paw.arbeidssoekerregisteret.utgang.pdl.kafka.serdes.OK
import no.nav.paw.arbeidssokerregisteret.api.v1.Metadata
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Opplysning
import java.time.Duration
import java.time.Instant

const val METRICS_UTGANG_PDL = "paw_arbeidssoekerregisteret_utgang_pdl"

const val AVSLUTTET_HENDELSE = "_avsluttet_hendelser"
const val AVSLUTTET_HENDELSE_AARSAK = "avsluttet_hendelse_aarsak"

const val PDL_HENT_PERSON = "_hent_person"
const val PDL_HENT_PERSON_STATUS = "hent_person_status"

fun PrometheusMeterRegistry.tellPdlAvsluttetHendelser(aarsak: String) = counter(
    METRICS_UTGANG_PDL + AVSLUTTET_HENDELSE,
    listOf(Tag.of(AVSLUTTET_HENDELSE_AARSAK, aarsak))
).increment()

fun PrometheusMeterRegistry.tellStatusFraPdlHentPersonBolk(status: String) = counter(
    METRICS_UTGANG_PDL + PDL_HENT_PERSON,
    listOf(Tag.of(PDL_HENT_PERSON_STATUS, status))
).increment()

fun PrometheusMeterRegistry.tellAvsluttetPeriode(metadata: Metadata, tilstand: HendelseState?) {
    val forsinkelse = tilstand?.sisteEndring?.tidspunkt?.let { sistEndring ->
        Duration.between(sistEndring, metadata.tidspunkt).toDays().tilMetricVerdi()
    } ?: "ingen_endring"

    val sisteTilstand = tilstand?.sisteEndring?.tilRegelId ?: OK
    val forrigeTilstand = tilstand?.sisteEndring?.fraRegelId ?: OK

    counter(
        "paw_arbeidssoekerregisteret_utgang_pdl_avsluttetv2",
        Tags.of(
            Tag.of(
                "siste_tilstand", sisteTilstand
            ),
            Tag.of(
                "forrige_tilstand", forrigeTilstand
            ),
            Tag.of(
                "forsinkelse_dager", forsinkelse
            ),
            Tag.of(
                "utfoert_av", metadata.utfoertAv.type.name.lowercase()
            ),
            Tag.of(
                "forhaandsgodkjent", tilstand?.opplysninger?.contains(Opplysning.FORHAANDSGODKJENT_AV_ANSATT)?.toString() ?: "ukjent"
            ),
            Tag.of(
                "kilde", metadata.kilde
                    .split("/paw/")
                    .let { if (it.size  >= 2) {
                        it[1].split(":").first()
                    } else it[0] }
                    .split(":")
                    .let {
                    if (it.size >=2) "${it[0]}_${it[1]}" else it[0]
                }
            )
        )
    ).increment()
}

fun PrometheusMeterRegistry.tellEndring(tidspunktForrigeEndring: Instant, forhaandsgodkjent: Boolean, endring: Endring) {
    val varighet = Duration.between(tidspunktForrigeEndring, Instant.now())
    val dager = varighet.toDays().tilMetricVerdi()
    counter(
        "paw_arbeidssoekerregisteret_utgang_pdl_endring",
        Tags.of(
            Tag.of(
                "fra", endring.fraRegelId
            ),
            Tag.of(
                "til", endring.tilRegelId
            ),
            Tag.of(
                "varighet_dager", dager
            ),
            Tag.of(
                "forhaandsgodkjent", forhaandsgodkjent.toString()
            )
        )
    ).increment()
}

private fun Long.tilMetricVerdi(): String =
    when {
        this < 0 -> "negative"
        this < 14 -> this.toString()
        this < 21 -> "14-21"
        this < 28 -> "21-28"
        this < 35 -> "28-35"
        else -> "35+"
    }




