package no.nav.paw.arbeidssokerregisteret.arbeidssoker

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.CustomFunction
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.UUIDColumnType
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime
import java.util.*

object ArbeidssokerPeriodeTable : Table(name = "arbeidssokerperiode") {
    val id: Column<UUID> = uuid(name = "id").defaultExpression(CustomFunction("gen_random_uuid", UUIDColumnType()))
    val foedselsnummer: Column<String> = varchar(name = "foedselsnummer", length = 11)
    val opprettetTidspunkt: Column<LocalDateTime> = datetime(name = "opprettet_tidspunkt").defaultExpression(CurrentDateTime)
    val endretTidspunkt: Column<LocalDateTime> = datetime(name = "endret_tidspunkt").defaultExpression(CurrentDateTime)
    val fraOgMedDato: Column<LocalDateTime> = datetime(name = "fra_og_med_dato")
    val tilOgMedDato: Column<LocalDateTime?> = datetime(name = "til_og_med_dato").nullable()
    val begrunnelse: Column<String?> = varchar(name = "begrunnelse", length = 255).nullable()
    val opprettetAv: Column<String?> = varchar(name = "opprettet_av", length = 255).nullable()

    override val primaryKey: PrimaryKey = PrimaryKey(foedselsnummer, name = "index_arbeidssokerperiode_foedselsnummer")
}
