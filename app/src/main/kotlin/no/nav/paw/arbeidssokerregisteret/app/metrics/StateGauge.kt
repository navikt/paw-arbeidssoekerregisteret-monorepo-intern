package no.nav.paw.arbeidssokerregisteret.app.metrics

import io.micrometer.core.instrument.Tag
import io.micrometer.prometheus.PrometheusMeterRegistry
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.atomic.AtomicLong

class StateGauge(
    private val registry: PrometheusMeterRegistry
) {
    private val stateObjects: ConcurrentMap<WithMetricsInfo, AtomicLong> = ConcurrentHashMap()
    fun update(source: Sequence<WithMetricsInfo>) {
        val candidatesForRemoval = stateObjects.filter { (_, value) ->
            value.get() == 0L
        }.map { (key, _) -> key }
            .toList()
        val currentNumbers = source
            .fold(emptyMap<WithMetricsInfo, Long>()) { map, key ->
                map + (key to (map[key] ?: 0L) + 1L)
            }
        currentNumbers
            .forEach { (key, currentValue) ->
                val stateObject = stateObjects[key]
                if (stateObject != null) {
                    stateObject.set(currentValue)
                } else {
                    val newStateObject = AtomicLong(currentValue)
                    registry.gauge(key.name, key.labels, newStateObject)
                }
            }
        stateObjects.forEach { (key, value) ->
            if (!currentNumbers.containsKey(key)) {
                value.set(0L)
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
