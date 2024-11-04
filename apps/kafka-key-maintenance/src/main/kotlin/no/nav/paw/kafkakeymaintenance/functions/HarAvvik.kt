package no.nav.paw.kafkakeymaintenance.functions

import no.nav.paw.kafkakeymaintenance.vo.Data

fun harAvvik(data: Data): Boolean =
    data.alias
        .flatMap { it.kobliner }
        .map { it.arbeidsoekerId }
        .distinct().size > 1