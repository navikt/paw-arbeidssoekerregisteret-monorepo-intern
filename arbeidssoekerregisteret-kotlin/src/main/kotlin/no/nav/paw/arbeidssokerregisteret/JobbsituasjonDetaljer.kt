package no.nav.paw.arbeidssokerregisteret

/**
 * Denne filen inneholder gyldige nøkler for map(no.nav.paw.arbeidssokerregisteret.api.v1.Element.detaljer).
 * Denne listen er nødvendigvis ikke dekkende, så klienter må takle å finne andre nøkler også. Det er heller ikke
 * gitt at alle nøklene alltid er tilstede(selv om de er oppført som aktuelle for den gitte jobb situasjonen).
 */


/**
 * Tekst beskrivelse av en stilling, aktuelt for:
 * {@link no.nav.paw.arbeidssokerregisteret.api.v1.Element.JobbsituasjonBeskrivelse.HAR_SAGT_OPP}
 * {@link no.nav.paw.arbeidssokerregisteret.api.v1.Element.JobbsituasjonBeskrivelse.HAR_BLITT_SAGT_OPP}
 * {@link no.nav.paw.arbeidssokerregisteret.api.v1.Element.JobbsituasjonBeskrivelse.ER_PERMITTERT}
 */
const val STILLING = "stilling"

/**
 * Styrk08 kode for en stilling
 * Aktuelle i same tilfeller som {@link STILLING}
 * */
const val STILLING_STYRK08 = "stilling_styrk08"

/** konsept id for stilling, mulig denne blir droppet i fremtidig versjoner.*/
const val KONSEPT_ID = "konsept_id"

/**
 * Datoen situasjonen gjelder fra,
 * feks dato oppsigelsen er gitt(eller oppsgielses tiden startet)
 * */
const val GJELDER_FRA_DATO = "gjelder_fra_dato_iso8601"
/** Datoen situasjonen gjelder til*/
const val GJELDER_TIL_DATO = "gjelder_til_dato_iso8601"

/**
 * Prosent verdi for den antatt situasjonen, aktuelt for:
 * {@link no.nav.paw.arbeidssokerregisteret.api.v1.Element.JobbsituasjonBeskrivelse.ER_PERMITTERT}
 * {@link no.nav.paw.arbeidssokerregisteret.api.v1.Element.JobbsituasjonBeskrivelse.DELETTIDSJOBB_VIL_MER}
 */
const val PROSENT = "prosent"

/**
 * Tekst beskrivelse av en stilling, aktuelt for:
 * {@link no.nav.paw.arbeidssokerregisteret.api.v1.Element.JobbsituasjonBeskrivelse.HAR_SAGT_OPP}
 * {@link no.nav.paw.arbeidssokerregisteret.api.v1.Element.JobbsituasjonBeskrivelse.HAR_BLITT_SAGT_OPP}
 */
const val SISTE_DAG_MED_LOENN = "siste_dag_med_loen_iso8601"

/**
 * Siste arbeidsdag, aktuelt for:
 * {@link no.nav.paw.arbeidssokerregisteret.api.v1.Element.JobbsituasjonBeskrivelse.KONKURS}
 */
const val SISTE_ARBEIDSDAG = "siste_arbeidsdag_iso8601"