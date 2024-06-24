package no.nav.paw.arbeidssokerregisteret.services

import io.ktor.http.*
import no.nav.paw.arbeidssokerregisteret.plugins.ArbeidssoekerException
import no.nav.paw.arbeidssokerregisteret.plugins.InternFeilkode

class RemoteServiceException(
    description: String? = null,
    feilkode: InternFeilkode,
    causedBy: Throwable? = null
) : ArbeidssoekerException(HttpStatusCode.FailedDependency, description, feilkode, causedBy)
