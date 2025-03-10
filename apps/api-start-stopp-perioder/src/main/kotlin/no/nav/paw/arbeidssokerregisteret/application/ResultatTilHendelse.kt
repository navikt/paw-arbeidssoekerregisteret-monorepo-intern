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
import no.nav.paw.arbeidssokerregisteret.domain.m2mToken
import no.nav.paw.arbeidssokerregisteret.domain.navAnsatt
import no.nav.paw.arbeidssokerregisteret.domain.sluttbruker
import no.nav.paw.arbeidssokerregisteret.intern.v1.Avsluttet
import no.nav.paw.arbeidssokerregisteret.intern.v1.Avvist
import no.nav.paw.arbeidssokerregisteret.intern.v1.AvvistStoppAvPeriode
import no.nav.paw.arbeidssokerregisteret.intern.v1.Hendelse
import no.nav.paw.arbeidssokerregisteret.intern.v1.OpplysningerOmArbeidssoekerMottatt
import no.nav.paw.arbeidssokerregisteret.intern.v1.Startet
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Bruker
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.BrukerType
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Metadata
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.OpplysningerOmArbeidssoeker
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.TidspunktFraKilde
import no.nav.paw.arbeidssokerregisteret.utils.AzureACR
import no.nav.paw.collections.PawNonEmptyList
import java.time.Instant
import java.util.*
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Opplysning as HendelseOpplysning


fun stoppResultatSomHendelse(
    requestScope: RequestScope,
    id: Long,
    identitetsnummer: Identitetsnummer,
    resultat: Either<PawNonEmptyList<Problem>, GrunnlagForGodkjenning>,
    feilretting: Feilretting?
): Hendelse =
    when (resultat) {
        is Either.Left -> AvvistStoppAvPeriode(
            id = id,
            hendelseId = UUID.randomUUID(),
            identitetsnummer = identitetsnummer.verdi,
            metadata = hendelseMetadata(
                requestScope = requestScope,
                aarsak = resultat.value.map { it.regel.id.beskrivelse }.toList().joinToString(". "),
                tidspunktFraKilde = feilretting.tidspunktFraKilde
            ),
            opplysninger = resultat.value.first.opplysninger.map(::mapToHendelseOpplysning).toSet()
        )

        is Either.Right -> Avsluttet(
            id = id,
            hendelseId = UUID.randomUUID(),
            identitetsnummer = identitetsnummer.verdi,
            metadata = hendelseMetadata(
                requestScope = requestScope,
                aarsak = feilretting.aarsak ?: "Stopp av periode",
                tidspunktFraKilde = feilretting.tidspunktFraKilde
            ),
            opplysninger = resultat.value.opplysning.map(::mapToHendelseOpplysning).toSet(),
        )
    }

fun mapToHendelseOpplysning(opplysning: Opplysning): HendelseOpplysning =
    when (opplysning) {
        is AuthOpplysning -> authOpplysningTilHendelseOpplysning(opplysning)
        is DomeneOpplysning -> domeneOpplysningTilHendelseOpplysning(opplysning)
        else -> HendelseOpplysning.UKJENT_OPPLYSNING
    }

fun somHendelse(
    requestScope: RequestScope,
    id: Long,
    identitetsnummer: Identitetsnummer,
    resultat: Either<PawNonEmptyList<Problem>, GrunnlagForGodkjenning>,
    feilretting: Feilretting?
): Hendelse =
    when (resultat) {
        is Either.Left -> Avvist(
            id = id,
            hendelseId = UUID.randomUUID(),
            identitetsnummer = identitetsnummer.verdi,
            metadata = hendelseMetadata(
                requestScope = requestScope,
                aarsak = resultat.value.map { it.regel.id.beskrivelse }.toList().joinToString(". "),
                tidspunktFraKilde = feilretting?.tidspunktFraKilde
            ),
            opplysninger = resultat.value.first.opplysninger.map(::mapToHendelseOpplysning).toSet(),
            handling = requestScope.path
        )

        is Either.Right -> Startet(
            id = id,
            hendelseId = UUID.randomUUID(),
            identitetsnummer = identitetsnummer.verdi,
            metadata = hendelseMetadata(
                requestScope = requestScope,
                aarsak = resultat.value.regel.id.beskrivelse,
                tidspunktFraKilde = feilretting?.tidspunktFraKilde
            ),
            opplysninger = resultat.value.opplysning.map(::mapToHendelseOpplysning).toSet()
        )
    }

fun opplysningerHendelse(
    requestScope: RequestScope,
    id: Long,
    opplysningerRequest: OpplysningerRequest
): Hendelse = OpplysningerOmArbeidssoekerMottatt(
    hendelseId = UUID.randomUUID(),
    id = id,
    identitetsnummer = opplysningerRequest.identitetsnummer,
    opplysningerOmArbeidssoeker = OpplysningerOmArbeidssoeker(
        id = UUID.randomUUID(),
        metadata = Metadata(
            tidspunkt = Instant.now(),
            utfoertAv = requestScope.brukerFraClaims(),
            kilde = ApplicationInfo.id,
            aarsak = "opplysning om arbeidssøker sendt inn"
        ),
        annet = opplysningerRequest.opplysningerOmArbeidssoeker.annet.toInternalApi(),
        helse = opplysningerRequest.opplysningerOmArbeidssoeker.helse.toInternalApi(),
        jobbsituasjon = opplysningerRequest.opplysningerOmArbeidssoeker.jobbsituasjon.toInternalApi(),
        utdanning = opplysningerRequest.opplysningerOmArbeidssoeker.utdanning.toInternalApi()
    )
)

fun hendelseMetadata(
    requestScope: RequestScope,
    aarsak: String,
    tidspunktFraKilde: TidspunktFraKilde? = null
): Metadata = Metadata(
    tidspunkt = Instant.now(),
    utfoertAv = requestScope.brukerFraClaims(),
    kilde = ApplicationInfo.id,
    aarsak = aarsak,
    tidspunktFraKilde = tidspunktFraKilde
)

fun RequestScope.brukerFraClaims(): Bruker {
    return sluttbruker(claims)?.let {
        Bruker(
            type = BrukerType.SLUTTBRUKER,
            id = it.identitetsnummer.verdi,
            sikkerhetsnivaa = it.sikkerhetsnivaa
        )
    } ?: navAnsatt(claims)?.let {
        Bruker(
            type = BrukerType.VEILEDER,
            id = it.ident,
            sikkerhetsnivaa = "${claims.issuer}:${claims[AzureACR] ?: "undefined"}"
        )
    } ?: m2mToken(claims)?.let {
        Bruker(
            type = BrukerType.SYSTEM,
            id = it.tjeneste,
            sikkerhetsnivaa = "${claims.issuer}:${claims[AzureACR] ?: "undefined"}"
        )
    } ?: throw IllegalStateException("Kunne ikke finne bruker i claims")
}
