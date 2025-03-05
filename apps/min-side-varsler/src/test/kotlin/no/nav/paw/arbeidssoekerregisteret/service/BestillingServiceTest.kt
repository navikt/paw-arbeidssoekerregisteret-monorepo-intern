package no.nav.paw.arbeidssoekerregisteret.service

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.paw.arbeidssoekerregisteret.api.models.BestillingStatus
import no.nav.paw.arbeidssoekerregisteret.context.TestContext
import no.nav.paw.arbeidssoekerregisteret.model.BestiltVarselStatus
import no.nav.paw.arbeidssoekerregisteret.model.VarselEventName
import no.nav.paw.arbeidssoekerregisteret.model.VarselKilde
import no.nav.paw.arbeidssoekerregisteret.model.VarselStatus
import no.nav.paw.arbeidssoekerregisteret.model.VarselType
import no.nav.paw.arbeidssoekerregisteret.test.TestData

class BestillingServiceTest : FreeSpec({
    with(TestContext.buildWithPostgres()) {
        with(TestData) {
            beforeTest { initDatabase() }

            "Skal teste flyt for å bestille varsler" {
                val bestiller = "TEST_BRUKER"

                periodeRepository.findAll() shouldHaveSize 0
                varselRepository.findAll() shouldHaveSize 0
                bestillingRepository.findAll() shouldHaveSize 0
                bestiltVarselRepository.findAll() shouldHaveSize 0

                val insertPeriode1 = insertPeriodeRow()
                val insertPeriode2 = insertPeriodeRow()
                val insertPeriode3 = insertPeriodeRow()
                val insertPeriode4 = insertPeriodeRow()
                val insertPeriode5 = insertPeriodeRow()
                val insertPeriode6 = insertPeriodeRow()
                val insertPeriode7 = insertPeriodeRow()
                val updatePeriode6 = updatePeriodeRow(insertPeriode6.periodeId, insertPeriode6.identitetsnummer)
                val updatePeriode7 = updatePeriodeRow(insertPeriode7.periodeId, insertPeriode7.identitetsnummer)
                periodeRepository.insert(insertPeriode1)
                periodeRepository.insert(insertPeriode2)
                periodeRepository.insert(insertPeriode3)
                periodeRepository.insert(insertPeriode4)
                periodeRepository.insert(insertPeriode5)
                periodeRepository.insert(insertPeriode6)
                periodeRepository.insert(insertPeriode7)
                periodeRepository.update(updatePeriode6)
                periodeRepository.update(updatePeriode7)

                periodeRepository.findAll() shouldHaveSize 7
                varselRepository.findAll() shouldHaveSize 0
                bestillingRepository.findAll() shouldHaveSize 0
                bestiltVarselRepository.findAll() shouldHaveSize 0

                val bestillingResponse1 = bestillingService.opprettBestilling(bestiller)
                bestillingResponse1.bestiller shouldBe bestiller
                bestillingResponse1.status shouldBe BestillingStatus.VENTER
                bestillingResponse1.varslerTotalt shouldBe 0
                bestillingResponse1.varslerSendt shouldBe 0
                bestillingResponse1.varslerFeilet shouldBe 0

                periodeRepository.findAll() shouldHaveSize 7
                varselRepository.findAll() shouldHaveSize 0
                bestillingRepository.findAll() shouldHaveSize 1
                bestiltVarselRepository.findAll() shouldHaveSize 0

                val bestillingResponse2 = bestillingService.bekreftBestilling(bestillingResponse1.bestillingId)
                bestillingResponse2.bestillingId shouldBe bestillingResponse1.bestillingId
                bestillingResponse2.bestiller shouldBe bestiller
                bestillingResponse2.status shouldBe BestillingStatus.BEKREFTET
                bestillingResponse2.varslerTotalt shouldBe 5
                bestillingResponse2.varslerSendt shouldBe 0
                bestillingResponse2.varslerFeilet shouldBe 0

                periodeRepository.findAll() shouldHaveSize 7
                varselRepository.findAll() shouldHaveSize 0
                bestillingRepository.findAll() shouldHaveSize 1
                val bestilteVarsler1 = bestiltVarselRepository.findAll()
                bestilteVarsler1 shouldHaveSize 5
                bestilteVarsler1.forEach { it.status shouldBe BestiltVarselStatus.VENTER }

                bestillingService.prosesserBestillinger()
                val bestillingResponse3 = bestillingService.hentBestilling(bestillingResponse1.bestillingId)
                bestillingResponse3.bestillingId shouldBe bestillingResponse1.bestillingId
                bestillingResponse3.bestiller shouldBe bestiller
                bestillingResponse3.status shouldBe BestillingStatus.SENDT
                bestillingResponse3.varslerTotalt shouldBe 5
                bestillingResponse3.varslerSendt shouldBe 5
                bestillingResponse3.varslerFeilet shouldBe 0

                periodeRepository.findAll() shouldHaveSize 7
                val varsler1 = varselRepository.findAll()
                varsler1 shouldHaveSize 5
                varsler1.forEach { it.varselType shouldBe VarselType.BESKJED }
                varsler1.forEach { it.varselKilde shouldBe VarselKilde.MANUELL_VARSLING }
                varsler1.forEach { it.varselStatus shouldBe VarselStatus.UKJENT }
                varsler1.forEach { it.hendelseName shouldBe VarselEventName.UKJENT }
                bestillingRepository.findAll() shouldHaveSize 1
                val bestilteVarsler2 = bestiltVarselRepository.findAll()
                bestilteVarsler2 shouldHaveSize 5
                bestilteVarsler2.forEach { it.status shouldBe BestiltVarselStatus.SENDT }
            }
        }
    }
})