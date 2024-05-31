package no.nav.paw.arbeidssokerregisteret.app.helse

import io.kotest.core.spec.style.FreeSpec
import java.util.jar.Manifest

class ApplicationInfoKtTest: FreeSpec({
    "getModelInfo" - {
        "should return ModelInfo" {
            val manifest = manifestAsString.byteInputStream().use(::Manifest)
            println(manifest.mainAttributes
                .getValue("Implementation-Title"))
            println(manifest.mainAttributes
                .getValue("Arbeidssokerregisteret-Modul"))
        }
    }
})

val manifestAsString = """
    Manifest-Version: 1.0
    Implementation-Version: 1.0-SNAPSHOT
    Implementation-Title: main-avro-schema
    Arbeidssokerregisteret-Modul: avro-schema
    build-timestamp: 2024-04-10T04:31:09.382249184Z
""".trimIndent()