package no.nav.paw.bqadapter

import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.joran.JoranConfigurator
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import org.slf4j.Logger

class LoggingConfigTest : FreeSpec({
    "logback.xml" - {
        "should configure stdout without a matching environment branch" {
            val context = LoggerContext()
            val configurator = JoranConfigurator().apply {
                this.context = context
            }

            try {
                val logbackConfig = checkNotNull(javaClass.classLoader.getResource("logback.xml"))
                configurator.doConfigure(logbackConfig)

                context
                    .getLogger(Logger.ROOT_LOGGER_NAME)
                    .getAppender("STDOUT_JSON")
                    .shouldNotBeNull()
            } finally {
                context.stop()
            }
        }
    }
})
