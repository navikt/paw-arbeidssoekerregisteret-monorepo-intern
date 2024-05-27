package no.nav.paw.arbeidssokerregisteret.services

import io.ktor.http.*
import no.nav.paw.arbeidssokerregisteret.plugins.ArbeidssoekerException
import no.nav.paw.arbeidssokerregisteret.domain.Feilkode

class RemoteServiceException(
    description: String? = null,
    feilkode: Feilkode,
    causedBy: Throwable? = null
) : ArbeidssoekerException(HttpStatusCode.FailedDependency, description, feilkode, causedBy)
