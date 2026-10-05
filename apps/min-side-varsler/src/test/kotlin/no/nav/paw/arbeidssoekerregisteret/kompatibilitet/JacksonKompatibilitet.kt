package no.nav.paw.arbeidssoekerregisteret.kompatibilitet

import java.nio.file.Files
import java.nio.file.Path

private const val GENERER_TESTDATA = "GENERER_JACKSON_TESTDATA"
private const val MAPPE = "jackson-kompatibilitet"

/**
 * Leser JSON som er lagret i src/test/resources/jackson-kompatibilitet.
 *
 * Filene er generert med Jackson 2-utgaven av koden og skal ikke genereres på nytt
 * etter oppgradering til Jackson 3. Testene som bruker dem, sjekker at data skrevet
 * av gammel kode fortsatt kan leses.
 *
 * Generer filene (kun på Jackson 2):
 * GENERER_JACKSON_TESTDATA=true ./gradlew :<modul>:test --rerun
 */
fun lagretJson(navn: String, serialiser: () -> ByteArray): ByteArray {
    val fil = "$MAPPE/$navn.json"
    if (System.getenv(GENERER_TESTDATA) == "true") {
        val json = serialiser()
        val sti = Path.of("src/test/resources", fil)
        Files.createDirectories(sti.parent)
        Files.write(sti, json)
        return json
    }
    return requireNotNull(Thread.currentThread().contextClassLoader.getResourceAsStream(fil)) {
        "Fant ikke $fil. Generer den med $GENERER_TESTDATA=true på Jackson 2-utgaven av koden."
    }.use { it.readBytes() }
}
