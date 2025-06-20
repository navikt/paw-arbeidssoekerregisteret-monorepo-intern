package no.nav.paw.kafkakeygenerator.service

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import no.nav.paw.identitet.internehendelser.IdentiteterEndretHendelse
import no.nav.paw.identitet.internehendelser.vo.Identitet
import no.nav.paw.identitet.internehendelser.vo.IdentitetType
import no.nav.paw.kafkakeygenerator.context.TestContext
import no.nav.paw.kafkakeygenerator.model.HendelseStatus
import no.nav.paw.kafkakeygenerator.model.IdentitetStatus
import no.nav.paw.kafkakeygenerator.model.KonfliktStatus
import no.nav.paw.kafkakeygenerator.model.KonfliktType
import no.nav.paw.kafkakeygenerator.model.asIdentitet
import no.nav.paw.kafkakeygenerator.test.TestData
import no.nav.paw.kafkakeygenerator.test.TestData.asIdentitetsnummer
import no.nav.paw.kafkakeygenerator.vo.Identitetsnummer
import java.time.Duration
import java.time.Instant

class KonfliktServiceTest : FreeSpec({
    with(TestContext.buildWithPostgres()) {

        beforeSpec {
            setUp()
        }

        afterSpec {
            tearDown()
        }

        /**
         * ----- MERGE -----
         */
        "Tester for merge-konflikter" - {
            "Skal håndtere merge-konflikt uten perioder" {
                // GIVEN
                val aktorId = Identitet(TestData.aktorId1, IdentitetType.AKTORID, true)
                val npId = Identitet(TestData.npId1, IdentitetType.NPID, true)
                val dnr = Identitet(TestData.dnr1, IdentitetType.FOLKEREGISTERIDENT, false)
                val fnr1 = Identitet(TestData.fnr1_1, IdentitetType.FOLKEREGISTERIDENT, false)
                val fnr2 = Identitet(TestData.fnr1_2, IdentitetType.FOLKEREGISTERIDENT, true)
                val arbeidssoekerId1 = kafkaKeysRepository.opprett(dnr.asIdentitetsnummer())
                    .fold(onLeft = { null }, onRight = { it })!!.value
                val arbeidssoekerId2 = kafkaKeysRepository.opprett(fnr1.asIdentitetsnummer())
                    .fold(onLeft = { null }, onRight = { it })!!.value
                val arbeidssoekerId3 = kafkaKeysRepository.opprett(fnr2.asIdentitetsnummer())
                    .fold(onLeft = { null }, onRight = { it })!!.value
                identitetRepository.insert(
                    arbeidssoekerId = arbeidssoekerId1,
                    aktorId = aktorId.identitet,
                    identitet = aktorId.identitet,
                    type = aktorId.type,
                    gjeldende = aktorId.gjeldende,
                    status = IdentitetStatus.MERGE,
                    sourceTimestamp = Instant.now()
                )
                identitetRepository.insert(
                    arbeidssoekerId = arbeidssoekerId1,
                    aktorId = aktorId.identitet,
                    identitet = npId.identitet,
                    type = npId.type,
                    gjeldende = npId.gjeldende,
                    status = IdentitetStatus.MERGE,
                    sourceTimestamp = Instant.now()
                )
                identitetRepository.insert(
                    arbeidssoekerId = arbeidssoekerId1,
                    aktorId = aktorId.identitet,
                    identitet = dnr.identitet,
                    type = dnr.type,
                    gjeldende = dnr.gjeldende,
                    status = IdentitetStatus.MERGE,
                    sourceTimestamp = Instant.now()
                )
                identitetRepository.insert(
                    arbeidssoekerId = arbeidssoekerId2,
                    aktorId = aktorId.identitet,
                    identitet = fnr1.identitet,
                    type = fnr1.type,
                    gjeldende = fnr1.gjeldende,
                    status = IdentitetStatus.MERGE,
                    sourceTimestamp = Instant.now()
                )
                identitetRepository.insert(
                    arbeidssoekerId = arbeidssoekerId3,
                    aktorId = aktorId.identitet,
                    identitet = fnr2.identitet,
                    type = fnr2.type,
                    gjeldende = fnr2.gjeldende,
                    status = IdentitetStatus.MERGE,
                    sourceTimestamp = Instant.now()
                )
                konfliktRepository.insert(
                    aktorId = aktorId.identitet,
                    type = KonfliktType.MERGE,
                    status = KonfliktStatus.VENTER,
                    sourceTimestamp = Instant.now(),
                    identiteter = listOf(aktorId, npId, dnr, fnr1, fnr2)
                )

                // WHEN
                konfliktService.handleVentendeMergeKonflikter()

                // THEN
                val konfliktRows = konfliktRepository.findByAktorId(aktorId.identitet)
                konfliktRows shouldHaveSize 1

                val konfliktRow1 = konfliktRows[0]
                konfliktRow1.aktorId shouldBe aktorId.identitet
                konfliktRow1.type shouldBe KonfliktType.MERGE
                konfliktRow1.status shouldBe KonfliktStatus.FULLFOERT

                val identitetRows = identitetRepository.findByAktorId(aktorId.identitet)
                identitetRows shouldHaveSize 5

                // 1
                val identitetRow1 = identitetRows[0]
                identitetRow1.aktorId shouldBe aktorId.identitet
                identitetRow1.arbeidssoekerId shouldBe arbeidssoekerId3
                identitetRow1.asIdentitet() shouldBe aktorId
                identitetRow1.status shouldBe IdentitetStatus.AKTIV

                // 2
                val identitetRow2 = identitetRows[1]
                identitetRow2.aktorId shouldBe aktorId.identitet
                identitetRow2.arbeidssoekerId shouldBe arbeidssoekerId3
                identitetRow2.asIdentitet() shouldBe npId
                identitetRow2.status shouldBe IdentitetStatus.AKTIV

                // 3
                val identitetRow3 = identitetRows[2]
                identitetRow3.aktorId shouldBe aktorId.identitet
                identitetRow3.arbeidssoekerId shouldBe arbeidssoekerId3
                identitetRow3.asIdentitet() shouldBe dnr
                identitetRow3.status shouldBe IdentitetStatus.AKTIV

                // 4
                val identitetRow4 = identitetRows[3]
                identitetRow4.aktorId shouldBe aktorId.identitet
                identitetRow4.arbeidssoekerId shouldBe arbeidssoekerId3
                identitetRow4.asIdentitet() shouldBe fnr1
                identitetRow4.status shouldBe IdentitetStatus.AKTIV

                // 5
                val identitetRow5 = identitetRows[4]
                identitetRow5.aktorId shouldBe aktorId.identitet
                identitetRow5.arbeidssoekerId shouldBe arbeidssoekerId3
                identitetRow5.asIdentitet() shouldBe fnr2
                identitetRow5.status shouldBe IdentitetStatus.AKTIV

                val hendelseRows = hendelseRepository.findByAktorId(aktorId.identitet)
                hendelseRows shouldHaveSize 3

                // 1
                val hendelseRow1 = hendelseRows[0]
                hendelseRow1.arbeidssoekerId shouldBe arbeidssoekerId3
                hendelseRow1.aktorId shouldBe aktorId.identitet
                hendelseRow1.status shouldBe HendelseStatus.VENTER
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
                hendelseRow2.aktorId shouldBe aktorId.identitet
                hendelseRow2.status shouldBe HendelseStatus.VENTER
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
                hendelseRow3.aktorId shouldBe aktorId.identitet
                hendelseRow3.status shouldBe HendelseStatus.VENTER
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

            "Skal håndtere merge-konflikt med kun avsluttede perioder" {
                // GIVEN
                val aktorId = Identitet(TestData.aktorId2, IdentitetType.AKTORID, true)
                val npId = Identitet(TestData.npId2, IdentitetType.NPID, true)
                val dnr = Identitet(TestData.dnr2, IdentitetType.FOLKEREGISTERIDENT, false)
                val fnr1 = Identitet(TestData.fnr2_1, IdentitetType.FOLKEREGISTERIDENT, false)
                val fnr2 = Identitet(TestData.fnr2_2, IdentitetType.FOLKEREGISTERIDENT, true)
                val periodeId1 = TestData.periodeId2_1
                val periodeId2 = TestData.periodeId2_2
                val periodeId3 = TestData.periodeId2_3
                val arbeidssoekerId1 = kafkaKeysRepository.opprett(dnr.asIdentitetsnummer())
                    .fold(onLeft = { null }, onRight = { it })!!.value
                val arbeidssoekerId2 = kafkaKeysRepository.opprett(fnr1.asIdentitetsnummer())
                    .fold(onLeft = { null }, onRight = { it })!!.value
                val arbeidssoekerId3 = kafkaKeysRepository.opprett(fnr2.asIdentitetsnummer())
                    .fold(onLeft = { null }, onRight = { it })!!.value
                identitetRepository.insert(
                    arbeidssoekerId = arbeidssoekerId1,
                    aktorId = aktorId.identitet,
                    identitet = aktorId.identitet,
                    type = aktorId.type,
                    gjeldende = aktorId.gjeldende,
                    status = IdentitetStatus.MERGE,
                    sourceTimestamp = Instant.now().minus(Duration.ofDays(90))
                )
                identitetRepository.insert(
                    arbeidssoekerId = arbeidssoekerId1,
                    aktorId = aktorId.identitet,
                    identitet = npId.identitet,
                    type = npId.type,
                    gjeldende = npId.gjeldende,
                    status = IdentitetStatus.MERGE,
                    sourceTimestamp = Instant.now().minus(Duration.ofDays(90))
                )
                identitetRepository.insert(
                    arbeidssoekerId = arbeidssoekerId1,
                    aktorId = aktorId.identitet,
                    identitet = dnr.identitet,
                    type = dnr.type,
                    gjeldende = dnr.gjeldende,
                    status = IdentitetStatus.MERGE,
                    sourceTimestamp = Instant.now().minus(Duration.ofDays(90))
                )
                identitetRepository.insert(
                    arbeidssoekerId = arbeidssoekerId2,
                    aktorId = aktorId.identitet,
                    identitet = fnr1.identitet,
                    type = fnr1.type,
                    gjeldende = fnr1.gjeldende,
                    status = IdentitetStatus.MERGE,
                    sourceTimestamp = Instant.now().minus(Duration.ofDays(60))
                )
                identitetRepository.insert(
                    arbeidssoekerId = arbeidssoekerId3,
                    aktorId = aktorId.identitet,
                    identitet = fnr2.identitet,
                    type = fnr2.type,
                    gjeldende = fnr2.gjeldende,
                    status = IdentitetStatus.MERGE,
                    sourceTimestamp = Instant.now().minus(Duration.ofDays(30))
                )
                konfliktRepository.insert(
                    aktorId = aktorId.identitet,
                    type = KonfliktType.MERGE,
                    status = KonfliktStatus.VENTER,
                    sourceTimestamp = Instant.now(),
                    identiteter = listOf(aktorId, npId, dnr, fnr1, fnr2)
                )
                periodeRepository.insert(
                    periodeId = periodeId1,
                    identitet = fnr1.identitet,
                    startetTimestamp = Instant.now().minus(Duration.ofDays(180)),
                    avsluttetTimestamp = Instant.now().minus(Duration.ofDays(150)),
                    sourceTimestamp = Instant.now()
                )
                periodeRepository.insert(
                    periodeId = periodeId2,
                    identitet = fnr2.identitet,
                    startetTimestamp = Instant.now().minus(Duration.ofDays(120)),
                    avsluttetTimestamp = Instant.now().minus(Duration.ofDays(90)),
                    sourceTimestamp = Instant.now()
                )
                periodeRepository.insert(
                    periodeId = periodeId3,
                    identitet = dnr.identitet,
                    startetTimestamp = Instant.now().minus(Duration.ofDays(60)),
                    avsluttetTimestamp = Instant.now().minus(Duration.ofDays(30)),
                    sourceTimestamp = Instant.now()
                )

                // WHEN
                konfliktService.handleVentendeMergeKonflikter()

                // THEN
                val konfliktRows = konfliktRepository.findByAktorId(aktorId.identitet)
                konfliktRows shouldHaveSize 1
                val konfliktRow1 = konfliktRows[0]
                konfliktRow1.aktorId shouldBe aktorId.identitet
                konfliktRow1.type shouldBe KonfliktType.MERGE
                konfliktRow1.status shouldBe KonfliktStatus.FULLFOERT
                val identitetRows = identitetRepository.findByAktorId(aktorId.identitet)
                identitetRows shouldHaveSize 5

                // 1
                val identitetRow1 = identitetRows[0]
                identitetRow1.aktorId shouldBe aktorId.identitet
                identitetRow1.arbeidssoekerId shouldBe arbeidssoekerId1
                identitetRow1.asIdentitet() shouldBe aktorId
                identitetRow1.status shouldBe IdentitetStatus.AKTIV

                // 2
                val identitetRow2 = identitetRows[1]
                identitetRow2.aktorId shouldBe aktorId.identitet
                identitetRow2.arbeidssoekerId shouldBe arbeidssoekerId1
                identitetRow2.asIdentitet() shouldBe npId
                identitetRow2.status shouldBe IdentitetStatus.AKTIV

                // 3
                val identitetRow3 = identitetRows[2]
                identitetRow3.aktorId shouldBe aktorId.identitet
                identitetRow3.arbeidssoekerId shouldBe arbeidssoekerId1
                identitetRow3.asIdentitet() shouldBe dnr
                identitetRow3.status shouldBe IdentitetStatus.AKTIV

                // 4
                val identitetRow4 = identitetRows[3]
                identitetRow4.aktorId shouldBe aktorId.identitet
                identitetRow4.arbeidssoekerId shouldBe arbeidssoekerId1
                identitetRow4.asIdentitet() shouldBe fnr1
                identitetRow4.status shouldBe IdentitetStatus.AKTIV

                // 5
                val identitetRow5 = identitetRows[4]
                identitetRow5.aktorId shouldBe aktorId.identitet
                identitetRow5.arbeidssoekerId shouldBe arbeidssoekerId1
                identitetRow5.asIdentitet() shouldBe fnr2
                identitetRow5.status shouldBe IdentitetStatus.AKTIV

                val hendelseRows = hendelseRepository.findByAktorId(aktorId.identitet)
                hendelseRows shouldHaveSize 3

                // 1
                val hendelseRow1 = hendelseRows[0]
                hendelseRow1.arbeidssoekerId shouldBe arbeidssoekerId1
                hendelseRow1.aktorId shouldBe aktorId.identitet
                hendelseRow1.status shouldBe HendelseStatus.VENTER
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
                hendelseRow2.aktorId shouldBe aktorId.identitet
                hendelseRow2.status shouldBe HendelseStatus.VENTER
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
                hendelseRow3.aktorId shouldBe aktorId.identitet
                hendelseRow3.status shouldBe HendelseStatus.VENTER
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

            "Skal håndtere merge-konflikt med én aktiv periode" {
                // GIVEN
                val aktorId = Identitet(TestData.aktorId3, IdentitetType.AKTORID, true)
                val npId = Identitet(TestData.npId3, IdentitetType.NPID, true)
                val dnr = Identitet(TestData.dnr3, IdentitetType.FOLKEREGISTERIDENT, false)
                val fnr1 = Identitet(TestData.fnr3_1, IdentitetType.FOLKEREGISTERIDENT, false)
                val fnr2 = Identitet(TestData.fnr3_2, IdentitetType.FOLKEREGISTERIDENT, true)
                val periodeId1 = TestData.periodeId3_1
                val periodeId2 = TestData.periodeId3_2
                val periodeId3 = TestData.periodeId3_3
                val arbeidssoekerId1 = kafkaKeysRepository.opprett(Identitetsnummer(dnr.identitet))
                    .fold(onLeft = { null }, onRight = { it })!!.value
                val arbeidssoekerId2 = kafkaKeysRepository.opprett(Identitetsnummer(fnr1.identitet))
                    .fold(onLeft = { null }, onRight = { it })!!.value
                val arbeidssoekerId3 = kafkaKeysRepository.opprett(Identitetsnummer(fnr2.identitet))
                    .fold(onLeft = { null }, onRight = { it })!!.value
                identitetRepository.insert(
                    arbeidssoekerId = arbeidssoekerId1,
                    aktorId = aktorId.identitet,
                    identitet = aktorId.identitet,
                    type = aktorId.type,
                    gjeldende = aktorId.gjeldende,
                    status = IdentitetStatus.MERGE,
                    sourceTimestamp = Instant.now().minus(Duration.ofDays(90))
                )
                identitetRepository.insert(
                    arbeidssoekerId = arbeidssoekerId1,
                    aktorId = aktorId.identitet,
                    identitet = npId.identitet,
                    type = npId.type,
                    gjeldende = npId.gjeldende,
                    status = IdentitetStatus.MERGE,
                    sourceTimestamp = Instant.now().minus(Duration.ofDays(90))
                )
                identitetRepository.insert(
                    arbeidssoekerId = arbeidssoekerId1,
                    aktorId = aktorId.identitet,
                    identitet = dnr.identitet,
                    type = dnr.type,
                    gjeldende = dnr.gjeldende,
                    status = IdentitetStatus.MERGE,
                    sourceTimestamp = Instant.now().minus(Duration.ofDays(90))
                )
                identitetRepository.insert(
                    arbeidssoekerId = arbeidssoekerId2,
                    aktorId = aktorId.identitet,
                    identitet = fnr1.identitet,
                    type = fnr1.type,
                    gjeldende = fnr1.gjeldende,
                    status = IdentitetStatus.MERGE,
                    sourceTimestamp = Instant.now().minus(Duration.ofDays(60))
                )
                identitetRepository.insert(
                    arbeidssoekerId = arbeidssoekerId3,
                    aktorId = aktorId.identitet,
                    identitet = fnr2.identitet,
                    type = fnr2.type,
                    gjeldende = fnr2.gjeldende,
                    status = IdentitetStatus.MERGE,
                    sourceTimestamp = Instant.now().minus(Duration.ofDays(30))
                )
                konfliktRepository.insert(
                    aktorId = aktorId.identitet,
                    type = KonfliktType.MERGE,
                    status = KonfliktStatus.VENTER,
                    sourceTimestamp = Instant.now(),
                    identiteter = listOf(aktorId, npId, dnr, fnr1, fnr2)
                )
                periodeRepository.insert(
                    periodeId = periodeId1,
                    identitet = dnr.identitet,
                    startetTimestamp = Instant.now().minus(Duration.ofDays(180)),
                    avsluttetTimestamp = Instant.now().minus(Duration.ofDays(150)),
                    sourceTimestamp = Instant.now()
                )
                periodeRepository.insert(
                    periodeId = periodeId2,
                    identitet = fnr1.identitet,
                    startetTimestamp = Instant.now().minus(Duration.ofDays(120)),
                    sourceTimestamp = Instant.now()
                )
                periodeRepository.insert(
                    periodeId = periodeId3,
                    identitet = fnr2.identitet,
                    startetTimestamp = Instant.now().minus(Duration.ofDays(60)),
                    avsluttetTimestamp = Instant.now().minus(Duration.ofDays(30)),
                    sourceTimestamp = Instant.now()
                )

                // WHEN
                konfliktService.handleVentendeMergeKonflikter()

                // THEN
                val konfliktRows = konfliktRepository.findByAktorId(aktorId.identitet)
                konfliktRows shouldHaveSize 1
                val konflikt = konfliktRows.first()
                konflikt.aktorId shouldBe aktorId.identitet
                konflikt.type shouldBe KonfliktType.MERGE
                konflikt.status shouldBe KonfliktStatus.FULLFOERT
                val identitetRows = identitetRepository.findByAktorId(aktorId.identitet)
                identitetRows shouldHaveSize 5

                // 1
                val identitetRow1 = identitetRows[0]
                identitetRow1.aktorId shouldBe aktorId.identitet
                identitetRow1.arbeidssoekerId shouldBe arbeidssoekerId2
                identitetRow1.asIdentitet() shouldBe aktorId
                identitetRow1.status shouldBe IdentitetStatus.AKTIV

                // 2
                val identitetRow2 = identitetRows[1]
                identitetRow2.aktorId shouldBe aktorId.identitet
                identitetRow2.arbeidssoekerId shouldBe arbeidssoekerId2
                identitetRow2.asIdentitet() shouldBe npId
                identitetRow2.status shouldBe IdentitetStatus.AKTIV

                // 3
                val identitetRow3 = identitetRows[2]
                identitetRow3.aktorId shouldBe aktorId.identitet
                identitetRow3.arbeidssoekerId shouldBe arbeidssoekerId2
                identitetRow3.asIdentitet() shouldBe dnr
                identitetRow3.status shouldBe IdentitetStatus.AKTIV

                // 4
                val identitetRow4 = identitetRows[3]
                identitetRow4.aktorId shouldBe aktorId.identitet
                identitetRow4.arbeidssoekerId shouldBe arbeidssoekerId2
                identitetRow4.asIdentitet() shouldBe fnr1
                identitetRow4.status shouldBe IdentitetStatus.AKTIV

                // 5
                val identitetRow5 = identitetRows[4]
                identitetRow5.aktorId shouldBe aktorId.identitet
                identitetRow5.arbeidssoekerId shouldBe arbeidssoekerId2
                identitetRow5.asIdentitet() shouldBe fnr2
                identitetRow5.status shouldBe IdentitetStatus.AKTIV

                val hendelseRows = hendelseRepository.findByAktorId(aktorId.identitet)
                hendelseRows shouldHaveSize 3

                // 1
                val hendelseRow1 = hendelseRows[0]
                hendelseRow1.arbeidssoekerId shouldBe arbeidssoekerId2
                hendelseRow1.aktorId shouldBe aktorId.identitet
                hendelseRow1.status shouldBe HendelseStatus.VENTER
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
                hendelseRow2.aktorId shouldBe aktorId.identitet
                hendelseRow2.status shouldBe HendelseStatus.VENTER
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
                hendelseRow3.aktorId shouldBe aktorId.identitet
                hendelseRow3.status shouldBe HendelseStatus.VENTER
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

            "Skal håndtere merge-konflikt med to aktive perioder" {
                // GIVEN
                val aktorId = Identitet(TestData.aktorId4, IdentitetType.AKTORID, true)
                val npId = Identitet(TestData.npId4, IdentitetType.NPID, true)
                val dnr = Identitet(TestData.dnr4, IdentitetType.FOLKEREGISTERIDENT, false)
                val fnr1 = Identitet(TestData.fnr4_1, IdentitetType.FOLKEREGISTERIDENT, false)
                val fnr2 = Identitet(TestData.fnr4_2, IdentitetType.FOLKEREGISTERIDENT, true)
                val periodeId1 = TestData.periodeId4_1
                val periodeId2 = TestData.periodeId4_2
                val periodeId3 = TestData.periodeId4_3
                val arbeidssoekerId1 = kafkaKeysRepository.opprett(Identitetsnummer(dnr.identitet))
                    .fold(onLeft = { null }, onRight = { it })!!.value
                val arbeidssoekerId2 = kafkaKeysRepository.opprett(Identitetsnummer(fnr1.identitet))
                    .fold(onLeft = { null }, onRight = { it })!!.value
                val arbeidssoekerId3 = kafkaKeysRepository.opprett(Identitetsnummer(fnr2.identitet))
                    .fold(onLeft = { null }, onRight = { it })!!.value

                identitetRepository.insert(
                    arbeidssoekerId = arbeidssoekerId1,
                    aktorId = aktorId.identitet,
                    identitet = aktorId.identitet,
                    type = IdentitetType.AKTORID,
                    gjeldende = true,
                    status = IdentitetStatus.MERGE,
                    sourceTimestamp = Instant.now().minus(Duration.ofDays(90))
                )
                identitetRepository.insert(
                    arbeidssoekerId = arbeidssoekerId1,
                    aktorId = aktorId.identitet,
                    identitet = npId.identitet,
                    type = IdentitetType.NPID,
                    gjeldende = true,
                    status = IdentitetStatus.MERGE,
                    sourceTimestamp = Instant.now().minus(Duration.ofDays(90))
                )
                identitetRepository.insert(
                    arbeidssoekerId = arbeidssoekerId1,
                    aktorId = aktorId.identitet,
                    identitet = dnr.identitet,
                    type = IdentitetType.FOLKEREGISTERIDENT,
                    gjeldende = false,
                    status = IdentitetStatus.MERGE,
                    sourceTimestamp = Instant.now().minus(Duration.ofDays(90))
                )
                identitetRepository.insert(
                    arbeidssoekerId = arbeidssoekerId2,
                    aktorId = aktorId.identitet,
                    identitet = fnr1.identitet,
                    type = IdentitetType.FOLKEREGISTERIDENT,
                    gjeldende = false,
                    status = IdentitetStatus.MERGE,
                    sourceTimestamp = Instant.now().minus(Duration.ofDays(60))
                )
                identitetRepository.insert(
                    arbeidssoekerId = arbeidssoekerId3,
                    aktorId = aktorId.identitet,
                    identitet = fnr2.identitet,
                    type = IdentitetType.FOLKEREGISTERIDENT,
                    gjeldende = true,
                    status = IdentitetStatus.MERGE,
                    sourceTimestamp = Instant.now().minus(Duration.ofDays(30))
                )

                konfliktRepository.insert(
                    aktorId = aktorId.identitet,
                    type = KonfliktType.MERGE,
                    status = KonfliktStatus.VENTER,
                    sourceTimestamp = Instant.now(),
                    identiteter = listOf(aktorId, npId, dnr, fnr1, fnr2)
                )

                periodeRepository.insert(
                    periodeId = periodeId1,
                    identitet = dnr.identitet,
                    startetTimestamp = Instant.now().minus(Duration.ofDays(180)),
                    avsluttetTimestamp = Instant.now().minus(Duration.ofDays(150)),
                    sourceTimestamp = Instant.now()
                )
                periodeRepository.insert(
                    periodeId = periodeId2,
                    identitet = fnr1.identitet,
                    startetTimestamp = Instant.now().minus(Duration.ofDays(120)),
                    sourceTimestamp = Instant.now()
                )
                periodeRepository.insert(
                    periodeId = periodeId3,
                    identitet = fnr2.identitet,
                    startetTimestamp = Instant.now().minus(Duration.ofDays(60)),
                    sourceTimestamp = Instant.now()
                )

                // WHEN
                konfliktService.handleVentendeMergeKonflikter()

                // THEN
                val konfliktRows = konfliktRepository.findByAktorId(aktorId.identitet)
                konfliktRows shouldHaveSize 1
                val konfliktRow1 = konfliktRows.first()
                konfliktRow1.aktorId shouldBe aktorId.identitet
                konfliktRow1.type shouldBe KonfliktType.MERGE
                konfliktRow1.status shouldBe KonfliktStatus.FEILET
                val identitetRows = identitetRepository.findByAktorId(aktorId.identitet)
                identitetRows shouldHaveSize 5

                // 1
                val identitetRow1 = identitetRows[0]
                identitetRow1.aktorId shouldBe aktorId.identitet
                identitetRow1.arbeidssoekerId shouldBe arbeidssoekerId1
                identitetRow1.asIdentitet() shouldBe aktorId
                identitetRow1.status shouldBe IdentitetStatus.MERGE

                // 2
                val identitetRow2 = identitetRows[1]
                identitetRow2.aktorId shouldBe aktorId.identitet
                identitetRow2.arbeidssoekerId shouldBe arbeidssoekerId1
                identitetRow2.asIdentitet() shouldBe npId
                identitetRow2.status shouldBe IdentitetStatus.MERGE

                // 3
                val identitetRow3 = identitetRows[2]
                identitetRow3.aktorId shouldBe aktorId.identitet
                identitetRow3.arbeidssoekerId shouldBe arbeidssoekerId1
                identitetRow3.asIdentitet() shouldBe dnr
                identitetRow3.status shouldBe IdentitetStatus.MERGE

                // 4
                val identitetRow4 = identitetRows[3]
                identitetRow4.aktorId shouldBe aktorId.identitet
                identitetRow4.arbeidssoekerId shouldBe arbeidssoekerId2
                identitetRow4.asIdentitet() shouldBe fnr1
                identitetRow4.status shouldBe IdentitetStatus.MERGE

                // 5
                val identitetRow5 = identitetRows[4]
                identitetRow5.aktorId shouldBe aktorId.identitet
                identitetRow5.arbeidssoekerId shouldBe arbeidssoekerId3
                identitetRow5.asIdentitet() shouldBe fnr2
                identitetRow5.status shouldBe IdentitetStatus.MERGE

                val hendelseRows = hendelseRepository.findByAktorId(aktorId.identitet)
                hendelseRows shouldHaveSize 0
            }
        }
    }
})