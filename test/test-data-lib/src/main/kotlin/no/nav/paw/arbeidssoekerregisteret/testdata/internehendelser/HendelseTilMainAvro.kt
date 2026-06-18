package no.nav.paw.arbeidssoekerregisteret.testdata.internehendelser

import no.nav.paw.arbeidssokerregisteret.api.v1.AvslutningsInfo
import no.nav.paw.arbeidssokerregisteret.api.v1.AvsluttetAarsakType.BEKREFTELSE_IKKE_LEVERT_INNEN_FRIST
import no.nav.paw.arbeidssokerregisteret.api.v1.AvsluttetAarsakType.SVARTE_NEI_I_BEKREFTELSE
import no.nav.paw.arbeidssokerregisteret.api.v1.AvsluttetAarsakType.UDEFINERT
import no.nav.paw.arbeidssokerregisteret.api.v1.AvsluttetAarsakType.UKJENT_VERDI
import no.nav.paw.arbeidssokerregisteret.api.v1.AvviksType
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.arbeidssokerregisteret.intern.v1.Avsluttet
import no.nav.paw.arbeidssokerregisteret.intern.v1.Startet
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Aarsaksinformasjon
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.AvsluttetAarsakType
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.AvviksType.FORSINKELSE
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.AvviksType.RETTING
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.AvviksType.SLETTET
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.AvviksType.TIDSPUNKT_KORRIGERT
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Bruker
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.BrukerType
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Metadata
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.TidspunktFraKilde
import no.nav.paw.arbeidssokerregisteret.api.v1.Metadata as AvroMetadata


fun Startet.tilAvroPeriode(): Periode =
    Periode(
        hendelseId,
        identitetsnummer,
        metadata.tilAvroMetadata(),
        null,
        null
    )

fun Avsluttet.tilAvroPeriode(periode: Periode): Periode =
    Periode(
        periode.id,
        periode.identitetsnummer,
        periode.startet,
        metadata.tilAvroMetadata(),
        aarsaksInformasjon?.tilAvro()
    )

fun Aarsaksinformasjon.tilAvro() : AvslutningsInfo = AvslutningsInfo(
    no.nav.paw.arbeidssokerregisteret.api.v1.Aarsaksinformasjon(
        when(this.aarsak) {
            AvsluttetAarsakType.SVARTE_NEI_I_BEKREFTELSE -> SVARTE_NEI_I_BEKREFTELSE
            AvsluttetAarsakType.BEKREFTELSE_IKKE_LEVERT_INNEN_FRIST -> BEKREFTELSE_IKKE_LEVERT_INNEN_FRIST
            AvsluttetAarsakType.FEILREGISTRERING -> UDEFINERT
            AvsluttetAarsakType.UDEFINERT -> UDEFINERT
            AvsluttetAarsakType.UKJENT_VERDI -> UKJENT_VERDI
        }
    )
)

fun Metadata.tilAvroMetadata(): AvroMetadata =
    AvroMetadata(
        tidspunkt,
        utfoertAv.tilAvroBruker(),
        kilde,
        aarsak,
        tidspunktFraKilde?.tilAvroTidspunktFraKilde()
    )

fun Bruker.tilAvroBruker(): no.nav.paw.arbeidssokerregisteret.api.v1.Bruker =
    no.nav.paw.arbeidssokerregisteret.api.v1.Bruker(
        when (type) {
            BrukerType.UDEFINERT -> no.nav.paw.arbeidssokerregisteret.api.v1.BrukerType.UDEFINERT
            BrukerType.UKJENT_VERDI -> no.nav.paw.arbeidssokerregisteret.api.v1.BrukerType.UKJENT_VERDI
            BrukerType.SYSTEM -> no.nav.paw.arbeidssokerregisteret.api.v1.BrukerType.SYSTEM
            BrukerType.SLUTTBRUKER -> no.nav.paw.arbeidssokerregisteret.api.v1.BrukerType.SLUTTBRUKER
            BrukerType.VEILEDER -> no.nav.paw.arbeidssokerregisteret.api.v1.BrukerType.VEILEDER
        },
        id,
        sikkerhetsnivaa
    )

fun TidspunktFraKilde.tilAvroTidspunktFraKilde(): no.nav.paw.arbeidssokerregisteret.api.v1.TidspunktFraKilde =
    no.nav.paw.arbeidssokerregisteret.api.v1.TidspunktFraKilde(
        tidspunkt,
        when (avviksType) {
            FORSINKELSE -> AvviksType.FORSINKELSE
            TIDSPUNKT_KORRIGERT -> AvviksType.TIDSPUNKT_KORRIGERT
            SLETTET -> AvviksType.SLETTET
            RETTING -> AvviksType.RETTING
        }
    )