package no.nav.paw.kafkakeygenerator

import java.util.regex.Pattern

private val mønster: Pattern = Pattern.compile("""(\D|^)(\d{11})(\D|$)""")

fun masker(verdi: Any?): String =
    mønster.matcher(verdi.toString())
        .replaceAll("$1***********$3")