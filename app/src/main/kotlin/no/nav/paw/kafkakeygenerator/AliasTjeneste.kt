package no.nav.paw.kafkakeygenerator

fun interface AliasTjeneste {
    fun hentAlias(identitet: String): List<String>
}