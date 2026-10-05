package no.nav.paw.test.openapi

import com.atlassian.oai.validator.OpenApiInteractionValidator
import com.atlassian.oai.validator.model.Request
import com.atlassian.oai.validator.model.SimpleResponse
import com.atlassian.oai.validator.report.LevelResolver
import com.atlassian.oai.validator.report.ValidationReport
import io.ktor.client.call.save
import io.ktor.client.plugins.api.Send
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.request
import io.ktor.http.contentType

/**
 * Ktor-klientplugin for tester som validerer hvert svar mot én eller flere OpenAPI-spesifikasjoner.
 *
 * Brukes sammen med `testApplication`:
 * ```
 * createClient {
 *     install(OpenApiValidering) { spesifikasjon("src/main/resources/openapi/documentation.yaml") }
 * }
 * ```
 * Svar på stier som ikke finnes i noen spesifikasjon, valideres ikke (f.eks. testruter).
 * Avvik fra spesifikasjonen gir [AssertionError], slik at testen feiler.
 */
val OpenApiValidering = createClientPlugin("OpenApiValidering", ::OpenApiValideringConfig) {
    val validatorer = pluginConfig.spesifikasjoner.map(::byggValidator)
    val kjenteAvvik = pluginConfig.kjenteAvvik.toList()
    require(validatorer.isNotEmpty()) { "OpenApiValidering krever minst én spesifikasjon" }
    on(Send) { request ->
        val call = proceed(request).save()
        call.response.validerMotOpenApi(validatorer, kjenteAvvik)
        call
    }
}

class OpenApiValideringConfig {
    internal val spesifikasjoner = mutableListOf<String>()
    internal val kjenteAvvik = mutableListOf<KjentAvvik>()

    /** Sti til spesifikasjonen, relativt til modulens prosjektmappe eller som classpath-ressurs. */
    fun spesifikasjon(sti: String) {
        spesifikasjoner += sti
    }

    /** Avvik som er kjent og akseptert inntil videre. Se [KjentAvvik]. */
    fun kjentAvvik(avvik: KjentAvvik) {
        kjenteAvvik += avvik
    }

    fun kjenteAvvik(avvik: List<KjentAvvik>) {
        kjenteAvvik += avvik
    }
}

/**
 * Et avvik mellom svar og spesifikasjon som ignoreres fordi det fantes før valideringen ble innført.
 * Skal fjernes når koden eller spesifikasjonen er rettet.
 *
 * @param sti regulært uttrykk som må matche hele stien i forespørselen
 * @param noekkel valideringsnøkkelen fra swagger-request-validator, f.eks. `validation.response.status.unknown`
 */
data class KjentAvvik(
    val metode: String,
    val sti: Regex,
    val status: Int,
    val noekkel: String,
    val begrunnelse: String
) {
    constructor(metode: String, sti: String, status: Int, noekkel: String, begrunnelse: String) :
        this(metode, Regex(sti), status, noekkel, begrunnelse)

    internal fun gjelder(metode: String, sti: String, status: Int, noekkel: String): Boolean =
        this.metode.equals(metode, ignoreCase = true) &&
            this.sti.matches(sti) &&
            this.status == status &&
            this.noekkel == noekkel
}

fun byggValidator(spesifikasjon: String): OpenApiInteractionValidator =
    OpenApiInteractionValidator
        .createForSpecificationUrl(spesifikasjon)
        .withLevelResolver(
            LevelResolver.create()
                .withLevel("validation.request.path.missing", ValidationReport.Level.IGNORE)
                .build()
        )
        .build()

suspend fun HttpResponse.validerMotOpenApi(
    validatorer: List<OpenApiInteractionValidator>,
    kjenteAvvik: List<KjentAvvik> = emptyList()
) {
    val metode = Request.Method.valueOf(request.method.value.uppercase())
    val sti = request.url.encodedPath
    val body = bodyAsBytes()
    val feil = validatorer.flatMap { validator ->
        val svar = SimpleResponse.Builder(status.value)
            .withContentType(contentType()?.toString())
            .apply { if (body.isNotEmpty()) withBody(body.inputStream()) }
            .build()
        validator.validateResponse(sti, metode, svar).messages
            .filter { it.level == ValidationReport.Level.ERROR }
            .filterNot { melding ->
                kjenteAvvik.any { it.gjelder(metode.name, sti, status.value, melding.key) }
            }
    }
    if (feil.isNotEmpty()) {
        throw AssertionError(
            "Svar på $metode $sti (${status.value}) følger ikke OpenAPI-spesifikasjonen:\n" +
                feil.joinToString("\n") { " - [${it.key}] ${it.message}" }
        )
    }
}
