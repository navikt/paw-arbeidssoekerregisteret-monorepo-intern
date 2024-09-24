package no.nav.paw.arbeidssoekerregisteret.testdata.mainavro

import no.nav.paw.arbeidssokerregisteret.api.v1.*
import java.time.Instant

fun metadata(
    kilde: String = "junit-kilde",
    aarsak: String = "junit-aarsak",
    tidspunkt: Instant = Instant.now(),
    tidspunktFraKilde: TidspunktFraKilde = tidsPunktFraKilde(),
    utfoertAv: Bruker = bruker()
): Metadata = Metadata.newBuilder()
    .setKilde(kilde)
    .setAarsak(aarsak)
    .setTidspunkt(tidspunkt)
    .setTidspunktFraKilde(tidspunktFraKilde)
    .setUtfoertAv(utfoertAv)
    .build()

fun tidsPunktFraKilde(
    tidspunkt: Instant = Instant.now(),
    avviksType: AvviksType = AvviksType.FORSINKELSE
): TidspunktFraKilde = TidspunktFraKilde.newBuilder()
    .setTidspunkt(tidspunkt)
    .setAvviksType(avviksType)
    .build()

fun bruker(
    id: String = "junit-bruker",
    type: BrukerType = BrukerType.SYSTEM
): Bruker = Bruker.newBuilder()
    .setId(id)
    .setType(type)
    .build()
