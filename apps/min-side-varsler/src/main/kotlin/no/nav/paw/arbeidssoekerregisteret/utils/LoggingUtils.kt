package no.nav.paw.arbeidssoekerregisteret.utils

import org.slf4j.spi.MDCAdapter

enum class MDCKey(val key: String) {
    ACTION("x_action"),
    EVENT_NAME("x_event_name")
}

fun MDCAdapter.action(action: Action) {
    put(MDCKey.ACTION.key, action.value)
}

fun MDCAdapter.eventName(eventName: String) {
    put(MDCKey.EVENT_NAME.key, eventName)
}

fun MDCAdapter.removeAction() {
    remove(MDCKey.ACTION.key)
}

fun MDCAdapter.removeEventName() {
    remove(MDCKey.EVENT_NAME.key)
}

fun MDCAdapter.removeAll() {
    removeAction()
    removeEventName()
}
