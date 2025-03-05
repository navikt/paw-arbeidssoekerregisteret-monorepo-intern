package no.nav.paw.bekreftelsetjeneste.testcases

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import no.nav.paw.arbeidssoekerregisteret.testdata.ValueWithKafkaKeyData
import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import no.nav.paw.bekreftelse.internehendelser.BekreftelseHendelse
import no.nav.paw.bekreftelse.melding.v1.Bekreftelse
import no.nav.paw.bekreftelse.paavegneav.v1.PaaVegneAv
import no.nav.paw.bekreftelsetjeneste.testutils.prettyPrint
import java.time.Instant
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

inline fun <reified A : BekreftelseHendelse> FreeSpec.forventer(
    kilde: MutableList<Pair<Instant, BekreftelseHendelse>>,
    input: MutableList<Pair<Instant, ValueWithKafkaKeyData<*>>> = mutableListOf(),
    fra: Instant,
    til: Instant,
    crossinline asserts: (List<A>) -> Unit = {}
) {
    val kandidater = kilde.toList().filter { (tidspunkt, _) -> tidspunkt == fra || (tidspunkt.isAfter(fra) && tidspunkt.isBefore(til)) }
    val resultat = kandidater.singleOrNull { it.second is A }
    val inputHendelser =
        input.filter { it.first.isBefore(fra) || it.first == fra }
    input.removeAll(inputHendelser)
    inputHendelser.forEach { "..INN: ${it.prettyPrint()}" {} }
    "UT: ${A::class.simpleName} i tidsrommet ${fra.prettyPrint} til ${til.prettyPrint}" {
        val filtered = kandidater.mapNotNull { (it.second as? A)?.let { typed -> it.first to typed } }
        withClue(
            "[UTC intervall ($fra-$til)]\nKilde: ${kilde}\nkandidater: ${kandidater}\nTidspunkt for ${A::class.simpleName}:\n\t${
            filtered.joinToString("\n\t", "\n\t") { it.first.prettyPrint }
        }") {
            resultat.shouldNotBeNull()
            asserts(filtered.map { it.second })
            kilde.remove(resultat)
        }
    }

}

fun Pair<Instant, ValueWithKafkaKeyData<*>>.prettyPrint() = when (val value = second.value) {
    is Periode -> "${first.prettyPrint}: periode startet"
    is PaaVegneAv -> "${first.prettyPrint}: på veiene av: ${value.handling::class.simpleName}(${value.bekreftelsesloesning.name})"
    is Bekreftelse -> "${first.prettyPrint}: bekreftelse levert, ønsker å fortsette: ${if (value.svar.vilFortsetteSomArbeidssoeker) "Ja" else "Nei"}"
    null -> "${first.prettyPrint}: null"
    else -> "${first.prettyPrint}: ${value::class.simpleName}"
}