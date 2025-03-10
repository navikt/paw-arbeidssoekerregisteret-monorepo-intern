package no.nav.paw.arbeidssoekerregisteret.testdata.internehendelser

import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.*
import java.time.Duration
import java.time.Instant

fun metadata(
    tidspunkt: Instant = Instant.now(),
    utfoertAv: Bruker = sluttBruker(),
    kilde: String = "junit-kilde",
    aarsak: String = "junit-aarsak",
    tidspunktFraKilde: TidspunktFraKilde? = null
): Metadata = Metadata(
    tidspunkt = tidspunkt,
    utfoertAv = utfoertAv,
    kilde = kilde,
    aarsak = aarsak,
    tidspunktFraKilde = tidspunktFraKilde
)

fun sluttBruker(identitetsnummer: String = "12345678901"): Bruker = bruker(id = identitetsnummer, type = BrukerType.SLUTTBRUKER)
fun veillederBruker(ident: String): Bruker = bruker(id = ident, type = BrukerType.VEILEDER)
fun systemBruker(system: String): Bruker = bruker(id = system, type = BrukerType.SYSTEM)

fun bruker (
    id: String = "09876543211",
    type: BrukerType = BrukerType.SLUTTBRUKER,
    sikkerhetsnivaa: String? = "idporten-loa-high"
): Bruker = Bruker(
    id = id,
    type = type,
    sikkerhetsnivaa = sikkerhetsnivaa
)

fun feilregistrering(): TidspunktFraKilde = tidsPunktFraKilde(avviksType = AvviksType.SLETTET)
fun tidspunktKorrigert(tidspunkt: Instant): TidspunktFraKilde = tidsPunktFraKilde(
    tidspunkt = tidspunkt,
    avviksType = AvviksType.TIDSPUNKT_KORRIGERT
)

fun tidsPunktFraKilde(
    tidspunkt: Instant = Instant.now(),
    avviksType: AvviksType = AvviksType.FORSINKELSE
): TidspunktFraKilde = TidspunktFraKilde(
    tidspunkt = tidspunkt,
    avviksType = avviksType
)
