package no.nav.paw.arbeidssoekerregisteret.service

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.paw.arbeidssoekerregisteret.api.models.BestillingStatus
import no.nav.paw.arbeidssoekerregisteret.context.TestContext
import no.nav.paw.arbeidssoekerregisteret.model.BestillingerTable
import no.nav.paw.arbeidssoekerregisteret.model.BestiltVarselStatus
import no.nav.paw.arbeidssoekerregisteret.model.BestilteVarslerTable
import no.nav.paw.arbeidssoekerregisteret.model.PerioderTable
import no.nav.paw.arbeidssoekerregisteret.model.VarselEventName
import no.nav.paw.arbeidssoekerregisteret.model.VarselKilde
import no.nav.paw.arbeidssoekerregisteret.model.VarselStatus
import no.nav.paw.arbeidssoekerregisteret.model.VarselType
import no.nav.paw.arbeidssoekerregisteret.model.VarslerTable
import no.nav.paw.arbeidssoekerregisteret.test.TestData

class BestillingServiceTest : FreeSpec({
    with(TestContext.buildWithPostgres()) {
        with(TestData) {
            beforeTest { initDatabase() }

            "Skal teste flyt for å bestille varsler" {
                val bestiller = "TEST_BRUKER"

                PerioderTable.findAll() shouldHaveSize 0
                VarslerTable.findAll() shouldHaveSize 0
                BestillingerTable.findAll() shouldHaveSize 0
                BestilteVarslerTable.findAll() shouldHaveSize 0

                val insertPeriodeRows = (1..42).map { insertPeriodeRow() }
                val insertPeriode43 = insertPeriodeRow()
                val insertPeriode44 = insertPeriodeRow()
                val updatePeriode43 = updatePeriodeRow(insertPeriode43.periodeId, insertPeriode43.identitetsnummer)
                val updatePeriode44 = updatePeriodeRow(insertPeriode44.periodeId, insertPeriode44.identitetsnummer)
                insertPeriodeRows.forEach { PerioderTable.insert(it) }
                PerioderTable.insert(insertPeriode43)
                PerioderTable.insert(insertPeriode44)
                PerioderTable.update(updatePeriode43)
                PerioderTable.update(updatePeriode44)

                PerioderTable.findAll() shouldHaveSize 44
                VarslerTable.findAll() shouldHaveSize 0
                BestillingerTable.findAll() shouldHaveSize 0
                BestilteVarslerTable.findAll() shouldHaveSize 0

                val bestillingResponse1 = bestillingService.opprettBestilling(bestiller)
                bestillingResponse1.bestiller shouldBe bestiller
                bestillingResponse1.status shouldBe BestillingStatus.VENTER
                bestillingResponse1.varslerTotalt shouldBe 0
                bestillingResponse1.varslerSendt shouldBe 0
                bestillingResponse1.varslerFeilet shouldBe 0

                PerioderTable.findAll() shouldHaveSize 44
                VarslerTable.findAll() shouldHaveSize 0
                BestillingerTable.findAll() shouldHaveSize 1
                BestilteVarslerTable.findAll() shouldHaveSize 0

                val bestillingResponse2 = bestillingService.bekreftBestilling(bestillingResponse1.bestillingId)
                bestillingResponse2.bestillingId shouldBe bestillingResponse1.bestillingId
                bestillingResponse2.bestiller shouldBe bestiller
                bestillingResponse2.status shouldBe BestillingStatus.BEKREFTET
                bestillingResponse2.varslerTotalt shouldBe 42
                bestillingResponse2.varslerSendt shouldBe 0
                bestillingResponse2.varslerFeilet shouldBe 0

                PerioderTable.findAll() shouldHaveSize 44
                VarslerTable.findAll() shouldHaveSize 0
                BestillingerTable.findAll() shouldHaveSize 1
                val bestilteVarsler1 = BestilteVarslerTable.findAll()
                bestilteVarsler1 shouldHaveSize 42
                bestilteVarsler1.forEach { it.status shouldBe BestiltVarselStatus.VENTER }

                bestillingService.prosesserBestillinger()
                val bestillingResponse3 = bestillingService.hentBestilling(bestillingResponse1.bestillingId)
                bestillingResponse3.bestillingId shouldBe bestillingResponse1.bestillingId
                bestillingResponse3.bestiller shouldBe bestiller
                bestillingResponse3.status shouldBe BestillingStatus.SENDT
                bestillingResponse3.varslerTotalt shouldBe 42
                bestillingResponse3.varslerSendt shouldBe 42
                bestillingResponse3.varslerFeilet shouldBe 0

                PerioderTable.findAll() shouldHaveSize 44
                val varsler1 = VarslerTable.findAll()
                varsler1 shouldHaveSize 42
                varsler1.forEach {
                    it.varselType shouldBe VarselType.BESKJED
                    it.varselKilde shouldBe VarselKilde.MANUELL_VARSLING
                    it.varselStatus shouldBe VarselStatus.UKJENT
                    it.hendelseName shouldBe VarselEventName.UKJENT
                }
                BestillingerTable.findAll() shouldHaveSize 1
                val bestilteVarsler2 = BestilteVarslerTable.findAll()
                bestilteVarsler2 shouldHaveSize 42
                bestilteVarsler2.forEach {
                    it.bestillingId shouldBe bestillingResponse1.bestillingId
                    it.status shouldBe BestiltVarselStatus.SENDT
                }
            }
        }
    }
})