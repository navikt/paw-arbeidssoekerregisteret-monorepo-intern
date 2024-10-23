package no.nav.paw.arbeidssoekerregisteret.testdata.bekreftelse

import no.nav.paw.bekreftelse.ansvar.v1.AnsvarEndret
import no.nav.paw.bekreftelse.ansvar.v1.vo.AvslutterAnsvar
import no.nav.paw.bekreftelse.ansvar.v1.vo.Bekreftelsesloesning
import no.nav.paw.bekreftelse.ansvar.v1.vo.TarAnsvar
import java.time.Duration
import java.util.UUID


fun tarAnsvar(
    periodeId: UUID = UUID.randomUUID(),
    bekreftelsesloesning: Bekreftelsesloesning = Bekreftelsesloesning.DAGPENGER,
    grace: Duration = Duration.ofDays(14),
    interval: Duration = Duration.ofDays(7)
): AnsvarEndret =
    AnsvarEndret.newBuilder()
        .setHandling(
            TarAnsvar
                .newBuilder()
                .setGraceMS(grace.toMillis())
                .setIntervalMS(interval.toMillis())
                .build()
        )
        .setPeriodeId(periodeId)
        .setBekreftelsesloesning(bekreftelsesloesning)
        .build()

fun avslutterAnsvar(
    periodeId: UUID = UUID.randomUUID(),
    bekreftelsesloesning: Bekreftelsesloesning = Bekreftelsesloesning.DAGPENGER
): AnsvarEndret =
    AnsvarEndret.newBuilder()
        .setHandling(AvslutterAnsvar())
        .setBekreftelsesloesning(bekreftelsesloesning)
        .setPeriodeId(periodeId)
        .build()