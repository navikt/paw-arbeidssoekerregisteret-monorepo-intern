package no.nav.paw.kafkakeygenerator.service

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import no.nav.paw.identitet.internehendelser.IdentiteterEndretHendelse
import no.nav.paw.identitet.internehendelser.vo.IdentitetType
import no.nav.paw.kafkakeygenerator.context.TestContext
import no.nav.paw.kafkakeygenerator.model.IdentitetHendelseStatus
import no.nav.paw.kafkakeygenerator.model.IdentitetKonfliktStatus
import no.nav.paw.kafkakeygenerator.model.IdentitetStatus
import no.nav.paw.kafkakeygenerator.model.asIdentitet
import no.nav.paw.kafkakeygenerator.test.TestData
import no.nav.paw.kafkakeygenerator.vo.Identitetsnummer
import java.time.Duration
import java.time.Instant

class IdentitetKonfliktServiceTest : FreeSpec({
    with(TestContext.buildWithPostgres()) {

        beforeSpec {
            setUp()
        }

        afterSpec {
            tearDown()
        }

        "Skal håndtere konflikt uten perioder" {
            // GIVEN
            val aktorId = TestData.aktorId1
            val npId = TestData.npId1
            val dnr = TestData.dnr1
            val fnr1 = TestData.fnr1_1
            val fnr2 = TestData.fnr1_2
            val arbeidssoekerId1 = kafkaKeysRepository.opprett(Identitetsnummer(dnr))
                .fold(onLeft = { null }, onRight = { it })!!.value
            val arbeidssoekerId2 = kafkaKeysRepository.opprett(Identitetsnummer(fnr1))
                .fold(onLeft = { null }, onRight = { it })!!.value
            val arbeidssoekerId3 = kafkaKeysRepository.opprett(Identitetsnummer(fnr2))
                .fold(onLeft = { null }, onRight = { it })!!.value
            identitetRepository.insert(
                arbeidssoekerId = arbeidssoekerId1,
                aktorId = aktorId,
                identitet = aktorId,
                type = IdentitetType.AKTORID,
                gjeldende = true,
                status = IdentitetStatus.KONFLIKT,
                sourceTimestamp = Instant.now()
            )
            identitetRepository.insert(
                arbeidssoekerId = arbeidssoekerId1,
                aktorId = aktorId,
                identitet = npId,
                type = IdentitetType.NPID,
                gjeldende = true,
                status = IdentitetStatus.KONFLIKT,
                sourceTimestamp = Instant.now()
            )
            identitetRepository.insert(
                arbeidssoekerId = arbeidssoekerId1,
                aktorId = aktorId,
                identitet = dnr,
                type = IdentitetType.FOLKEREGISTERIDENT,
                gjeldende = false,
                status = IdentitetStatus.KONFLIKT,
                sourceTimestamp = Instant.now()
            )
            identitetRepository.insert(
                arbeidssoekerId = arbeidssoekerId2,
                aktorId = aktorId,
                identitet = fnr1,
                type = IdentitetType.FOLKEREGISTERIDENT,
                gjeldende = false,
                status = IdentitetStatus.KONFLIKT,
                sourceTimestamp = Instant.now()
            )
            identitetRepository.insert(
                arbeidssoekerId = arbeidssoekerId3,
                aktorId = aktorId,
                identitet = fnr2,
                type = IdentitetType.FOLKEREGISTERIDENT,
                gjeldende = true,
                status = IdentitetStatus.KONFLIKT,
                sourceTimestamp = Instant.now()
            )
            identitetKonfliktRepository.insert(
                aktorId = aktorId,
                status = IdentitetKonfliktStatus.VENTER
            )

            // WHEN
            identitetKonfliktService.handleVentendeIdentitetKonflikter()

            // THEN
            val konfliktRows = identitetKonfliktRepository.findByAktorId(aktorId)
            konfliktRows shouldHaveSize 1

            val konfliktRow1 = konfliktRows[0]
            konfliktRow1.aktorId shouldBe aktorId
            konfliktRow1.status shouldBe IdentitetKonfliktStatus.FULLFOERT

            val identitetRows = identitetRepository.findByAktorId(aktorId)
            identitetRows shouldHaveSize 5

            // 1
            val identitetRow1 = identitetRows[0]
            identitetRow1.aktorId shouldBe aktorId
            identitetRow1.arbeidssoekerId shouldBe arbeidssoekerId3
            identitetRow1.identitet shouldBe aktorId
            identitetRow1.type shouldBe IdentitetType.AKTORID
            identitetRow1.gjeldende shouldBe true
            identitetRow1.status shouldBe IdentitetStatus.AKTIV

            // 2
            val identitetRow2 = identitetRows[1]
            identitetRow2.aktorId shouldBe aktorId
            identitetRow2.arbeidssoekerId shouldBe arbeidssoekerId3
            identitetRow2.identitet shouldBe npId
            identitetRow2.type shouldBe IdentitetType.NPID
            identitetRow2.gjeldende shouldBe true
            identitetRow2.status shouldBe IdentitetStatus.AKTIV

            // 3
            val identitetRow3 = identitetRows[2]
            identitetRow3.aktorId shouldBe aktorId
            identitetRow3.arbeidssoekerId shouldBe arbeidssoekerId3
            identitetRow3.identitet shouldBe dnr
            identitetRow3.type shouldBe IdentitetType.FOLKEREGISTERIDENT
            identitetRow3.gjeldende shouldBe false
            identitetRow3.status shouldBe IdentitetStatus.AKTIV

            // 4
            val identitetRow4 = identitetRows[3]
            identitetRow4.aktorId shouldBe aktorId
            identitetRow4.arbeidssoekerId shouldBe arbeidssoekerId3
            identitetRow4.identitet shouldBe fnr1
            identitetRow4.type shouldBe IdentitetType.FOLKEREGISTERIDENT
            identitetRow4.gjeldende shouldBe false
            identitetRow4.status shouldBe IdentitetStatus.AKTIV

            // 5
            val identitetRow5 = identitetRows[4]
            identitetRow5.aktorId shouldBe aktorId
            identitetRow5.arbeidssoekerId shouldBe arbeidssoekerId3
            identitetRow5.identitet shouldBe fnr2
            identitetRow5.type shouldBe IdentitetType.FOLKEREGISTERIDENT
            identitetRow5.gjeldende shouldBe true
            identitetRow5.status shouldBe IdentitetStatus.AKTIV

            val hendelseRows = identitetHendelseRepository.findByAktorId(aktorId)
            hendelseRows shouldHaveSize 3

            // 1
            val hendelseRow1 = hendelseRows[0]
            hendelseRow1.arbeidssoekerId shouldBe arbeidssoekerId3
            hendelseRow1.aktorId shouldBe aktorId
            hendelseRow1.status shouldBe IdentitetHendelseStatus.VENTER
            val hendelse1 = deserializer.deserializeFromString(hendelseRow1.data)
            val endretHendelse1 = hendelse1.shouldBeTypeOf<IdentiteterEndretHendelse>()
            endretHendelse1.identiteter shouldHaveSize 8
            identitetRow1.asIdentitet() shouldBe endretHendelse1.identiteter[0]
            identitetRow2.asIdentitet() shouldBe endretHendelse1.identiteter[1]
            identitetRow3.asIdentitet() shouldBe endretHendelse1.identiteter[2]
            identitetRow4.asIdentitet() shouldBe endretHendelse1.identiteter[3]
            identitetRow5.asIdentitet() shouldBe endretHendelse1.identiteter[4]
            arbeidssoekerId3.asIdentitet(gjeldende = true) shouldBe endretHendelse1.identiteter[5]
            arbeidssoekerId1.asIdentitet(gjeldende = false) shouldBe endretHendelse1.identiteter[6]
            arbeidssoekerId2.asIdentitet(gjeldende = false) shouldBe endretHendelse1.identiteter[7]

            // 2
            val hendelseRow2 = hendelseRows[1]
            hendelseRow2.arbeidssoekerId shouldBe arbeidssoekerId1
            hendelseRow2.aktorId shouldBe aktorId
            hendelseRow2.status shouldBe IdentitetHendelseStatus.VENTER
            val hendelse2 = deserializer.deserializeFromString(hendelseRow2.data)
            val endretHendelse2 = hendelse2.shouldBeTypeOf<IdentiteterEndretHendelse>()
            endretHendelse2.identiteter shouldHaveSize 8
            identitetRow1.asIdentitet() shouldBe endretHendelse2.identiteter[0]
            identitetRow2.asIdentitet() shouldBe endretHendelse2.identiteter[1]
            identitetRow3.asIdentitet() shouldBe endretHendelse2.identiteter[2]
            identitetRow4.asIdentitet() shouldBe endretHendelse2.identiteter[3]
            identitetRow5.asIdentitet() shouldBe endretHendelse2.identiteter[4]
            arbeidssoekerId3.asIdentitet(gjeldende = true) shouldBe endretHendelse2.identiteter[5]
            arbeidssoekerId1.asIdentitet(gjeldende = false) shouldBe endretHendelse2.identiteter[6]
            arbeidssoekerId2.asIdentitet(gjeldende = false) shouldBe endretHendelse2.identiteter[7]

            // 3
            val hendelseRow3 = hendelseRows[2]
            hendelseRow3.arbeidssoekerId shouldBe arbeidssoekerId2
            hendelseRow3.aktorId shouldBe aktorId
            hendelseRow3.status shouldBe IdentitetHendelseStatus.VENTER
            val hendelse3 = deserializer.deserializeFromString(hendelseRow3.data)
            val endretHendelse3 = hendelse3.shouldBeTypeOf<IdentiteterEndretHendelse>()
            endretHendelse3.identiteter shouldHaveSize 8
            identitetRow1.asIdentitet() shouldBe endretHendelse3.identiteter[0]
            identitetRow2.asIdentitet() shouldBe endretHendelse3.identiteter[1]
            identitetRow3.asIdentitet() shouldBe endretHendelse3.identiteter[2]
            identitetRow4.asIdentitet() shouldBe endretHendelse3.identiteter[3]
            identitetRow5.asIdentitet() shouldBe endretHendelse3.identiteter[4]
            arbeidssoekerId3.asIdentitet(gjeldende = true) shouldBe endretHendelse3.identiteter[5]
            arbeidssoekerId1.asIdentitet(gjeldende = false) shouldBe endretHendelse3.identiteter[6]
            arbeidssoekerId2.asIdentitet(gjeldende = false) shouldBe endretHendelse3.identiteter[7]
        }

        "Skal håndtere konflikt med kun avsluttede perioder" {
            // GIVEN
            val aktorId = TestData.aktorId2
            val npId = TestData.npId2
            val dnr = TestData.dnr2
            val fnr1 = TestData.fnr2_1
            val fnr2 = TestData.fnr2_2
            val periodeId1 = TestData.periodeId2_1
            val periodeId2 = TestData.periodeId2_2
            val periodeId3 = TestData.periodeId2_3
            val arbeidssoekerId1 = kafkaKeysRepository.opprett(Identitetsnummer(dnr))
                .fold(onLeft = { null }, onRight = { it })!!.value
            val arbeidssoekerId2 = kafkaKeysRepository.opprett(Identitetsnummer(fnr1))
                .fold(onLeft = { null }, onRight = { it })!!.value
            val arbeidssoekerId3 = kafkaKeysRepository.opprett(Identitetsnummer(fnr2))
                .fold(onLeft = { null }, onRight = { it })!!.value
            identitetRepository.insert(
                arbeidssoekerId = arbeidssoekerId1,
                aktorId = aktorId,
                identitet = aktorId,
                type = IdentitetType.AKTORID,
                gjeldende = true,
                status = IdentitetStatus.KONFLIKT,
                sourceTimestamp = Instant.now().minus(Duration.ofDays(90))
            )
            identitetRepository.insert(
                arbeidssoekerId = arbeidssoekerId1,
                aktorId = aktorId,
                identitet = npId,
                type = IdentitetType.NPID,
                gjeldende = true,
                status = IdentitetStatus.KONFLIKT,
                sourceTimestamp = Instant.now().minus(Duration.ofDays(90))
            )
            identitetRepository.insert(
                arbeidssoekerId = arbeidssoekerId1,
                aktorId = aktorId,
                identitet = dnr,
                type = IdentitetType.FOLKEREGISTERIDENT,
                gjeldende = false,
                status = IdentitetStatus.KONFLIKT,
                sourceTimestamp = Instant.now().minus(Duration.ofDays(90))
            )
            identitetRepository.insert(
                arbeidssoekerId = arbeidssoekerId2,
                aktorId = aktorId,
                identitet = fnr1,
                type = IdentitetType.FOLKEREGISTERIDENT,
                gjeldende = false,
                status = IdentitetStatus.KONFLIKT,
                sourceTimestamp = Instant.now().minus(Duration.ofDays(60))
            )
            identitetRepository.insert(
                arbeidssoekerId = arbeidssoekerId3,
                aktorId = aktorId,
                identitet = fnr2,
                type = IdentitetType.FOLKEREGISTERIDENT,
                gjeldende = true,
                status = IdentitetStatus.KONFLIKT,
                sourceTimestamp = Instant.now().minus(Duration.ofDays(30))
            )
            identitetKonfliktRepository.insert(
                aktorId = aktorId,
                status = IdentitetKonfliktStatus.VENTER
            )
            periodeRepository.insert(
                periodeId = periodeId1,
                identitet = fnr1,
                startetTimestamp = Instant.now().minus(Duration.ofDays(180)),
                avsluttetTimestamp = Instant.now().minus(Duration.ofDays(150)),
                sourceTimestamp = Instant.now()
            )
            periodeRepository.insert(
                periodeId = periodeId2,
                identitet = fnr2,
                startetTimestamp = Instant.now().minus(Duration.ofDays(120)),
                avsluttetTimestamp = Instant.now().minus(Duration.ofDays(90)),
                sourceTimestamp = Instant.now()
            )
            periodeRepository.insert(
                periodeId = periodeId3,
                identitet = dnr,
                startetTimestamp = Instant.now().minus(Duration.ofDays(60)),
                avsluttetTimestamp = Instant.now().minus(Duration.ofDays(30)),
                sourceTimestamp = Instant.now()
            )

            // WHEN
            identitetKonfliktService.handleVentendeIdentitetKonflikter()

            // THEN
            val konfliktRows = identitetKonfliktRepository.findByAktorId(aktorId)
            konfliktRows shouldHaveSize 1
            val konfliktRow1 = konfliktRows[0]
            konfliktRow1.aktorId shouldBe aktorId
            konfliktRow1.status shouldBe IdentitetKonfliktStatus.FULLFOERT
            val identitetRows = identitetRepository.findByAktorId(aktorId)
            identitetRows shouldHaveSize 5

            // 1
            val identitetRow1 = identitetRows[0]
            identitetRow1.aktorId shouldBe aktorId
            identitetRow1.arbeidssoekerId shouldBe arbeidssoekerId1
            identitetRow1.identitet shouldBe aktorId
            identitetRow1.type shouldBe IdentitetType.AKTORID
            identitetRow1.gjeldende shouldBe true
            identitetRow1.status shouldBe IdentitetStatus.AKTIV

            // 2
            val identitetRow2 = identitetRows[1]
            identitetRow2.aktorId shouldBe aktorId
            identitetRow2.arbeidssoekerId shouldBe arbeidssoekerId1
            identitetRow2.identitet shouldBe npId
            identitetRow2.type shouldBe IdentitetType.NPID
            identitetRow2.gjeldende shouldBe true
            identitetRow2.status shouldBe IdentitetStatus.AKTIV

            // 3
            val identitetRow3 = identitetRows[2]
            identitetRow3.aktorId shouldBe aktorId
            identitetRow3.arbeidssoekerId shouldBe arbeidssoekerId1
            identitetRow3.identitet shouldBe dnr
            identitetRow3.type shouldBe IdentitetType.FOLKEREGISTERIDENT
            identitetRow3.gjeldende shouldBe false
            identitetRow3.status shouldBe IdentitetStatus.AKTIV

            // 4
            val identitetRow4 = identitetRows[3]
            identitetRow4.aktorId shouldBe aktorId
            identitetRow4.arbeidssoekerId shouldBe arbeidssoekerId1
            identitetRow4.identitet shouldBe fnr1
            identitetRow4.type shouldBe IdentitetType.FOLKEREGISTERIDENT
            identitetRow4.gjeldende shouldBe false
            identitetRow4.status shouldBe IdentitetStatus.AKTIV

            // 5
            val identitetRow5 = identitetRows[4]
            identitetRow5.aktorId shouldBe aktorId
            identitetRow5.arbeidssoekerId shouldBe arbeidssoekerId1
            identitetRow5.identitet shouldBe fnr2
            identitetRow5.type shouldBe IdentitetType.FOLKEREGISTERIDENT
            identitetRow5.gjeldende shouldBe true
            identitetRow5.status shouldBe IdentitetStatus.AKTIV

            val hendelseRows = identitetHendelseRepository.findByAktorId(aktorId)
            hendelseRows shouldHaveSize 3

            // 1
            val hendelseRow1 = hendelseRows[0]
            hendelseRow1.arbeidssoekerId shouldBe arbeidssoekerId1
            hendelseRow1.aktorId shouldBe aktorId
            hendelseRow1.status shouldBe IdentitetHendelseStatus.VENTER
            val hendelse1 = deserializer.deserializeFromString(hendelseRow1.data)
            val endretHendelse1 = hendelse1.shouldBeTypeOf<IdentiteterEndretHendelse>()
            endretHendelse1.identiteter shouldHaveSize 8
            identitetRow1.asIdentitet() shouldBe endretHendelse1.identiteter[0]
            identitetRow2.asIdentitet() shouldBe endretHendelse1.identiteter[1]
            identitetRow3.asIdentitet() shouldBe endretHendelse1.identiteter[2]
            identitetRow4.asIdentitet() shouldBe endretHendelse1.identiteter[3]
            identitetRow5.asIdentitet() shouldBe endretHendelse1.identiteter[4]
            arbeidssoekerId1.asIdentitet(gjeldende = true) shouldBe endretHendelse1.identiteter[5]
            arbeidssoekerId2.asIdentitet(gjeldende = false) shouldBe endretHendelse1.identiteter[6]
            arbeidssoekerId3.asIdentitet(gjeldende = false) shouldBe endretHendelse1.identiteter[7]

            // 2
            val hendelseRow2 = hendelseRows[1]
            hendelseRow2.arbeidssoekerId shouldBe arbeidssoekerId2
            hendelseRow2.aktorId shouldBe aktorId
            hendelseRow2.status shouldBe IdentitetHendelseStatus.VENTER
            val hendelse2 = deserializer.deserializeFromString(hendelseRow2.data)
            val endretHendelse2 = hendelse2.shouldBeTypeOf<IdentiteterEndretHendelse>()
            endretHendelse2.identiteter shouldHaveSize 8
            identitetRow1.asIdentitet() shouldBe endretHendelse2.identiteter[0]
            identitetRow2.asIdentitet() shouldBe endretHendelse2.identiteter[1]
            identitetRow3.asIdentitet() shouldBe endretHendelse2.identiteter[2]
            identitetRow4.asIdentitet() shouldBe endretHendelse2.identiteter[3]
            identitetRow5.asIdentitet() shouldBe endretHendelse2.identiteter[4]
            arbeidssoekerId1.asIdentitet(gjeldende = true) shouldBe endretHendelse2.identiteter[5]
            arbeidssoekerId2.asIdentitet(gjeldende = false) shouldBe endretHendelse2.identiteter[6]
            arbeidssoekerId3.asIdentitet(gjeldende = false) shouldBe endretHendelse2.identiteter[7]

            // 3
            val hendelseRow3 = hendelseRows[2]
            hendelseRow3.arbeidssoekerId shouldBe arbeidssoekerId3
            hendelseRow3.aktorId shouldBe aktorId
            hendelseRow3.status shouldBe IdentitetHendelseStatus.VENTER
            val hendelse3 = deserializer.deserializeFromString(hendelseRow3.data)
            val endretHendelse3 = hendelse3.shouldBeTypeOf<IdentiteterEndretHendelse>()
            endretHendelse3.identiteter shouldHaveSize 8
            identitetRow1.asIdentitet() shouldBe endretHendelse3.identiteter[0]
            identitetRow2.asIdentitet() shouldBe endretHendelse3.identiteter[1]
            identitetRow3.asIdentitet() shouldBe endretHendelse3.identiteter[2]
            identitetRow4.asIdentitet() shouldBe endretHendelse3.identiteter[3]
            identitetRow5.asIdentitet() shouldBe endretHendelse3.identiteter[4]
            arbeidssoekerId1.asIdentitet(gjeldende = true) shouldBe endretHendelse3.identiteter[5]
            arbeidssoekerId2.asIdentitet(gjeldende = false) shouldBe endretHendelse3.identiteter[6]
            arbeidssoekerId3.asIdentitet(gjeldende = false) shouldBe endretHendelse3.identiteter[7]
        }

        "Skal håndtere konflikt med én aktiv periode" {
            // GIVEN
            val aktorId = TestData.aktorId3
            val npId = TestData.npId3
            val dnr = TestData.dnr3
            val fnr1 = TestData.fnr3_1
            val fnr2 = TestData.fnr3_2
            val periodeId1 = TestData.periodeId3_1
            val periodeId2 = TestData.periodeId3_2
            val periodeId3 = TestData.periodeId3_3
            val arbeidssoekerId1 = kafkaKeysRepository.opprett(Identitetsnummer(dnr))
                .fold(onLeft = { null }, onRight = { it })!!.value
            val arbeidssoekerId2 = kafkaKeysRepository.opprett(Identitetsnummer(fnr1))
                .fold(onLeft = { null }, onRight = { it })!!.value
            val arbeidssoekerId3 = kafkaKeysRepository.opprett(Identitetsnummer(fnr2))
                .fold(onLeft = { null }, onRight = { it })!!.value
            identitetRepository.insert(
                arbeidssoekerId = arbeidssoekerId1,
                aktorId = aktorId,
                identitet = aktorId,
                type = IdentitetType.AKTORID,
                gjeldende = true,
                status = IdentitetStatus.KONFLIKT,
                sourceTimestamp = Instant.now().minus(Duration.ofDays(90))
            )
            identitetRepository.insert(
                arbeidssoekerId = arbeidssoekerId1,
                aktorId = aktorId,
                identitet = npId,
                type = IdentitetType.NPID,
                gjeldende = true,
                status = IdentitetStatus.KONFLIKT,
                sourceTimestamp = Instant.now().minus(Duration.ofDays(90))
            )
            identitetRepository.insert(
                arbeidssoekerId = arbeidssoekerId1,
                aktorId = aktorId,
                identitet = dnr,
                type = IdentitetType.FOLKEREGISTERIDENT,
                gjeldende = false,
                status = IdentitetStatus.KONFLIKT,
                sourceTimestamp = Instant.now().minus(Duration.ofDays(90))
            )
            identitetRepository.insert(
                arbeidssoekerId = arbeidssoekerId2,
                aktorId = aktorId,
                identitet = fnr1,
                type = IdentitetType.FOLKEREGISTERIDENT,
                gjeldende = false,
                status = IdentitetStatus.KONFLIKT,
                sourceTimestamp = Instant.now().minus(Duration.ofDays(60))
            )
            identitetRepository.insert(
                arbeidssoekerId = arbeidssoekerId3,
                aktorId = aktorId,
                identitet = fnr2,
                type = IdentitetType.FOLKEREGISTERIDENT,
                gjeldende = true,
                status = IdentitetStatus.KONFLIKT,
                sourceTimestamp = Instant.now().minus(Duration.ofDays(30))
            )
            identitetKonfliktRepository.insert(
                aktorId = aktorId,
                status = IdentitetKonfliktStatus.VENTER
            )
            periodeRepository.insert(
                periodeId = periodeId1,
                identitet = dnr,
                startetTimestamp = Instant.now().minus(Duration.ofDays(180)),
                avsluttetTimestamp = Instant.now().minus(Duration.ofDays(150)),
                sourceTimestamp = Instant.now()
            )
            periodeRepository.insert(
                periodeId = periodeId2,
                identitet = fnr1,
                startetTimestamp = Instant.now().minus(Duration.ofDays(120)),
                sourceTimestamp = Instant.now()
            )
            periodeRepository.insert(
                periodeId = periodeId3,
                identitet = fnr2,
                startetTimestamp = Instant.now().minus(Duration.ofDays(60)),
                avsluttetTimestamp = Instant.now().minus(Duration.ofDays(30)),
                sourceTimestamp = Instant.now()
            )

            // WHEN
            identitetKonfliktService.handleVentendeIdentitetKonflikter()

            // THEN
            val konfliktRows = identitetKonfliktRepository.findByAktorId(aktorId)
            konfliktRows shouldHaveSize 1
            val konflikt = konfliktRows.first()
            konflikt.aktorId shouldBe aktorId
            konflikt.status shouldBe IdentitetKonfliktStatus.FULLFOERT
            val identitetRows = identitetRepository.findByAktorId(aktorId)
            identitetRows shouldHaveSize 5

            // 1
            val identitetRow1 = identitetRows[0]
            identitetRow1.aktorId shouldBe aktorId
            identitetRow1.arbeidssoekerId shouldBe arbeidssoekerId2
            identitetRow1.identitet shouldBe aktorId
            identitetRow1.type shouldBe IdentitetType.AKTORID
            identitetRow1.gjeldende shouldBe true
            identitetRow1.status shouldBe IdentitetStatus.AKTIV

            // 2
            val identitetRow2 = identitetRows[1]
            identitetRow2.aktorId shouldBe aktorId
            identitetRow2.arbeidssoekerId shouldBe arbeidssoekerId2
            identitetRow2.identitet shouldBe npId
            identitetRow2.type shouldBe IdentitetType.NPID
            identitetRow2.gjeldende shouldBe true
            identitetRow2.status shouldBe IdentitetStatus.AKTIV

            // 3
            val identitetRow3 = identitetRows[2]
            identitetRow3.aktorId shouldBe aktorId
            identitetRow3.arbeidssoekerId shouldBe arbeidssoekerId2
            identitetRow3.identitet shouldBe dnr
            identitetRow3.type shouldBe IdentitetType.FOLKEREGISTERIDENT
            identitetRow3.gjeldende shouldBe false
            identitetRow3.status shouldBe IdentitetStatus.AKTIV

            // 4
            val identitetRow4 = identitetRows[3]
            identitetRow4.aktorId shouldBe aktorId
            identitetRow4.arbeidssoekerId shouldBe arbeidssoekerId2
            identitetRow4.identitet shouldBe fnr1
            identitetRow4.type shouldBe IdentitetType.FOLKEREGISTERIDENT
            identitetRow4.gjeldende shouldBe false
            identitetRow4.status shouldBe IdentitetStatus.AKTIV

            // 5
            val identitetRow5 = identitetRows[4]
            identitetRow5.aktorId shouldBe aktorId
            identitetRow5.arbeidssoekerId shouldBe arbeidssoekerId2
            identitetRow5.identitet shouldBe fnr2
            identitetRow5.type shouldBe IdentitetType.FOLKEREGISTERIDENT
            identitetRow5.gjeldende shouldBe true
            identitetRow5.status shouldBe IdentitetStatus.AKTIV

            val hendelseRows = identitetHendelseRepository.findByAktorId(aktorId)
            hendelseRows shouldHaveSize 3

            // 1
            val hendelseRow1 = hendelseRows[0]
            hendelseRow1.arbeidssoekerId shouldBe arbeidssoekerId2
            hendelseRow1.aktorId shouldBe aktorId
            hendelseRow1.status shouldBe IdentitetHendelseStatus.VENTER
            val hendelse1 = deserializer.deserializeFromString(hendelseRow1.data)
            val endretHendelse1 = hendelse1.shouldBeTypeOf<IdentiteterEndretHendelse>()
            endretHendelse1.identiteter shouldHaveSize 8
            identitetRow1.asIdentitet() shouldBe endretHendelse1.identiteter[0]
            identitetRow2.asIdentitet() shouldBe endretHendelse1.identiteter[1]
            identitetRow3.asIdentitet() shouldBe endretHendelse1.identiteter[2]
            identitetRow4.asIdentitet() shouldBe endretHendelse1.identiteter[3]
            identitetRow5.asIdentitet() shouldBe endretHendelse1.identiteter[4]
            arbeidssoekerId2.asIdentitet(gjeldende = true) shouldBe endretHendelse1.identiteter[5]
            arbeidssoekerId1.asIdentitet(gjeldende = false) shouldBe endretHendelse1.identiteter[6]
            arbeidssoekerId3.asIdentitet(gjeldende = false) shouldBe endretHendelse1.identiteter[7]

            // 2
            val hendelseRow2 = hendelseRows[1]
            hendelseRow2.arbeidssoekerId shouldBe arbeidssoekerId1
            hendelseRow2.aktorId shouldBe aktorId
            hendelseRow2.status shouldBe IdentitetHendelseStatus.VENTER
            val hendelse2 = deserializer.deserializeFromString(hendelseRow2.data)
            val endretHendelse2 = hendelse2.shouldBeTypeOf<IdentiteterEndretHendelse>()
            endretHendelse2.identiteter shouldHaveSize 8
            identitetRow1.asIdentitet() shouldBe endretHendelse2.identiteter[0]
            identitetRow2.asIdentitet() shouldBe endretHendelse2.identiteter[1]
            identitetRow3.asIdentitet() shouldBe endretHendelse2.identiteter[2]
            identitetRow4.asIdentitet() shouldBe endretHendelse2.identiteter[3]
            identitetRow5.asIdentitet() shouldBe endretHendelse2.identiteter[4]
            arbeidssoekerId2.asIdentitet(gjeldende = true) shouldBe endretHendelse2.identiteter[5]
            arbeidssoekerId1.asIdentitet(gjeldende = false) shouldBe endretHendelse2.identiteter[6]
            arbeidssoekerId3.asIdentitet(gjeldende = false) shouldBe endretHendelse2.identiteter[7]

            // 3
            val hendelseRow3 = hendelseRows[2]
            hendelseRow3.arbeidssoekerId shouldBe arbeidssoekerId3
            hendelseRow3.aktorId shouldBe aktorId
            hendelseRow3.status shouldBe IdentitetHendelseStatus.VENTER
            val hendelse3 = deserializer.deserializeFromString(hendelseRow3.data)
            val endretHendelse3 = hendelse3.shouldBeTypeOf<IdentiteterEndretHendelse>()
            endretHendelse3.identiteter shouldHaveSize 8
            identitetRow1.asIdentitet() shouldBe endretHendelse3.identiteter[0]
            identitetRow2.asIdentitet() shouldBe endretHendelse3.identiteter[1]
            identitetRow3.asIdentitet() shouldBe endretHendelse3.identiteter[2]
            identitetRow4.asIdentitet() shouldBe endretHendelse3.identiteter[3]
            identitetRow5.asIdentitet() shouldBe endretHendelse3.identiteter[4]
            arbeidssoekerId2.asIdentitet(gjeldende = true) shouldBe endretHendelse3.identiteter[5]
            arbeidssoekerId1.asIdentitet(gjeldende = false) shouldBe endretHendelse3.identiteter[6]
            arbeidssoekerId3.asIdentitet(gjeldende = false) shouldBe endretHendelse3.identiteter[7]
        }

        "Skal håndtere konflikt med to aktive perioder" {
            // GIVEN
            val aktorId = TestData.aktorId4
            val npId = TestData.npId4
            val dnr = TestData.dnr4
            val fnr1 = TestData.fnr4_1
            val fnr2 = TestData.fnr4_2
            val periodeId1 = TestData.periodeId4_1
            val periodeId2 = TestData.periodeId4_2
            val periodeId3 = TestData.periodeId4_3
            val arbeidssoekerId1 = kafkaKeysRepository.opprett(Identitetsnummer(dnr))
                .fold(onLeft = { null }, onRight = { it })!!.value
            val arbeidssoekerId2 = kafkaKeysRepository.opprett(Identitetsnummer(fnr1))
                .fold(onLeft = { null }, onRight = { it })!!.value
            val arbeidssoekerId3 = kafkaKeysRepository.opprett(Identitetsnummer(fnr2))
                .fold(onLeft = { null }, onRight = { it })!!.value

            identitetRepository.insert(
                arbeidssoekerId = arbeidssoekerId1,
                aktorId = aktorId,
                identitet = aktorId,
                type = IdentitetType.AKTORID,
                gjeldende = true,
                status = IdentitetStatus.KONFLIKT,
                sourceTimestamp = Instant.now().minus(Duration.ofDays(90))
            )
            identitetRepository.insert(
                arbeidssoekerId = arbeidssoekerId1,
                aktorId = aktorId,
                identitet = npId,
                type = IdentitetType.NPID,
                gjeldende = true,
                status = IdentitetStatus.KONFLIKT,
                sourceTimestamp = Instant.now().minus(Duration.ofDays(90))
            )
            identitetRepository.insert(
                arbeidssoekerId = arbeidssoekerId1,
                aktorId = aktorId,
                identitet = dnr,
                type = IdentitetType.FOLKEREGISTERIDENT,
                gjeldende = false,
                status = IdentitetStatus.KONFLIKT,
                sourceTimestamp = Instant.now().minus(Duration.ofDays(90))
            )
            identitetRepository.insert(
                arbeidssoekerId = arbeidssoekerId2,
                aktorId = aktorId,
                identitet = fnr1,
                type = IdentitetType.FOLKEREGISTERIDENT,
                gjeldende = false,
                status = IdentitetStatus.KONFLIKT,
                sourceTimestamp = Instant.now().minus(Duration.ofDays(60))
            )
            identitetRepository.insert(
                arbeidssoekerId = arbeidssoekerId3,
                aktorId = aktorId,
                identitet = fnr2,
                type = IdentitetType.FOLKEREGISTERIDENT,
                gjeldende = true,
                status = IdentitetStatus.KONFLIKT,
                sourceTimestamp = Instant.now().minus(Duration.ofDays(30))
            )

            identitetKonfliktRepository.insert(
                aktorId = aktorId,
                status = IdentitetKonfliktStatus.VENTER
            )

            periodeRepository.insert(
                periodeId = periodeId1,
                identitet = dnr,
                startetTimestamp = Instant.now().minus(Duration.ofDays(180)),
                avsluttetTimestamp = Instant.now().minus(Duration.ofDays(150)),
                sourceTimestamp = Instant.now()
            )
            periodeRepository.insert(
                periodeId = periodeId2,
                identitet = fnr1,
                startetTimestamp = Instant.now().minus(Duration.ofDays(120)),
                sourceTimestamp = Instant.now()
            )
            periodeRepository.insert(
                periodeId = periodeId3,
                identitet = fnr2,
                startetTimestamp = Instant.now().minus(Duration.ofDays(60)),
                sourceTimestamp = Instant.now()
            )

            // WHEN
            identitetKonfliktService.handleVentendeIdentitetKonflikter()

            // THEN
            val konfliktRows = identitetKonfliktRepository.findByAktorId(aktorId)
            konfliktRows shouldHaveSize 1
            val konflikt = konfliktRows.first()
            konflikt.aktorId shouldBe aktorId
            konflikt.status shouldBe IdentitetKonfliktStatus.KONFLIKT
            val identitetRows = identitetRepository.findByAktorId(aktorId)
            identitetRows shouldHaveSize 5

            // 1
            val identitetRow1 = identitetRows[0]
            identitetRow1.aktorId shouldBe aktorId
            identitetRow1.arbeidssoekerId shouldBe arbeidssoekerId1
            identitetRow1.identitet shouldBe aktorId
            identitetRow1.type shouldBe IdentitetType.AKTORID
            identitetRow1.gjeldende shouldBe true
            identitetRow1.status shouldBe IdentitetStatus.KONFLIKT

            // 2
            val identitetRow2 = identitetRows[1]
            identitetRow2.aktorId shouldBe aktorId
            identitetRow2.arbeidssoekerId shouldBe arbeidssoekerId1
            identitetRow2.identitet shouldBe npId
            identitetRow2.type shouldBe IdentitetType.NPID
            identitetRow2.gjeldende shouldBe true
            identitetRow2.status shouldBe IdentitetStatus.KONFLIKT

            // 3
            val identitetRow3 = identitetRows[2]
            identitetRow3.aktorId shouldBe aktorId
            identitetRow3.arbeidssoekerId shouldBe arbeidssoekerId1
            identitetRow3.identitet shouldBe dnr
            identitetRow3.type shouldBe IdentitetType.FOLKEREGISTERIDENT
            identitetRow3.gjeldende shouldBe false
            identitetRow3.status shouldBe IdentitetStatus.KONFLIKT

            // 4
            val identitetRow4 = identitetRows[3]
            identitetRow4.aktorId shouldBe aktorId
            identitetRow4.arbeidssoekerId shouldBe arbeidssoekerId2
            identitetRow4.identitet shouldBe fnr1
            identitetRow4.type shouldBe IdentitetType.FOLKEREGISTERIDENT
            identitetRow4.gjeldende shouldBe false
            identitetRow4.status shouldBe IdentitetStatus.KONFLIKT

            // 5
            val identitetRow5 = identitetRows[4]
            identitetRow5.aktorId shouldBe aktorId
            identitetRow5.arbeidssoekerId shouldBe arbeidssoekerId3
            identitetRow5.identitet shouldBe fnr2
            identitetRow5.type shouldBe IdentitetType.FOLKEREGISTERIDENT
            identitetRow5.gjeldende shouldBe true
            identitetRow5.status shouldBe IdentitetStatus.KONFLIKT

            val hendelseRows = identitetHendelseRepository.findByAktorId(aktorId)
            hendelseRows shouldHaveSize 0
        }
    }
})