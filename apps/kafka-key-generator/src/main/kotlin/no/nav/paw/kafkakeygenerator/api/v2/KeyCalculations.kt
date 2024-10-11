package no.nav.paw.kafkakeygenerator.api.v2

import no.nav.paw.kafkakeygenerator.vo.ArbeidssoekerId
import no.nav.paw.kafkakeygenerator.vo.RecordKey

//Endring av denne verdien krever replay av eventlog til nye topics!!
const val PUBLIC_KEY_MODULO_VALUE = 7_500

//Endring av denne funksjonen krever replay av eventlog til nye topics!!
fun publicTopicKeyFunction(arbeidssoekerId: ArbeidssoekerId): RecordKey =
    RecordKey("internal_key_${arbeidssoekerId.value}".hashCode().toLong() % PUBLIC_KEY_MODULO_VALUE)
