package no.nav.paw.arbeidssokerregisteret.app.metrics

import io.micrometer.core.instrument.Tag
import io.micrometer.prometheus.PrometheusMeterRegistry
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.atomic.AtomicLong

class StateGauge<A : WithMetricsInfo>(
    private val registry: PrometheusMeterRegistry
) {
    private val stateObjects: ConcurrentMap<A, AtomicLong> = ConcurrentHashMap()
    fun <E> update(source: Iterable<E>, mapper: (E) -> A) {
        val candidatesForRemoval = stateObjects.filter { (_, value) ->
            value.get() == 0L
        }.map { (key, _) -> key }
            .toList()
        val currentNumbers = source.map(mapper)
            .fold(emptyMap<A, Long>()) { map, key ->
                map + (key to (map[key] ?: 0L) + 1L)
            }
        stateObjects.keys
            .forEach { key ->
                val stateObject = stateObjects[key]
                val currentNumber = currentNumbers[key] ?: 0L
                if (stateObject != null) {
                    stateObject.set(currentNumber)
                } else {
                    val newStateObject = AtomicLong(currentNumber)
                    registry.gauge(key.name, key.labels, newStateObject)
                }
            }
        candidatesForRemoval.forEach { key ->
            if (stateObjects[key]?.get() == 0L) {
                stateObjects.remove(key)
            }
        }
    }
}


    interface WithMetricsInfo {
        val partition: Int?
        val name: String
        val labels: List<Tag>
    }
