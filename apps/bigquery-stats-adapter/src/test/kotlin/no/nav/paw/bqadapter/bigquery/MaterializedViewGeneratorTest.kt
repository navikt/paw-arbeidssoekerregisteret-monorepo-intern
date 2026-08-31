package no.nav.paw.bqadapter.bigquery

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe

class MaterializedViewGeneratorTest : FreeSpec({
    "MaterializedViewGenerator" - {
        "should create materialized views from resources" {
            val views = viewsFromResource(views_path).orEmpty()
            views.shouldNotBeEmpty()
            views.map { it.name } shouldContain
                "avsluttede_perioder_sammenhengende_har_jobbet_per_uke"
        }

        "should configure automatic refresh" {
            val table = createMaterializedViewDefinition(
                DatasetName("dataset"),
                View("view", Sql("SELECT 1"))
            )

            table.materializedView.enableRefresh shouldBe true
            table.materializedView.refreshIntervalMs shouldBe materializedViewsRefreshInterval.toMillis()
            table.maxStaleness shouldBe "0-0 0 ${materializedViewsMaxStaleness.toHours()}:0:0"
        }
    }
})