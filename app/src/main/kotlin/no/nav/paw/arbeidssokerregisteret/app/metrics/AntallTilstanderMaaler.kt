package no.nav.paw.arbeidssokerregisteret.app.metrics

import io.micrometer.core.instrument.Tag
import no.nav.paw.arbeidssokerregisteret.app.tilstand.Tilstand

data class AntallTilstanderMaaler(
    override val partition: Int
): WithMetricsInfo {
    override val name: String = Names.ANTALL_TILSTANDER
    override val labels: List<Tag>
        get() = emptyList()
}

fun antallTilstanderMaaler(tilstand: Tilstand): AntallTilstanderMaaler =
    AntallTilstanderMaaler(tilstand.recordScope.partition)