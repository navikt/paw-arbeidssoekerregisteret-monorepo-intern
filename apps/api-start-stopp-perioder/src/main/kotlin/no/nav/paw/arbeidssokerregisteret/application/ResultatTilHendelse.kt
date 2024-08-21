package no.nav.paw.arbeidssokerregisteret.application

import arrow.core.Either
import no.nav.paw.arbeidssoekerregisteret.api.opplysningermottatt.models.OpplysningerRequest
import no.nav.paw.arbeidssokerregisteret.ApplicationInfo
import no.nav.paw.arbeidssokerregisteret.RequestScope
import no.nav.paw.arbeidssokerregisteret.api.extensions.toInternalApi
import no.nav.paw.arbeidssokerregisteret.application.authfaktka.AuthOpplysning
import no.nav.paw.arbeidssokerregisteret.application.opplysninger.DomeneOpplysning
import no.nav.paw.arbeidssokerregisteret.application.opplysninger.Opplysning
import no.nav.paw.arbeidssokerregisteret.authOpplysningTilHendelseOpplysning
import no.nav.paw.arbeidssokerregisteret.domain.Identitetsnummer
import no.nav.paw.arbeidssokerregisteret.domain.navAnsatt
import no.nav.paw.arbeidssokerregisteret.intern.v1.*
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.*
import no.nav.paw.arbeidssokerregisteret.utils.TokenXPID
import java.time.Instant
import java.util.*
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Opplysning as HendelseOpplysning


context(RequestScope)
fun stoppResultatSomHendelse(id: Long, identitetsnummer: Identitetsnummer, resultat: Either<Problem, GrunnlagForGodkjenning>): Hendelse =
    when (resultat) {
        is Either.Left -> AvvistStoppAvPeriode(
            id = id,
            hendelseId = UUID.randomUUID(),
            identitetsnummer = identitetsnummer.verdi,
            metadata = hendelseMetadata(resultat.value.regel.id.beskrivelse),
            opplysninger = resultat.value.opplysning.map(::mapToHendelseOpplysning).toSet()
        )

        is Either.Right -> Avsluttet(
            id = id,
            hendelseId = UUID.randomUUID(),
            identitetsnummer = identitetsnummer.verdi,
            metadata = hendelseMetadata("Stopp av periode"),
            opplysninger = resultat.value.opplysning.map(::mapToHendelseOpplysning).toSet(),
        )
    }

fun mapToHendelseOpplysning(opplysning: Opplysning): HendelseOpplysning =
    when (opplysning) {
        is AuthOpplysning -> authOpplysningTilHendelseOpplysning(opplysning)
        is DomeneOpplysning -> domeneOpplysningTilHendelseOpplysning(opplysning)
        else -> HendelseOpplysning.UKJENT_OPPLYSNING
    }

context(RequestScope)
fun somHendelse(id: Long, identitetsnummer: Identitetsnummer, resultat: Either<Problem, GrunnlagForGodkjenning>): Hendelse =
    when (resultat) {
        is Either.Left -> Avvist(
            id = id,
            hendelseId = UUID.randomUUID(),
            identitetsnummer = identitetsnummer.verdi,
            metadata = hendelseMetadata(resultat.value.regel.id.beskrivelse),
            opplysninger = resultat.value.opplysning.map(::mapToHendelseOpplysning).toSet(),
            handling = path
        )

        is Either.Right -> Startet(
            id = id,
            hendelseId = UUID.randomUUID(),
            identitetsnummer = identitetsnummer.verdi,
            metadata = hendelseMetadata(resultat.value.regel.id.beskrivelse),
            opplysninger = resultat.value.opplysning.map(::mapToHendelseOpplysning).toSet()
        )
    }

context(RequestScope)
fun opplysningerHendelse(id: Long, opplysningerRequest: OpplysningerRequest): Hendelse = OpplysningerOmArbeidssoekerMottatt(
    hendelseId = UUID.randomUUID(),
    id = id,
    identitetsnummer = opplysningerRequest.identitetsnummer,
    opplysningerOmArbeidssoeker = OpplysningerOmArbeidssoeker(
        id = UUID.randomUUID(),
        metadata = Metadata(
            tidspunkt = Instant.now(),
            utfoertAv = brukerFraClaims(),
            kilde = ApplicationInfo.id,
            aarsak = "opplysning om arbeidssøker sendt inn"
        ),
        annet = opplysningerRequest.opplysningerOmArbeidssoeker.annet.toInternalApi(),
        helse = opplysningerRequest.opplysningerOmArbeidssoeker.helse.toInternalApi(),
        jobbsituasjon = opplysningerRequest.opplysningerOmArbeidssoeker.jobbsituasjon.toInternalApi(),
        utdanning = opplysningerRequest.opplysningerOmArbeidssoeker.utdanning.toInternalApi()
    )
)

context(RequestScope)
fun hendelseMetadata(aarsak: String): Metadata = Metadata(
    tidspunkt = Instant.now(),
    utfoertAv = brukerFraClaims(),
    kilde = ApplicationInfo.id,
    aarsak = aarsak
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
