package no.nav.paw.arbeidssoekerregisteret.repository

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotContainAnyOf
import no.nav.paw.arbeidssoekerregisteret.context.TestContext
import no.nav.paw.arbeidssoekerregisteret.model.BestillingerTable
import no.nav.paw.arbeidssoekerregisteret.model.BestilteVarslerTable
import no.nav.paw.arbeidssoekerregisteret.model.Order
import no.nav.paw.arbeidssoekerregisteret.model.Paging
import no.nav.paw.arbeidssoekerregisteret.test.TestData

class BestilteVarslerTableTest : FreeSpec({
    with(TestContext.buildWithPostgres()) {
        with(TestData) {
            beforeTest { initDatabase() }

            "Skal teste paging" {
                val bestillingRow = insertBestillingRow()
                val insertBestiltVarselRows = (1..30)
                    .map { insertBestiltVarselRow(bestillingId = bestillingRow.bestillingId) }
                BestillingerTable.insert(bestillingRow)
                insertBestiltVarselRows.forEach { BestilteVarslerTable.insert(it) }

                BestillingerTable.findAll() shouldHaveSize 1
                val bestiltVarselRows1 = BestilteVarslerTable.findAll()
                bestiltVarselRows1 shouldHaveSize 30

                var paging = Paging.of(0, 10, Order.DESC)
                val bestiltVarselRows2 = BestilteVarslerTable.findAll(paging)
                bestiltVarselRows2 shouldHaveSize 10
                bestiltVarselRows1.map { it.varselId } shouldContainAll bestiltVarselRows2.map { it.varselId }
                paging = paging.stepBySize()
                val bestiltVarselRows3 = BestilteVarslerTable.findAll(paging)
                bestiltVarselRows3 shouldHaveSize 10
                bestiltVarselRows1.map { it.varselId } shouldContainAll bestiltVarselRows3.map { it.varselId }
                bestiltVarselRows2.map { it.varselId } shouldNotContainAnyOf bestiltVarselRows3.map { it.varselId }
                paging = paging.stepBySize()
                val bestiltVarselRows4 = BestilteVarslerTable.findAll(paging)
                bestiltVarselRows4 shouldHaveSize 10
                bestiltVarselRows1.map { it.varselId } shouldContainAll bestiltVarselRows4.map { it.varselId }
                bestiltVarselRows2.map { it.varselId } shouldNotContainAnyOf bestiltVarselRows4.map { it.varselId }
                bestiltVarselRows3.map { it.varselId } shouldNotContainAnyOf bestiltVarselRows4.map { it.varselId }
                paging = paging.stepBySize()
                val bestiltVarselRows5 = BestilteVarslerTable.findAll(paging)
                bestiltVarselRows5 shouldHaveSize 0
            }
        }
    }
})