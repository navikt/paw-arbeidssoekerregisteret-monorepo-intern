package no.nav.paw.bqadapter.bigquery

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldNotBe

class MaterializedViewGeneratorTest: FreeSpec({
    "MaterializedViewGenerator" - {
        "should create materialized views from resources" {
            val views = viewsFromResource(views_path)
            views shouldNotBe null
            views.shouldNotBeEmpty()
        }
    }
})