package no.nav.paw.kafkakeygenerator.service

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainOnly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.paw.identitet.internehendelser.IDENTITETER_MERGET_HENDELSE_TYPE
import no.nav.paw.identitet.internehendelser.vo.Identitet
import no.nav.paw.identitet.internehendelser.vo.IdentitetType
import no.nav.paw.kafkakeygenerator.context.TestContext
import no.nav.paw.kafkakeygenerator.model.HendelseStatus
import no.nav.paw.kafkakeygenerator.model.IdentitetStatus
import no.nav.paw.kafkakeygenerator.model.KonfliktStatus
import no.nav.paw.kafkakeygenerator.model.KonfliktType
import no.nav.paw.kafkakeygenerator.model.asIdentitet
import no.nav.paw.kafkakeygenerator.test.HendelseWrapper
import no.nav.paw.kafkakeygenerator.test.IdentitetHendelseWrapper
import no.nav.paw.kafkakeygenerator.test.IdentitetWrapper
import no.nav.paw.kafkakeygenerator.test.TestData
import no.nav.paw.kafkakeygenerator.test.TestData.asIdentitetsnummer
import no.nav.paw.kafkakeygenerator.test.asWrapper
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
                val arbId1 = arbeidssoekerId1.asIdentitet(true)
                val arbId2 = arbeidssoekerId2.asIdentitet(true)
                val arbId3 = arbeidssoekerId3.asIdentitet(true)

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

                identitetRows.map { it.asWrapper() } shouldContainOnly listOf(
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId3,
                        aktorId = aktorId.identitet,
                        identitet = aktorId,
                        status = IdentitetStatus.AKTIV
                    ),
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId3,
                        aktorId = aktorId.identitet,
                        identitet = npId,
                        status = IdentitetStatus.AKTIV
                    ),
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId3,
                        aktorId = aktorId.identitet,
                        identitet = dnr,
                        status = IdentitetStatus.AKTIV
                    ),
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId3,
                        aktorId = aktorId.identitet,
                        identitet = fnr1,
                        status = IdentitetStatus.AKTIV
                    ),
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId3,
                        aktorId = aktorId.identitet,
                        identitet = fnr2,
                        status = IdentitetStatus.AKTIV
                    )
                )

                val hendelseRows = hendelseRepository.findByAktorId(aktorId.identitet)
                hendelseRows shouldHaveSize 3
                hendelseRows.map { it.asWrapper() } shouldContainOnly listOf(
                    HendelseWrapper(
                        arbeidssoekerId = arbeidssoekerId1,
                        aktorId = aktorId.identitet,
                        status = HendelseStatus.VENTER,
                        hendelse = IdentitetHendelseWrapper(
                            type = IDENTITETER_MERGET_HENDELSE_TYPE,
                            identiteter = emptyList(),
                            tidligereIdentiteter = listOf(aktorId, npId, dnr, arbId1)
                        )
                    ),
                    HendelseWrapper(
                        arbeidssoekerId = arbeidssoekerId2,
                        aktorId = aktorId.identitet,
                        status = HendelseStatus.VENTER,
                        hendelse = IdentitetHendelseWrapper(
                            type = IDENTITETER_MERGET_HENDELSE_TYPE,
                            identiteter = emptyList(),
                            tidligereIdentiteter = listOf(fnr1, arbId2)
                        )
                    ),
                    HendelseWrapper(
                        arbeidssoekerId = arbeidssoekerId3,
                        aktorId = aktorId.identitet,
                        status = HendelseStatus.VENTER,
                        hendelse = IdentitetHendelseWrapper(
                            type = IDENTITETER_MERGET_HENDELSE_TYPE,
                            identiteter = listOf(
                                aktorId,
                                npId,
                                dnr,
                                fnr1,
                                fnr2,
                                arbId1.copy(gjeldende = false),
                                arbId2.copy(gjeldende = false),
                                arbId3
                            ),
                            tidligereIdentiteter = listOf(fnr2, arbId3)
                        )
                    )
                )
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
                val arbId1 = arbeidssoekerId1.asIdentitet(true)
                val arbId2 = arbeidssoekerId2.asIdentitet(true)
                val arbId3 = arbeidssoekerId3.asIdentitet(true)

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
                identitetRows.map { it.asWrapper() } shouldContainOnly listOf(
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId1,
                        aktorId = aktorId.identitet,
                        identitet = aktorId,
                        status = IdentitetStatus.AKTIV
                    ),
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId1,
                        aktorId = aktorId.identitet,
                        identitet = npId,
                        status = IdentitetStatus.AKTIV
                    ),
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId1,
                        aktorId = aktorId.identitet,
                        identitet = dnr,
                        status = IdentitetStatus.AKTIV
                    ),
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId1,
                        aktorId = aktorId.identitet,
                        identitet = fnr1,
                        status = IdentitetStatus.AKTIV
                    ),
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId1,
                        aktorId = aktorId.identitet,
                        identitet = fnr2,
                        status = IdentitetStatus.AKTIV
                    )
                )

                val hendelseRows = hendelseRepository.findByAktorId(aktorId.identitet)
                hendelseRows shouldHaveSize 3
                hendelseRows.map { it.asWrapper() } shouldContainOnly listOf(
                    HendelseWrapper(
                        arbeidssoekerId = arbeidssoekerId1,
                        aktorId = aktorId.identitet,
                        status = HendelseStatus.VENTER,
                        hendelse = IdentitetHendelseWrapper(
                            type = IDENTITETER_MERGET_HENDELSE_TYPE,
                            identiteter = listOf(
                                aktorId,
                                npId,
                                dnr,
                                fnr1,
                                fnr2,
                                arbId1,
                                arbId2.copy(gjeldende = false),
                                arbId3.copy(gjeldende = false)
                            ),
                            tidligereIdentiteter = listOf(aktorId, npId, dnr, arbId1)
                        )
                    ),
                    HendelseWrapper(
                        arbeidssoekerId = arbeidssoekerId2,
                        aktorId = aktorId.identitet,
                        status = HendelseStatus.VENTER,
                        hendelse = IdentitetHendelseWrapper(
                            type = IDENTITETER_MERGET_HENDELSE_TYPE,
                            identiteter = emptyList(),
                            tidligereIdentiteter = listOf(fnr1, arbId2)
                        )
                    ),
                    HendelseWrapper(
                        arbeidssoekerId = arbeidssoekerId3,
                        aktorId = aktorId.identitet,
                        status = HendelseStatus.VENTER,
                        hendelse = IdentitetHendelseWrapper(
                            type = IDENTITETER_MERGET_HENDELSE_TYPE,
                            identiteter = emptyList(),
                            tidligereIdentiteter = listOf(fnr2, arbId3)
                        )
                    )
                )
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
                val arbId1 = arbeidssoekerId1.asIdentitet(true)
                val arbId2 = arbeidssoekerId2.asIdentitet(true)
                val arbId3 = arbeidssoekerId3.asIdentitet(true)

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
                identitetRows.map { it.asWrapper() } shouldContainOnly listOf(
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId2,
                        aktorId = aktorId.identitet,
                        identitet = aktorId,
                        status = IdentitetStatus.AKTIV
                    ),
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId2,
                        aktorId = aktorId.identitet,
                        identitet = npId,
                        status = IdentitetStatus.AKTIV
                    ),
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId2,
                        aktorId = aktorId.identitet,
                        identitet = dnr,
                        status = IdentitetStatus.AKTIV
                    ),
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId2,
                        aktorId = aktorId.identitet,
                        identitet = fnr1,
                        status = IdentitetStatus.AKTIV
                    ),
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId2,
                        aktorId = aktorId.identitet,
                        identitet = fnr2,
                        status = IdentitetStatus.AKTIV
                    )
                )

                val hendelseRows = hendelseRepository.findByAktorId(aktorId.identitet)
                hendelseRows shouldHaveSize 3
                hendelseRows.map { it.asWrapper() } shouldContainOnly listOf(
                    HendelseWrapper(
                        arbeidssoekerId = arbeidssoekerId1,
                        aktorId = aktorId.identitet,
                        status = HendelseStatus.VENTER,
                        hendelse = IdentitetHendelseWrapper(
                            type = IDENTITETER_MERGET_HENDELSE_TYPE,
                            identiteter = emptyList(),
                            tidligereIdentiteter = listOf(aktorId, npId, dnr, arbId1)
                        )
                    ),
                    HendelseWrapper(
                        arbeidssoekerId = arbeidssoekerId2,
                        aktorId = aktorId.identitet,
                        status = HendelseStatus.VENTER,
                        hendelse = IdentitetHendelseWrapper(
                            type = IDENTITETER_MERGET_HENDELSE_TYPE,
                            identiteter = listOf(
                                aktorId,
                                npId,
                                dnr,
                                fnr1,
                                fnr2,
                                arbId1.copy(gjeldende = false),
                                arbId2,
                                arbId3.copy(gjeldende = false)
                            ),
                            tidligereIdentiteter = listOf(fnr1, arbId2)
                        )
                    ),
                    HendelseWrapper(
                        arbeidssoekerId = arbeidssoekerId3,
                        aktorId = aktorId.identitet,
                        status = HendelseStatus.VENTER,
                        hendelse = IdentitetHendelseWrapper(
                            type = IDENTITETER_MERGET_HENDELSE_TYPE,
                            identiteter = emptyList(),
                            tidligereIdentiteter = listOf(fnr2, arbId3)
                        )
                    )
                )
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
                identitetRows.map { it.asWrapper() } shouldContainOnly listOf(
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId1,
                        aktorId = aktorId.identitet,
                        identitet = aktorId,
                        status = IdentitetStatus.MERGE
                    ),
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId1,
                        aktorId = aktorId.identitet,
                        identitet = npId,
                        status = IdentitetStatus.MERGE
                    ),
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId1,
                        aktorId = aktorId.identitet,
                        identitet = dnr,
                        status = IdentitetStatus.MERGE
                    ),
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId2,
                        aktorId = aktorId.identitet,
                        identitet = fnr1,
                        status = IdentitetStatus.MERGE
                    ),
                    IdentitetWrapper(
                        arbeidssoekerId = arbeidssoekerId3,
                        aktorId = aktorId.identitet,
                        identitet = fnr2,
                        status = IdentitetStatus.MERGE
                    )
                )

                val hendelseRows = hendelseRepository.findByAktorId(aktorId.identitet)
                hendelseRows shouldHaveSize 0
            }
        }
    }
})