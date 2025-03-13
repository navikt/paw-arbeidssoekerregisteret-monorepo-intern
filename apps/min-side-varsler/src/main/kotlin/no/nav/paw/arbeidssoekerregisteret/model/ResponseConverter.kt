package no.nav.paw.arbeidssoekerregisteret.model

import no.nav.paw.arbeidssoekerregisteret.api.models.BestillingResponse
import no.nav.paw.arbeidssoekerregisteret.api.models.EksterntVarselResponse
import no.nav.paw.arbeidssoekerregisteret.api.models.HendelseName
import no.nav.paw.arbeidssoekerregisteret.api.models.VarselResponse

fun VarselRow.asResponse() = VarselResponse(
    varselId = this.varselId,
    periodeId = this.periodeId,
    varselKilde = this.varselKilde.asResponse(),
    varselType = this.varselType.asResponse(),
    varselStatus = this.varselStatus.asResponse(),
    hendelseName = this.hendelseName.asResponse(),
    hendelseTimestamp = this.hendelseTimestamp,
    insertedTimestamp = this.insertedTimestamp,
    updatedTimestamp = this.updatedTimestamp,
    eksterntVarsel = this.eksterntVarsel?.asResponse()
)

fun EksterntVarselRow.asResponse() = EksterntVarselResponse(
    varselStatus = this.varselStatus.asResponse(),
    hendelseName = this.hendelseName.asResponse(),
    hendelseTimestamp = this.hendelseTimestamp,
    insertedTimestamp = this.insertedTimestamp,
    updatedTimestamp = this.updatedTimestamp
)

private fun VarselKilde.asResponse(): no.nav.paw.arbeidssoekerregisteret.api.models.VarselKilde = when (this) {
    VarselKilde.BEKREFTELSE_TILGJENGELIG -> no.nav.paw.arbeidssoekerregisteret.api.models.VarselKilde.BEKREFTELSE_TILGJENGELIG
    VarselKilde.PERIODE_AVSLUTTET -> no.nav.paw.arbeidssoekerregisteret.api.models.VarselKilde.PERIODE_AVSLUTTET
    VarselKilde.MANUELL_VARSLING -> no.nav.paw.arbeidssoekerregisteret.api.models.VarselKilde.MANUELL_VARSLING
    VarselKilde.UKJENT -> no.nav.paw.arbeidssoekerregisteret.api.models.VarselKilde.UKJENT
}

private fun VarselType.asResponse(): no.nav.paw.arbeidssoekerregisteret.api.models.VarselType = when (this) {
    VarselType.BESKJED -> no.nav.paw.arbeidssoekerregisteret.api.models.VarselType.BESKJED
    VarselType.OPPGAVE -> no.nav.paw.arbeidssoekerregisteret.api.models.VarselType.OPPGAVE
    VarselType.INNBOKS -> no.nav.paw.arbeidssoekerregisteret.api.models.VarselType.INNBOKS
    VarselType.UKJENT -> no.nav.paw.arbeidssoekerregisteret.api.models.VarselType.UKJENT
}

private fun VarselStatus.asResponse(): no.nav.paw.arbeidssoekerregisteret.api.models.VarselStatus = when (this) {
    VarselStatus.VENTER -> no.nav.paw.arbeidssoekerregisteret.api.models.VarselStatus.VENTER
    VarselStatus.SENDT -> no.nav.paw.arbeidssoekerregisteret.api.models.VarselStatus.SENDT
    VarselStatus.BESTILT -> no.nav.paw.arbeidssoekerregisteret.api.models.VarselStatus.BESTILT
    VarselStatus.FEILET -> no.nav.paw.arbeidssoekerregisteret.api.models.VarselStatus.FEILET
    VarselStatus.KANSELLERT -> no.nav.paw.arbeidssoekerregisteret.api.models.VarselStatus.KANSELLERT
    VarselStatus.FERDIGSTILT -> no.nav.paw.arbeidssoekerregisteret.api.models.VarselStatus.FERDIGSTILT
    VarselStatus.UKJENT -> no.nav.paw.arbeidssoekerregisteret.api.models.VarselStatus.UKJENT
}

private fun VarselEventName.asResponse(): HendelseName = when (this) {
    VarselEventName.OPPRETTET -> HendelseName.OPPRETTET
    VarselEventName.AKTIVERT -> HendelseName.AKTIVERT
    VarselEventName.INAKTIVERT -> HendelseName.INAKTIVERT
    VarselEventName.SLETTET -> HendelseName.SLETTET
    VarselEventName.EKSTERN_STATUS_OPPDATERT -> HendelseName.EKSTERN_STATUS_OPPDATERT
    VarselEventName.UKJENT -> HendelseName.UKJENT
}

fun BestillingRow.asResponse(
    varslerTotalt: Long,
    varslerSendt: Long,
    varslerFeilet: Long
): BestillingResponse = BestillingResponse(
    bestillingId = this.bestillingId,
    bestiller = this.bestiller,
    status = this.status.asResponse(),
    varslerTotalt = varslerTotalt,
    varslerSendt = varslerSendt,
    varslerFeilet = varslerFeilet,
    insertedTimestamp = this.insertedTimestamp,
    updatedTimestamp = this.updatedTimestamp
)

fun BestillingStatus.asResponse(): no.nav.paw.arbeidssoekerregisteret.api.models.BestillingStatus = when (this) {
    BestillingStatus.VENTER -> no.nav.paw.arbeidssoekerregisteret.api.models.BestillingStatus.VENTER
    BestillingStatus.BEKREFTET -> no.nav.paw.arbeidssoekerregisteret.api.models.BestillingStatus.BEKREFTET
    BestillingStatus.AKTIV -> no.nav.paw.arbeidssoekerregisteret.api.models.BestillingStatus.AKTIV
    BestillingStatus.SENDT -> no.nav.paw.arbeidssoekerregisteret.api.models.BestillingStatus.SENDT
    BestillingStatus.IGNORERT -> no.nav.paw.arbeidssoekerregisteret.api.models.BestillingStatus.IGNORERT
    BestillingStatus.FEILET -> no.nav.paw.arbeidssoekerregisteret.api.models.BestillingStatus.FEILET
}
