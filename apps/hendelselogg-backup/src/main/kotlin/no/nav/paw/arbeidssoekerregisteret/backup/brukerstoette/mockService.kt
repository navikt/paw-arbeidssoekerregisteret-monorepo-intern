package no.nav.paw.arbeidssoekerregisteret.backup.brukerstoette

import no.nav.paw.arbeidssoekerregisteret.backup.api.brukerstoette.models.*
import no.nav.paw.arbeidssokerregisteret.intern.v1.Avsluttet
import no.nav.paw.arbeidssokerregisteret.intern.v1.Startet
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Bruker
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.BrukerType
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Metadata
import java.time.Instant
import java.util.*

fun getMockResponse(): DetaljerResponse {
    return DetaljerResponse(
        recordKey = 123L,
        kafkaPartition = 3,
        arbeidssoekerId = 831645L,
        gjeldeneTilstand = Tilstand(
            harAktivePeriode = true,
            startet = Instant.now().minusSeconds(10000),
            avsluttet = null,
            harOpplysningerMottattHendelse = true,
            apiKall = TilstandApiKall(
                harOpplysning = true,
                harProfilering = true
            )
        ),
        historikk = listOf(
            InnkommendeHendelse(
                gjeldeneTilstand = Tilstand(
                    harAktivePeriode = false,
                    startet = Instant.now().minusSeconds(20000),
                    avsluttet = Instant.now().minusSeconds(19000),
                    harOpplysningerMottattHendelse = true,
                    apiKall = TilstandApiKall(
                        harOpplysning = true,
                        harProfilering = true
                    )
                ),
                nyTilstand = Tilstand(
                    harAktivePeriode = true,
                    startet = Instant.now().minusSeconds(10000),
                    avsluttet = null,
                    harOpplysningerMottattHendelse = true,
                    apiKall = TilstandApiKall(
                        harOpplysning = true,
                        harProfilering = false
                    )
                ),
                hendelse = Hendelse(
                    hendelseId = UUID.randomUUID(),
                    hendelseType = "Startet",
                    metadata = HendelseMetadata(
                        tidspunkt = Instant.now().minusSeconds(19000),
                        utfoertAv = HendelseMetadataUtfoertAv(
                            id = "12345678901",
                            type = "SLUTTBRUKER"
                        ),
                        kilde = "mock data",
                        aarsak = "bare mock data"
                    ),
                    kafkaOffset = 2345423L,
                    data = Startet(
                        identitetsnummer = "12345678901",
                        hendelseId = UUID.randomUUID(),
                        id = 123L,
                        metadata = Metadata(
                            tidspunkt = Instant.now().minusSeconds(19000),
                            utfoertAv = Bruker(
                                id = "12345678901",
                                type = BrukerType.SLUTTBRUKER
                            ),
                            kilde = "mock data",
                            aarsak = "bare mock data"
                        )
                    )
                )
            ),
            InnkommendeHendelse(
                gjeldeneTilstand = Tilstand(
                    harAktivePeriode = true,
                    startet = Instant.now().minusSeconds(20000),
                    avsluttet = Instant.now().minusSeconds(19000),
                    harOpplysningerMottattHendelse = true,
                    apiKall = TilstandApiKall(
                        harOpplysning = true,
                        harProfilering = true
                    )
                ),
                nyTilstand = Tilstand(
                    harAktivePeriode = false,
                    startet = Instant.now().minusSeconds(10000),
                    avsluttet = null,
                    harOpplysningerMottattHendelse = true,
                    apiKall = TilstandApiKall(
                        harOpplysning = true,
                        harProfilering = false
                    )
                ),
                hendelse = Hendelse(
                    hendelseId = UUID.randomUUID(),
                    hendelseType = "Avsluttet",
                    metadata = HendelseMetadata(
                        tidspunkt = Instant.now().minusSeconds(29000),
                        utfoertAv = HendelseMetadataUtfoertAv(
                            id = "12345678901",
                            type = "SLUTTBRUKER"
                        ),
                        kilde = "mock data",
                        aarsak = "bare mock data"
                    ),
                    kafkaOffset = 2345423L,
                    data = Avsluttet(
                        identitetsnummer = "12345678901",
                        hendelseId = UUID.randomUUID(),
                        id = 123L,
                        metadata = Metadata(
                            tidspunkt = Instant.now().minusSeconds(19000),
                            utfoertAv = Bruker(
                                id = "12345678901",
                                type = BrukerType.SLUTTBRUKER
                            ),
                            kilde = "mock data",
                            aarsak = "bare mock data"
                        )
                    )
                )
            )
        )
    )
}
