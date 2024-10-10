package no.nav.paw.bekreftelsetjeneste.ansvar

import java.time.Duration
import java.util.*

data class Ansvar(
    val periodeId: UUID,
    val ansvarlige: List<Ansvarlig>
)

fun ansvar(
    periodeId: UUID,
    ansvarlig: Ansvarlig? = null
): Ansvar = Ansvar(
    periodeId = periodeId,
    ansvarlige = listOfNotNull(ansvarlig)
)

operator fun Ansvar.plus(ansvarlig: Ansvarlig): Ansvar =
    copy(ansvarlige = ansvarlige
        .filterNot { it.namespace == ansvarlig.namespace} + ansvarlig
    )

operator fun Ansvar.minus(namespace: String): Ansvar? =
    ansvarlige.filterNot { it.namespace == namespace }
        .takeIf(List<Ansvarlig>::isNotEmpty)
        ?.let { copy(ansvarlige = it) }



data class Ansvarlig(
    val namespace: String,
    val id: String,
    val intervall: Duration,
    val gracePeriode: Duration
)