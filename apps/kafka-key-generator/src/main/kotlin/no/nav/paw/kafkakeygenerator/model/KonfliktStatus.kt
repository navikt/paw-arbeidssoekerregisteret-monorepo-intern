package no.nav.paw.kafkakeygenerator.model

/**
 * Statusen til en konflikt.
 *
 * Statusflyt for MERGE:
 *   * Normal flyt: VENTER -> PROSESSERER -> FULLFOERT
 *   * Ved feil: VENTER -> PROSESSERER -> FEILET
 *   * Manuelt avbrutt: VENTER -> SLETTET
 *
 * Statusflyt for SPLITT:
 *   * Manuelt valgt: VENTER -> VALGT
 *   * Normal flyt: VALGT -> PROSESSERER -> FULLFOERT
 *   * Ved feil: VALGT -> PROSESSERER -> FEILET
 *   * Manuelt avbrutt: VENTER / VALGT -> SLETTET
 */
enum class KonfliktStatus {
    VENTER, VALGT, PROSESSERER, FEILET, FULLFOERT, SLETTET
}