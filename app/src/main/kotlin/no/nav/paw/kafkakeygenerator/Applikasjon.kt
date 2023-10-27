package no.nav.paw.kafkakeygenerator

import no.nav.paw.kafkakeygenerator.pdl.PdlIdentitesTjeneste
import no.nav.paw.kafkakeygenerator.vo.Identitetsnummer
import no.nav.paw.kafkakeygenerator.vo.TracePath

class Applikasjon(
    private val kafkaKeys: KafkaKeys,
    private val identitetsTjeneste: PdlIdentitesTjeneste
) {
    fun hello(): String = "Hello, World!"

    suspend fun hentEllerOpprett(callId: TracePath, identitet: Identitetsnummer): Long? {
        return arrayOf<suspend () -> Long?>(
            { kafkaKeys.hent(identitet) },
            { sjekkMotAliaser(callId, identitet) },
            { kafkaKeys.opprett(identitet) },
            //'Opprett' returnerer null ved konflikt, da skal 'hent' finne noe under.
            { kafkaKeys.hent(identitet) }
        ).firstNonNullOrNull()
    }

    private suspend fun sjekkMotAliaser(callId: TracePath, identitet: Identitetsnummer): Long? {
        val identiter = identitetsTjeneste.hentIdentiter(callId, identitet)
        return kafkaKeys.hent(identiter)
            .values
            .firstOrNull()
            ?.also { nøkkel ->
                kafkaKeys.lagre(identitet, nøkkel)
            }
    }
}

private suspend fun <A> Array<suspend () -> A?>.firstNonNullOrNull(): A? {
    for (element in this) {
        val result = element()
        if (result != null) {
            return result
        }
    }
    return null
}