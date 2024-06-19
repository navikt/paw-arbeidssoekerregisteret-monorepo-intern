package no.nav.paw.arbeidssoekerregisteret.backup.brukerstoette

import no.nav.paw.arbeidssoekerregisteret.backup.api.brukerstoette.models.DetaljerResponse
import no.nav.paw.arbeidssoekerregisteret.backup.api.brukerstoette.models.Tilstand
import no.nav.paw.arbeidssoekerregisteret.backup.api.brukerstoette.models.TilstandApiKall
import java.time.Instant

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
        historikk = listOf()
    )
}
