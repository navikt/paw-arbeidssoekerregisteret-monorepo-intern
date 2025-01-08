package no.nav.paw.security.authentication.plugin

import java.util.concurrent.atomic.AtomicInteger

sealed class PluginName(val pluginName: String) {
    private val _pluginInstance = AtomicInteger(0)
    val pluginInstance: Int get() = _pluginInstance.get()
    val pluginInstanceName get(): String = "$pluginName${_pluginInstance.updateAndGet { it + 1 }}"
}