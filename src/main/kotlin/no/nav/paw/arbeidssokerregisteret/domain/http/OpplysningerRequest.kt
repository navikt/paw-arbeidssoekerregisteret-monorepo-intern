package no.nav.paw.arbeidssokerregisteret.domain.http

import no.nav.paw.arbeidssokerregisteret.domain.Identitetsnummer
import no.nav.paw.arbeidssokerregisteret.domain.http.validering.validerJobbsituasjon
import no.nav.paw.arbeidssokerregisteret.domain.http.validering.validerUtdanning
import no.nav.paw.arbeidssokerregisteret.domain.tilIdentitetsnummer
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Annet
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Helse
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Jobbsituasjon
import no.nav.paw.arbeidssokerregisteret.intern.v1.vo.Utdanning

data class OpplysningerRequest(
    val identitetsnummer: String,
    val opplysningerOmArbeidssoeker: OpplysningerOmArbeidssoeker
) {
    fun getId(): Identitetsnummer = identitetsnummer.tilIdentitetsnummer()
}

data class OpplysningerOmArbeidssoeker(
    val utdanning: Utdanning?,
    val helse: Helse?,
    val jobbsituasjon: Jobbsituasjon,
    val annet: Annet?
)

fun validerOpplysninger(opplysningerOmArbeidssoeker: OpplysningerOmArbeidssoeker): ValidationResult =
    listOfNotNull(
        opplysningerOmArbeidssoeker.utdanning?.let(::validerUtdanning),
        validerJobbsituasjon(opplysningerOmArbeidssoeker.jobbsituasjon)
    ).filterIsInstance<ValidationErrorResult>()
        .firstOrNull() ?: ValidationResultOk


