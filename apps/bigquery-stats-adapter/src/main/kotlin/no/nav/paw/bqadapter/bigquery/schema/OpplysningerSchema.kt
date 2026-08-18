package no.nav.paw.bqadapter.bigquery.schema

import com.google.cloud.bigquery.Field
import com.google.cloud.bigquery.FieldList
import com.google.cloud.bigquery.Schema
import com.google.cloud.bigquery.StandardSQLTypeName.STRING
import com.google.cloud.bigquery.StandardSQLTypeName.STRUCT
import no.nav.paw.arbeidssokerregisteret.api.v4.OpplysningerOmArbeidssoeker
import no.nav.paw.bqadapter.Encoder
import no.nav.paw.bqadapter.bigquery.schema.structs.metadataStruct

private const val correlation_id = "correlation_id"
private const val opplysninger_id = "opplysninger_id"
private const val sendt_inn_av = "sendt_inn_av"
private const val utdanning = "utdanning"
private const val nus = "nus"
private const val bestaatt = "bestaatt"
private const val godkjent = "godkjent"
private const val helse = "helse"
private const val helsetilstand_hindrer_arbeid = "helsetilstand_hindrer_arbeid"
private const val jobbsituasjoner = "jobbsituasjoner"
private const val beskrivelse = "beskrivelse"
private const val stilling_styrk08 = "stilling_styrk08"
private const val annet = "annet"
private const val andre_forhold_hindrer_arbeid = "andre_forhold_hindrer_arbeid"

private val utdanningStruct: FieldList
    get() = FieldList.of(
        nus.ofRequiredType(STRING),
        bestaatt.ofOptionalType(STRING),
        godkjent.ofOptionalType(STRING)
    )

private val helseStruct: FieldList
    get() = FieldList.of(
        helsetilstand_hindrer_arbeid.ofRequiredType(STRING)
    )

private val jobbsituasjonStruct: FieldList
    get() = FieldList.of(
        beskrivelse.ofRequiredType(STRING),
        stilling_styrk08.ofOptionalType(STRING)
    )

private val annetStruct: FieldList
    get() = FieldList.of(
        andre_forhold_hindrer_arbeid.ofOptionalType(STRING)
    )

val opplysningerSchema: Schema
    get() = Schema.of(
        correlation_id.ofRequiredType(STRING),
        opplysninger_id.ofRequiredType(STRING),
        Field.newBuilder(sendt_inn_av, STRUCT, metadataStruct)
            .setMode(Field.Mode.REQUIRED)
            .build(),
        Field.newBuilder(utdanning, STRUCT, utdanningStruct)
            .setMode(Field.Mode.NULLABLE)
            .build(),
        Field.newBuilder(helse, STRUCT, helseStruct)
            .setMode(Field.Mode.NULLABLE)
            .build(),
        Field.newBuilder(jobbsituasjoner, STRUCT, jobbsituasjonStruct)
            .setMode(Field.Mode.REPEATED)
            .build(),
        Field.newBuilder(annet, STRUCT, annetStruct)
            .setMode(Field.Mode.NULLABLE)
            .build()
    )

fun opplysningerRad(
    encoder: Encoder,
    opplysninger: OpplysningerOmArbeidssoeker
): Map<String, Any> {
    val metadata = opplysninger.sendtInnAv
    val obligatoriskeFelter = mapOf(
        correlation_id to encoder.encodePeriodeId(opplysninger.periodeId),
        opplysninger_id to encoder.encodeOpplysningsId(opplysninger.id),
        sendt_inn_av to metadataStruct(
            tidspunkt = metadata.tidspunkt,
            kilde = metadata.kilde,
            aarsak = metadata.aarsak,
            brukertype = metadata.utfoertAv.type.name.lowercase()
        ),
        jobbsituasjoner to opplysninger.jobbsituasjon.beskrivelser.map { jobbsituasjon ->
            mapOf(
                beskrivelse to jobbsituasjon.beskrivelse.name.lowercase()
            ) + jobbsituasjon.detaljer[stilling_styrk08]
                ?.let { mapOf(stilling_styrk08 to it) }
                .orEmpty()
        }
    )

    return obligatoriskeFelter +
            opplysninger.utdanning?.let { utdanningData ->
                mapOf(
                    utdanning to (
                            mapOf(nus to utdanningData.nus) +
                                    utdanningData.bestaatt
                                        ?.let { mapOf(bestaatt to it.name.lowercase()) }
                                        .orEmpty() +
                                    utdanningData.godkjent
                                        ?.let { mapOf(godkjent to it.name.lowercase()) }
                                        .orEmpty()
                            )
                )
            }.orEmpty() +
            opplysninger.helse?.let { helseData ->
                mapOf(
                    helse to mapOf(
                        helsetilstand_hindrer_arbeid to helseData.helsetilstandHindrerArbeid.name.lowercase()
                    )
                )
            }.orEmpty() +
            opplysninger.annet?.let { annetData ->
                mapOf(
                    annet to annetData.andreForholdHindrerArbeid
                        ?.let { mapOf(andre_forhold_hindrer_arbeid to it.name.lowercase()) }
                        .orEmpty()
                )
            }.orEmpty()
}
