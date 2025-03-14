package no.nav.paw.bekreftelsetjeneste.tilstand

import no.nav.paw.arbeidssokerregisteret.api.v1.Periode
import kotlin.reflect.KClass

const val MAKS_ANTALL_LEVERTE_BEKREFTELSER = 3

@JvmRecord
data class BekreftelseTilstand(
    val kafkaPartition: Int,
    val periode: PeriodeInfo,
    val bekreftelser: List<Bekreftelse>
)

fun opprettBekreftelseTilstand(
    kafkaPartition: Int,
    id: Long,
    key: Long,
    periode: Periode,
): BekreftelseTilstand =
    BekreftelseTilstand(
        kafkaPartition = kafkaPartition,
        periode = PeriodeInfo(
            periodeId = periode.id,
            identitetsnummer = periode.identitetsnummer,
            arbeidsoekerId = id,
            recordKey = key,
            startet = periode.startet.tidspunkt,
            avsluttet = periode.avsluttet?.tidspunkt
        ),
        bekreftelser = emptyList()
    )

fun BekreftelseTilstand.oppdaterBekreftelse(ny: Bekreftelse): BekreftelseTilstand {
    val nyBekreftelser = bekreftelser.map {
        if (it.bekreftelseId == ny.bekreftelseId) ny else it
    }
    return copy(bekreftelser = nyBekreftelser)
}

fun BekreftelseTilstand.leggTilNyEllerOppdaterBekreftelse(ny: Bekreftelse): BekreftelseTilstand {
    val nyBekreftelser = bekreftelser
        .filter { it.bekreftelseId != ny.bekreftelseId }
        .plus(ny)
    return copy(bekreftelser = nyBekreftelser)
}

val maksAntallBekreftelserEtterStatus = mapOf(
    Levert::class to 1,
    InternBekreftelsePaaVegneAvStartet::class to 4,
    GracePeriodeUtloept::class to 10
)
fun Collection<Bekreftelse>.filterByStatusAndCount(maxSizeConfig: Map<KClass<out BekreftelseTilstandStatus>, Int>): List<Bekreftelse> =
    groupBy { it.sisteTilstand()::class }
        .mapValues { (_, values) -> values.sortedBy { it.gjelderTil }.reversed() }
        .mapValues { (status, values) -> values.take(maxSizeConfig[status] ?: Integer.MAX_VALUE) }
        .flatMap { (_, values) -> values }
        .sortedBy { it.gjelderTil }
