package no.nav.paw.kafkakeymaintenance

import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.paw.kafkakeymaintenance.kafka.TransactionContext
import org.jetbrains.exposed.sql.Transaction
import org.slf4j.Logger
import java.time.Duration
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

data class ApplicationContext(
    val periodeConsumerVersion: Int,
    val aktorConsumerVersion: Int,
    val logger: Logger,
    val meterRegistry: PrometheusMeterRegistry,
    val shutdownCalled: AtomicBoolean = AtomicBoolean(false),
) {
    private val messages: BlockingQueue<Message> = LinkedBlockingQueue()

    fun eventOccured(message: Message) {
        this.messages.add(message)
    }

    fun pollMessage(timeout: Duration = Duration.ofSeconds(1)): Message =
        messages.poll(timeout.toMillis(), TimeUnit.MILLISECONDS) ?: Noop

}

val ApplicationContext.periodeTxContext: Transaction.() -> TransactionContext get() = {
    TransactionContext(periodeConsumerVersion, this)
}

val ApplicationContext.aktorTxContext: Transaction.() -> TransactionContext get() = {
    TransactionContext(aktorConsumerVersion, this)
}

sealed interface Message
data object Noop: Message
data class ErrorOccurred(val throwable: Throwable): Message
data class ShutdownSignal(val source: String): Message
