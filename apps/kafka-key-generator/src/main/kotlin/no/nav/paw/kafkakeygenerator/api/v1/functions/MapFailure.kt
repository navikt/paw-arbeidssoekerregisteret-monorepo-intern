package no.nav.paw.kafkakeygenerator.api.v1.functions

import io.ktor.http.HttpStatusCode
import no.nav.paw.kafkakeygenerator.api.v1.FailureResponseV1
import no.nav.paw.kafkakeygenerator.api.v1.Feilkode
import no.nav.paw.kafkakeygenerator.model.Failure
import no.nav.paw.kafkakeygenerator.model.FailureCode

fun mapFailure(result: Failure) =
    when (result.code()) {
        FailureCode.PDL_NOT_FOUND ->
            HttpStatusCode.NotFound to
                    FailureResponseV1("Ukjent ident", Feilkode.UKJENT_IDENT)

        FailureCode.DB_NOT_FOUND -> HttpStatusCode.NotFound to
                FailureResponseV1("Ikke funnet i arbeidssøkerregisteret", Feilkode.UKJENT_REGISTERET)

        FailureCode.EXTERNAL_TECHINCAL_ERROR ->
            HttpStatusCode.InternalServerError to FailureResponseV1(
                "Teknisk feil ved kommunikasjon med eksternt system",
                Feilkode.TEKNISK_FEIL
            )


        FailureCode.INTERNAL_TECHINCAL_ERROR ->
            HttpStatusCode.InternalServerError to
                    FailureResponseV1("Intern feil", Feilkode.TEKNISK_FEIL)

        FailureCode.CONFLICT ->
            HttpStatusCode.InternalServerError to
                    FailureResponseV1("Intern feil", Feilkode.TEKNISK_FEIL)
    }