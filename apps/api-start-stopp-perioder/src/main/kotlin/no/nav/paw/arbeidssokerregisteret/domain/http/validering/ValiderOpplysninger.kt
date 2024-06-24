package no.nav.paw.arbeidssokerregisteret.domain.http.validering

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.paw.arbeidssoekerregisteret.api.opplysningermottatt.models.OpplysningerOmArbeidssoeker

fun validerOpplysninger(opplysningerOmArbeidssoeker: OpplysningerOmArbeidssoeker): Either<ValidationErrorResult, Unit> =
    listOfNotNull(
        opplysningerOmArbeidssoeker.utdanning?.let(::validerUtdanning),
        validerJobbsituasjon(opplysningerOmArbeidssoeker.jobbsituasjon)
    ).filterIsInstance<ValidationErrorResult>()
        .firstOrNull()?.left() ?: Unit.right()
