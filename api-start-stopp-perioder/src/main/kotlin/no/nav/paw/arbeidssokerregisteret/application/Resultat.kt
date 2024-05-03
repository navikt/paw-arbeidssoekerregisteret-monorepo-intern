package no.nav.paw.arbeidssokerregisteret.application

import no.nav.paw.arbeidssoekerregisteret.api.opplysningermottatt.models.OpplysningerRequest
import no.nav.paw.arbeidssokerregisteret.ApplicationInfo
import no.nav.paw.arbeidssokerregisteret.RequestScope
import no.nav.paw.arbeidssokerregisteret.api.extensions.toInternalApi
import no.nav.paw.arbeidssokerregisteret.domain.Identitetsnummer
import no.nav.paw.arbeidssokerregisteret.domain.navAnsatt
import no.nav.paw.arbeidssokerregisteret.intern.v1.*
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Bruker
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.BrukerType
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.OpplysningerOmArbeidssoeker
import no.nav.paw.arbeidssokerregisteret.utils.TokenXPID
import java.time.Instant
import java.util.*
import no.nav.paw.arbeidssokerregisteret.intern.v1.Avvist as AvvistHendelse
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Metadata as HendelseMetadata

sealed interface Resultat {
    val opplysning: Iterable<Opplysning>
    val regel: Regel<out Resultat>
}

sealed interface EndeligResultat : Resultat
sealed interface TilgangskontrollResultat : Resultat

data class OK(
    override val regel: Regel<EndeligResultat>,
    override val opplysning: Iterable<Opplysning>
) : EndeligResultat

data class Avvist(
    override val regel: Regel<EndeligResultat>,
    override val opplysning: Iterable<Opplysning>
) : EndeligResultat

data class Uavklart(
    override val regel: Regel<EndeligResultat>,
    override val opplysning: Iterable<Opplysning>
) : EndeligResultat

data class IkkeTilgang(
    override val regel: Regel<out Resultat>,
    override val opplysning: Iterable<Opplysning>
) : EndeligResultat, TilgangskontrollResultat

data class TilgangOK(
    override val regel: Regel<TilgangskontrollResultat>,
    override val opplysning: Iterable<Opplysning>
) : TilgangskontrollResultat

data class UgyldigRequestBasertPaaAutentisering(
    override val regel: Regel<TilgangskontrollResultat>,
    override val opplysning: Iterable<Opplysning>
) : EndeligResultat, TilgangskontrollResultat

context(RequestScope)
fun stoppResultatSomHendelse(id: Long, identitetsnummer: Identitetsnummer, resultat: TilgangskontrollResultat): Hendelse =
    when (resultat) {
        is IkkeTilgang -> AvvistStoppAvPeriode(
            id = id,
            hendelseId = UUID.randomUUID(),
            identitetsnummer = identitetsnummer.verdi,
            metadata = hendelseMetadata(resultat)
        )

        is TilgangOK -> Avsluttet(
            id = id,
            hendelseId = UUID.randomUUID(),
            identitetsnummer = identitetsnummer.verdi,
            metadata = hendelseMetadata(resultat)
        )

        is UgyldigRequestBasertPaaAutentisering -> AvvistStoppAvPeriode(
            id = id,
            hendelseId = UUID.randomUUID(),
            identitetsnummer = identitetsnummer.verdi,
            metadata = hendelseMetadata(resultat)
        )
    }

context(RequestScope)
fun somHendelse(id: Long, identitetsnummer: Identitetsnummer, resultat: EndeligResultat): Hendelse =
    when (resultat) {
        is Avvist -> AvvistHendelse(
            id = id,
            hendelseId = UUID.randomUUID(),
            identitetsnummer = identitetsnummer.verdi,
            metadata = hendelseMetadata(resultat),
            opplysninger = resultat.opplysning.map(::mapToHendelseOpplysning).toSet()
        )

        is IkkeTilgang -> AvvistHendelse(
            id = id,
            hendelseId = UUID.randomUUID(),
            identitetsnummer = identitetsnummer.verdi,
            metadata = hendelseMetadata(resultat)
        )

        is OK -> Startet(
            id = id,
            hendelseId = UUID.randomUUID(),
            identitetsnummer = identitetsnummer.verdi,
            metadata = hendelseMetadata(resultat),
            opplysninger = resultat.opplysning.map(::mapToHendelseOpplysning).toSet()
        )

        is Uavklart -> AvvistHendelse(
            id = id,
            hendelseId = UUID.randomUUID(),
            identitetsnummer = identitetsnummer.verdi,
            metadata = hendelseMetadata(resultat),
            opplysninger = resultat.opplysning.map(::mapToHendelseOpplysning).toSet()
        )

        is UgyldigRequestBasertPaaAutentisering -> AvvistHendelse(
            id = id,
            hendelseId = UUID.randomUUID(),
            identitetsnummer = identitetsnummer.verdi,
            metadata = hendelseMetadata(resultat),
            opplysninger = resultat.opplysning.map(::mapToHendelseOpplysning).toSet()
        )
    }

context(RequestScope)
fun opplysningerHendelse(id: Long, opplysningerRequest: OpplysningerRequest): Hendelse = OpplysningerOmArbeidssoekerMottatt(
    hendelseId = UUID.randomUUID(),
    id = id,
    identitetsnummer = opplysningerRequest.identitetsnummer,
    opplysningerOmArbeidssoeker = OpplysningerOmArbeidssoeker(
        id = UUID.randomUUID(),
        metadata = HendelseMetadata(
            tidspunkt = Instant.now(),
            utfoertAv = brukerFraClaims(),
            kilde = "paw-arbeidssoekerregisteret-inngang",
            aarsak = "opplysning om arbeidssøker sendt inn"
        ),
        annet = opplysningerRequest.opplysningerOmArbeidssoeker.annet.toInternalApi(),
        helse = opplysningerRequest.opplysningerOmArbeidssoeker.helse.toInternalApi(),
        jobbsituasjon = opplysningerRequest.opplysningerOmArbeidssoeker.jobbsituasjon.toInternalApi(),
        utdanning = opplysningerRequest.opplysningerOmArbeidssoeker.utdanning.toInternalApi()
    )
)

context(RequestScope)
fun hendelseMetadata(resultat: Resultat): HendelseMetadata = HendelseMetadata(
    tidspunkt = Instant.now(),
    utfoertAv = brukerFraClaims(),
    kilde = ApplicationInfo.id,
    aarsak = resultat.regel.beskrivelse
)

context(RequestScope)
fun brukerFraClaims(): Bruker {
    return claims[TokenXPID]?.let { foedselsnummer ->
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
}
