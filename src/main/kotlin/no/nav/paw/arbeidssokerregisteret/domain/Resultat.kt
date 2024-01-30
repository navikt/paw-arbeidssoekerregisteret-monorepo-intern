package no.nav.paw.arbeidssokerregisteret.domain

import no.nav.paw.arbeidssokerregisteret.RequestScope
import no.nav.paw.arbeidssokerregisteret.evaluering.Attributt
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.arbeidssokerregisteret.intern.v1.Startet
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Bruker
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.BrukerType
import no.nav.paw.arbeidssokerregisteret.utils.TokenXPID
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Metadata as HendelseMetadata
import java.time.Instant
import java.util.*
import no.nav.paw.arbeidssokerregisteret.intern.v1.Avvist as AvvistHendelse

sealed interface Resultat {
    val attributt: Iterable<Attributt>
    val melding: String
}

sealed interface TilgangskontrollResultat {
    val attributt: Iterable<Attributt>
    val melding: String
}

data class OK(
    override val melding: String,
    override val attributt: Iterable<Attributt>
) : Resultat, TilgangskontrollResultat

data class Avvist(
    override val melding: String,
    override val attributt: Iterable<Attributt>
) : Resultat

data class Uavklart(
    override val melding: String,
    override val attributt: Iterable<Attributt>
) : Resultat

data class IkkeTilgang(
    override val melding: String,
    override val attributt: Iterable<Attributt>
) : Resultat, TilgangskontrollResultat

context(RequestScope)
fun somHendelse(identitetsnummer: Identitetsnummer, resultat: Resultat): Hendelse =
    when (resultat) {
        is Avvist -> AvvistHendelse(
            hendelseId = UUID.randomUUID(),
            identitetsnummer = identitetsnummer.verdi,
            metadata = hendelseMetadata(resultat)
        )

        is IkkeTilgang -> AvvistHendelse(
            hendelseId = UUID.randomUUID(),
            identitetsnummer = identitetsnummer.verdi,
            metadata = hendelseMetadata(resultat)
        )

        is OK -> Startet(
            hendelseId = UUID.randomUUID(),
            identitetsnummer = identitetsnummer.verdi,
            metadata = hendelseMetadata(resultat)
        )

        is Uavklart -> AvvistHendelse(
            hendelseId = UUID.randomUUID(),
            identitetsnummer = identitetsnummer.verdi,
            metadata = hendelseMetadata(resultat)
        )
    }

context(RequestScope)
fun hendelseMetadata(resultat: Resultat): HendelseMetadata {
    val bruker = claims[TokenXPID]?.let { foedselsnummer ->
        Bruker(
            type = BrukerType.SLUTTBRUKER,
            id = foedselsnummer.verdi
        )
    } ?: navAnsatt(claims)?.let { navAnsatt ->
        Bruker(
            type = BrukerType.VEILEDER,
            id = navAnsatt.ident
        )
    } ?: throw IllegalStateException("Kunne ikke finne bruker i claims")
    return HendelseMetadata(
        tidspunkt = Instant.now(),
        utfoertAv = bruker,
        kilde = "paw-arbeidssoelerregisteret-inngang",
        aarsak = resultat.melding
    )
}
