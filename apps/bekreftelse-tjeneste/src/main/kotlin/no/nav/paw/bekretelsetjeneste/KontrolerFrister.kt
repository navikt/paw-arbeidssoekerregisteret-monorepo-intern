package no.nav.paw.bekretelsetjeneste

import no.nav.paw.config.kafka.streams.Punctuation
import no.nav.paw.bekretelsetjeneste.tilstand.InternTilstand
import no.nav.paw.bekretelsetjeneste.tilstand.RapporteringsKonfigurasjon
import org.apache.kafka.streams.KeyValue
import java.time.Instant


context(ApplicationConfiguration, RapporteringsKonfigurasjon)
fun kontrollerFrister(): Punctuation<Long, Action> = TODO()

operator fun <K, V> KeyValue<K, V>.component1(): K = key
operator fun <K, V> KeyValue<K, V>.component2(): V = value

context(Instant, RapporteringsKonfigurasjon)
fun rapporteringSkalTilgjengeliggjoeres(tilstand: InternTilstand): Boolean {
    TODO()
}