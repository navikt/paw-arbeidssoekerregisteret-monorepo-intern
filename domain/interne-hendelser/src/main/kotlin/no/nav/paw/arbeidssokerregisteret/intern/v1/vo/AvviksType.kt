package no.nav.paw.arbeidssokerregisteret.intern.v1.vo

enum class AvviksType {
    FORSINKELSE,
    @Deprecated("Erstattet av 'SLETTET'", replaceWith = ReplaceWith("SLETTET"))
    RETTING,
    SLETTET,
    TIDSPUNKT_KORRIGERT
}