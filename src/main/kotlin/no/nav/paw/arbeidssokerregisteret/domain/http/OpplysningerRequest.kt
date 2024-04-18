package no.nav.paw.arbeidssokerregisteret.domain.http

import no.nav.paw.arbeidssoekerregisteret.api.opplysningermottatt.models.OpplysningerOmArbeidssoeker
import no.nav.paw.arbeidssokerregisteret.domain.http.validering.validerJobbsituasjon
import no.nav.paw.arbeidssokerregisteret.domain.http.validering.validerUtdanning

fun validerOpplysninger(opplysningerOmArbeidssoeker: OpplysningerOmArbeidssoeker): ValidationResult =
    listOfNotNull(
        opplysningerOmArbeidssoeker.utdanning?.let(::validerUtdanning),
        validerJobbsituasjon(opplysningerOmArbeidssoeker.jobbsituasjon)
    ).filterIsInstance<ValidationErrorResult>()
        .firstOrNull() ?: ValidationResultOk
