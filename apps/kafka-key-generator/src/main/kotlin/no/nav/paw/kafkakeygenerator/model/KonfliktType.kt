package no.nav.paw.kafkakeygenerator.model

enum class KonfliktType {
    MERGE, SPLITT, SLETT
}

fun KonfliktType.asApi(): no.nav.paw.kafkakeygenerator.api.models.KonfliktType = when (this) {
    KonfliktType.MERGE -> no.nav.paw.kafkakeygenerator.api.models.KonfliktType.MERGE
    KonfliktType.SPLITT -> no.nav.paw.kafkakeygenerator.api.models.KonfliktType.SPLITT
    KonfliktType.SLETT -> no.nav.paw.kafkakeygenerator.api.models.KonfliktType.SLETT
}