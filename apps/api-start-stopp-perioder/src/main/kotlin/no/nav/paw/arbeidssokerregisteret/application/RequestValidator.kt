package no.nav.paw.arbeidssokerregisteret.application

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.partially1
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.paw.arbeidssokerregisteret.RequestScope
import no.nav.paw.arbeidssokerregisteret.application.authfaktka.systemTilgangFakta
import no.nav.paw.arbeidssokerregisteret.application.authfaktka.navAnsattTilgangFakta
import no.nav.paw.arbeidssokerregisteret.application.authfaktka.sluttbrukerTilgangFakta
import no.nav.paw.arbeidssokerregisteret.application.opplysninger.DomeneOpplysning
import no.nav.paw.arbeidssokerregisteret.application.opplysninger.Opplysning
import no.nav.paw.arbeidssokerregisteret.application.opplysninger.adreseOpplysning
import no.nav.paw.arbeidssokerregisteret.application.opplysninger.alderOpplysning
import no.nav.paw.arbeidssokerregisteret.application.opplysninger.euEoesStatsborgerOpplysning
import no.nav.paw.arbeidssokerregisteret.application.opplysninger.forenkletFregOpplysning
import no.nav.paw.arbeidssokerregisteret.application.opplysninger.gbrStatsborgerOpplysning
import no.nav.paw.arbeidssokerregisteret.application.opplysninger.norskStatsborgerOpplysning
import no.nav.paw.arbeidssokerregisteret.application.opplysninger.oppholdstillatelseOpplysning
import no.nav.paw.arbeidssokerregisteret.application.opplysninger.plus
import no.nav.paw.arbeidssokerregisteret.application.opplysninger.utflyttingOpplysning
import no.nav.paw.arbeidssokerregisteret.application.regler.ValideringsRegler
import no.nav.paw.arbeidssokerregisteret.domain.Identitetsnummer
import no.nav.paw.arbeidssokerregisteret.services.AutorisasjonService
import no.nav.paw.arbeidssokerregisteret.services.PersonInfoService
import no.nav.paw.arbeidssokerregisteret.utils.logger
import no.nav.paw.collections.PawNonEmptyList
import no.nav.paw.pdl.graphql.generated.hentperson.Person

class RequestValidator(
    autorisasjonService: AutorisasjonService,
    private val personInfoService: PersonInfoService,
    private val regler: Regler,
    private val registry: PrometheusMeterRegistry
) {

    private val sjekkOmNavAnsattHarTilgang = ::navAnsattTilgangFakta.partially1(autorisasjonService)
    private val sjekkOmSystemHarTilgang = ::systemTilgangFakta.partially1(autorisasjonService)

    @WithSpan
    suspend fun validerRequest(
        requestScope: RequestScope,
        identitetsnummer: Identitetsnummer,
        erForhaandsGodkjentAvVeileder: Boolean = false,
        feilretting: Feilretting?
    ): Either<PawNonEmptyList<Problem>, GrunnlagForGodkjenning> {
        val valideringsFakta = requestScope.sluttbrukerTilgangFakta(identitetsnummer) +
                sjekkOmNavAnsattHarTilgang(requestScope, identitetsnummer) +
                sjekkOmSystemHarTilgang(requestScope) +
                listOfNotNull(
                    if (erForhaandsGodkjentAvVeileder) DomeneOpplysning.ErForhaandsgodkjent else null,
                    feilretting?.let { DomeneOpplysning.ErFeilretting },
                    (feilretting as? UgyldigFeilretting)?.grunn?.let { DomeneOpplysning.UgyldigFeilretting }
                )

        return ValideringsRegler.evaluer(valideringsFakta)
    }

    @WithSpan
    suspend fun validerStartAvPeriodeOenske(
        requestScope: RequestScope,
        identitetsnummer: Identitetsnummer,
        erForhaandsGodkjentAvVeileder: Boolean = false,
        feilretting: Feilretting?
    ): Either<PawNonEmptyList<Problem>, GrunnlagForGodkjenning> =
        validerRequest(
            requestScope = requestScope,
            identitetsnummer = identitetsnummer,
            erForhaandsGodkjentAvVeileder = erForhaandsGodkjentAvVeileder,
            feilretting = feilretting
        )
            .flatMap { valideringsFakta ->
                val person = personInfoService.hentPersonInfo(requestScope, identitetsnummer.verdi)
                val opplysning = person?.let { genererPersonFakta(it) } ?: setOf(DomeneOpplysning.PersonIkkeFunnet)
                if (person != null) {
                    try {
                        val oppholdIndfo = person.opphold.firstOrNull()?.let { opphold ->
                            statsOppholdstilatelse(
                                fra = opphold.oppholdFra,
                                til = opphold.oppholdTil,
                                type = opphold.type.name
                            )
                        }
                        registry.oppholdstillatelseStats(oppholdIndfo, opplysning)
                    } catch (ex: Exception) {
                        logger.warn("Feil under stats generering", ex)
                    }
                }
                regler.evaluer(
                    opplysning + valideringsFakta.opplysning
                )
            }

    fun genererPersonFakta(person: Person): Set<Opplysning> {
        require(person.foedselsdato.size <= 1) { "Personen har flere fødselsdatoer enn forventet" }
        require(person.bostedsadresse.size <= 1) { "Personen har flere bostedsadresser enn forventet" }
        require(person.opphold.size <= 1) { "Personen har flere opphold enn forventet" }

        return alderOpplysning(person.foedselsdato.firstOrNull()) +
                adreseOpplysning(person.bostedsadresse.firstOrNull()) +
                euEoesStatsborgerOpplysning(person.statsborgerskap) +
                gbrStatsborgerOpplysning(person.statsborgerskap) +
                norskStatsborgerOpplysning(person.statsborgerskap) +
                forenkletFregOpplysning(person.folkeregisterpersonstatus) +
                oppholdstillatelseOpplysning(person.opphold.firstOrNull()) +
                utflyttingOpplysning(person.innflyttingTilNorge, person.utflyttingFraNorge)
    }
}




