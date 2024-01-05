package no.nav.paw.arbeidssokerregisteret.domain.http

import no.nav.paw.arbeidssokerregisteret.plugins.ErrorCode

data class Error(
    val message: String,
    val errorCode: ErrorCode
)
