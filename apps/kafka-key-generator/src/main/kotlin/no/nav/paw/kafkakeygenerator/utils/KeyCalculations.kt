package no.nav.paw.kafkakeygenerator.utils

import no.nav.paw.felles.model.ArbeidssoekerId
import no.nav.paw.felles.model.RecordKey

//Endring av denne verdien krever replay av eventlog til nye topics!!
const val PUBLIC_KEY_MODULO_VALUE = 7_500

//Endring av denne funksjonen krever replay av eventlog til nye topics!!
fun publicTopicKeyFunction(arbeidssoekerId: ArbeidssoekerId): RecordKey =
    RecordKey("internal_key_${arbeidssoekerId.value}".hashCode().toLong() % PUBLIC_KEY_MODULO_VALUE)

fun Long.asRecordKey(): Long = publicTopicKeyFunction(ArbeidssoekerId(this)).value
