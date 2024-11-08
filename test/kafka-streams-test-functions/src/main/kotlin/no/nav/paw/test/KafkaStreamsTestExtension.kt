package no.nav.paw.test

import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.apache.kafka.streams.TestOutputTopic
import java.time.Duration

inline fun <reified T : Any, A> TestOutputTopic<*, in T>.assertEvent(function: (T) -> A): A =
    assertEvent<T>().let(function)

inline fun <reified T : Any> TestOutputTopic<*, in T>.assertEvent(): T {
    withClue("Expected event of type ${T::class.simpleName}, no event was found") {
        isEmpty shouldBe false
    }
    val event = readValue()
    return withClue("Expected event of type ${T::class}, found ${event?.let { it::class }}") {
        event.shouldBeInstanceOf<T>()
    }
}

fun TestOutputTopic<*, *>.assertNoMessage() {
    val message = if (isEmpty) null else readValue()
    withClue("Expected no message, but found: $message") {
        message shouldBe null
    }
}

val Int.seconds: Duration get() = Duration.ofSeconds(this.toLong())
val Int.days: Duration get() = Duration.ofDays(this.toLong())
val Long.seconds: Duration get() = Duration.ofSeconds(this)
val Long.days: Duration get() = Duration.ofDays(this)
val Long.minutes: Duration get() = Duration.ofMinutes(this)
val Int.minutes: Duration get() = Duration.ofMinutes(this.toLong())