package no.nav.paw.kafkakeygenerator.mergedetector

import no.nav.paw.kafkakeygenerator.Failure
import no.nav.paw.kafkakeygenerator.FailureCode

fun failure(code: FailureCode) : Failure =
    Failure(
        system = when (code) {
            FailureCode.PDL_NOT_FOUND -> "pdl"
            FailureCode.DB_NOT_FOUND -> "database"
            else -> "other"
        },
        code = code
    )