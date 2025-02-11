package no.nav.paw.bekreftelsetjeneste.metrics

import io.micrometer.core.instrument.Gauge.builder
import io.micrometer.core.instrument.Meter
import io.micrometer.core.instrument.Tag
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import java.util.concurrent.atomic.AtomicLong

class StateGauge(
    private val registry: PrometheusMeterRegistry
) {
    private val stateObjects: HashMap<WithMetricsInfo, Pair<Meter.Id, AtomicLong>> = HashMap()
    fun update(source: Sequence<WithMetricsInfo>) {
        val currentNumbers = source
            .fold(emptyMap<WithMetricsInfo, Long>()) { map, key ->
                map + (key to (map[key] ?: 0L) + 1L)
            }
        currentNumbers
            .forEach { (key, currentValue) ->
                val stateObject = stateObjects[key]
                if (stateObject != null) {
                    stateObject.second.set(currentValue)
                } else {
                    val newStateObject = AtomicLong(currentValue)
                    val regInfo = builder(key.name, newStateObject, AtomicLong::toDouble)
                        .tags(key.labels)
                        .register(registry)
                    stateObjects[key] = Pair(regInfo.id, newStateObject)
                }
            }
        stateObjects
            .filter { (_, value) -> value.second.get() == 0L }
            .toList()
            .forEach { (key, value) ->
                registry.remove(value.first)
                stateObjects.remove(key)
            }
        stateObjects.forEach { (key, value) ->
            if (!currentNumbers.containsKey(key)) {
                value.second.set(0L)
            }
        }
    }
}


data class WithMetricsInfo(
    val name: String,
    val labels: List<Tag>
)
