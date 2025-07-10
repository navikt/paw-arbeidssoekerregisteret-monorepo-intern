package no.nav.paw.kafkakeygenerator.mergedetector

import no.nav.paw.kafkakeygenerator.vo.Failure
import no.nav.paw.kafkakeygenerator.vo.FailureCode
import no.nav.paw.kafkakeygenerator.vo.GenericFailure

fun failure(code: FailureCode): Failure =
    GenericFailure(
        system = when (code) {
            FailureCode.PDL_NOT_FOUND -> "pdl"
            FailureCode.DB_NOT_FOUND -> "database"
            else -> "other"
        },
        code = code
    )