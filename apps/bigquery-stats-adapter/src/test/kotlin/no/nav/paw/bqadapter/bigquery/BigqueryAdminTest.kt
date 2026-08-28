package no.nav.paw.bqadapter.bigquery

import com.google.api.services.bigquery.model.MaterializedViewDefinition
import com.google.api.services.bigquery.model.Table
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

    "materializedViewSettingsDiffer" - {
        "should detect changed refresh settings" {
            val existingTable = materializedViewTable(
                enableRefresh = true,
                refreshIntervalMs = 60_000L,
                maxStaleness = "0-0 0 2:0:0"
            )
            val desiredTable = materializedViewTable(
                enableRefresh = true,
                refreshIntervalMs = 120_000L,
                maxStaleness = "0-0 0 3:0:0"
            )

            materializedViewSettingsDiffer(existingTable, desiredTable) shouldBe true
        }

        "should accept matching refresh settings" {
            val existingTable = materializedViewTable()
            val desiredTable = materializedViewTable()

            materializedViewSettingsDiffer(existingTable, desiredTable) shouldBe false
        }
    }

    "materializedViewSettingsPatch" - {
        "should only contain mutable refresh settings" {
            val patch = materializedViewSettingsPatch(materializedViewTable())

            patch.materializedView.enableRefresh shouldBe true
            patch.materializedView.refreshIntervalMs shouldBe 60_000L
            patch.materializedView.query shouldBe null
            patch.materializedView.allowNonIncrementalDefinition shouldBe null
            patch.maxStaleness shouldBe "0-0 0 2:0:0"
        }
    }
})

private fun materializedViewTable(
    enableRefresh: Boolean = true,
    refreshIntervalMs: Long = 60_000L,
    maxStaleness: String = "0-0 0 2:0:0"
): Table = Table()
    .setMaterializedView(
        MaterializedViewDefinition()
            .setQuery("SELECT 1")
            .setEnableRefresh(enableRefresh)
            .setRefreshIntervalMs(refreshIntervalMs)
            .setAllowNonIncrementalDefinition(true)
    )
    .setMaxStaleness(maxStaleness)
