package no.nav.paw.bqadapter.bigquery

import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class BigqueryAdminTest : FreeSpec({
    "logBigQueryOperation" - {
        "should propagate errors" {
            val expectedError = IllegalStateException("BigQuery unavailable")

            val actualError = shouldThrowExactly<IllegalStateException> {
                logBigQueryOperation("test-operation") {
                    throw expectedError
                }
            }

            actualError shouldBe expectedError
        }
    }
})
