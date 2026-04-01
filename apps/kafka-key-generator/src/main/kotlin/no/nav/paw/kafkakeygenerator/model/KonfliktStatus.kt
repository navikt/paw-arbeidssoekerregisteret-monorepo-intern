package no.nav.paw.kafkakeygenerator.model

/**
 * Statusen til en konflikt.
 *
 * Statusflyt for MERGE:
 *   * Normal flyt: VENTER -> PROSESSERER -> FULLFOERT
 *   * Ved problemer: VENTER -> PROSESSERER -> PAUSET
 *   * Manuelt avbrutt: VENTER -> SLETTET
 *
 * Statusflyt for SPLITT:
 *   * Normal flyt: VENTER -> PROSESSERER -> FULLFOERT
 *   * Ved problemer: VENTER -> PROSESSERER -> PAUSET
 *   * Manuelt avbrutt: VENTER -> SLETTET
 */
enum class KonfliktStatus {
    VENTER,
    VALGT,
    PROSESSERER,
    PAUSET,
    FEILET,
    FULLFOERT,
    SLETTET
}