package no.nav.paw.arbeidssokerregisteret.application

import no.nav.paw.arbeidssokerregisteret.RequestScope
import no.nav.paw.arbeidssokerregisteret.domain.Identitetsnummer
import no.nav.paw.arbeidssokerregisteret.domain.navAnsatt
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
    val fakta: Iterable<Fakta>
    val regel: Regel<out Resultat>
}

sealed interface EndeligResultat : Resultat
sealed interface TilgangskontrollResultat : Resultat

data class OK(
    override val regel: Regel<EndeligResultat>,
    override val fakta: Iterable<Fakta>
) : EndeligResultat

data class Avvist(
    override val regel: Regel<EndeligResultat>,
    override val fakta: Iterable<Fakta>
) : EndeligResultat

data class Uavklart(
    override val regel: Regel<EndeligResultat>,
    override val fakta: Iterable<Fakta>
) : EndeligResultat

data class IkkeTilgang(
    override val regel: Regel<out Resultat>,
    override val fakta: Iterable<Fakta>
) : EndeligResultat, TilgangskontrollResultat

data class TilgangOK(
    override val regel: Regel<TilgangskontrollResultat>,
    override val fakta: Iterable<Fakta>
) : TilgangskontrollResultat

context(RequestScope)
fun somHendelse(identitetsnummer: Identitetsnummer, resultat: EndeligResultat): Hendelse =
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
        aarsak = resultat.regel.beskrivelse
    )
}
