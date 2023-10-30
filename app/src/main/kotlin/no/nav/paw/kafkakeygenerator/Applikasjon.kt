package no.nav.paw.kafkakeygenerator

import no.nav.paw.kafkakeygenerator.pdl.PdlIdentitesTjeneste
import no.nav.paw.kafkakeygenerator.vo.CallId
import no.nav.paw.kafkakeygenerator.vo.Identitetsnummer

class Applikasjon(
    private val kafkaKeys: KafkaKeys,
    private val identitetsTjeneste: PdlIdentitesTjeneste
) {
    suspend fun hentEllerOpprett(callId: CallId, identitet: Identitetsnummer): Long? {
        return arrayOf<suspend () -> Long?>(
            { kafkaKeys.hent(identitet) },
            { sjekkMotAliaser(callId, identitet) },
            { kafkaKeys.opprett(identitet) },
            //'Opprett' returnerer null ved konflikt, da skal 'hent' finne noe under.
            { kafkaKeys.hent(identitet) }
        ).firstNonNullOrNull()
    }

    private suspend fun sjekkMotAliaser(callId: CallId, identitet: Identitetsnummer): Long? {
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
