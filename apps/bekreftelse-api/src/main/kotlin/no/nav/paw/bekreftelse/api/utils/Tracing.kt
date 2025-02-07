package no.nav.paw.bekreftelse.api.utils

import io.opentelemetry.api.trace.Span

sealed class TelemetryAction(val action: String)

data object SendAction : TelemetryAction("send")
data object ReceiveAction : TelemetryAction("receive")
data object CreateAction : TelemetryAction("create")
data object ReadAction : TelemetryAction("read")
data object UpdateAction : TelemetryAction("update")
data object DeleteAction : TelemetryAction("delete")
data object IgnoreAction : TelemetryAction("ignore")

fun Span.setEventAttribute(event: String): Span = setAttribute("paw.arbeidssoekerregisteret.event", event)
fun Span.setActionAttribute(action: TelemetryAction): Span =
    setAttribute("paw.arbeidssoekerregisteret.action", action.action)

fun Span.setCreateActionAttribute(): Span = setActionAttribute(CreateAction)
fun Span.setUpdateActionAttribute(): Span = setActionAttribute(UpdateAction)
fun Span.setDeleteActionAttribute(): Span = setActionAttribute(DeleteAction)
fun Span.setIgnoreActionAttribute(): Span = setActionAttribute(IgnoreAction)
