package no.nav.paw.arbeidssokerregisteret.application.opplysninger

/**
 * Enkelt interface for å representere en effekt av en opplysning eller andre ting.
 * Feks ved bruk på opplysninger vil 'Negative' indikere at denne opplysningen går i rettning av å ikke kunne starte/stoppes en periode.
 */
sealed interface Effect {
    interface Negative: Effect
    interface Positive: Effect
    interface Neutral: Effect
}