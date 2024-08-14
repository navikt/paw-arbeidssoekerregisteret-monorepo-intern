package no.nav.paw.kafkakeygenerator.api.v2

//Endring av denne verdien krever replay av eventlog til nye topics!!
const val PUBLIC_KEY_MODULO_VALUE = 7_500

//Endring av denne funksjonen krever replay av eventlog til nye topics!!
fun publicTopicKeyFunction(internalKey: Long): Long =
    "internal_key_$internalKey".hashCode().toLong() % PUBLIC_KEY_MODULO_VALUE
