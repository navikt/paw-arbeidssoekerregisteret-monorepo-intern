package no.nav.paw.kafkakeymaintenance.pdlprocessor.functions

import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Bruker
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.BrukerType
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Metadata
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.TidspunktFraKilde
import java.time.Instant

private val systemId = System.getenv("IMAGE_WITH_VERSION")?: "UNSPECIFIED"

fun metadata(
    kilde: String,
    tidspunkt: Instant = Instant.now(),
    tidspunktFraKilde: TidspunktFraKilde
): Metadata {

    return Metadata(
        tidspunkt = tidspunkt,
        utfoertAv = Bruker(
            type = BrukerType.SYSTEM,
            id = systemId
        ),
        kilde = kilde,
        aarsak = "Id oppdatering",
        tidspunktFraKilde = tidspunktFraKilde
    )
}
