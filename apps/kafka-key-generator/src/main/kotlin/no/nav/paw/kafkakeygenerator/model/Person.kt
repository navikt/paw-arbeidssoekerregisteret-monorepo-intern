package no.nav.paw.kafkakeygenerator.model

import no.nav.paw.identitet.internehendelser.vo.Identitet
import no.nav.person.pdl.aktor.v2.Aktor

data class Person(
    val identiteter: List<Identitet>
)

fun Aktor.asPerson(): Person = Person(
    identiteter = identifikatorer.map { it.asIdentitet() }
)
