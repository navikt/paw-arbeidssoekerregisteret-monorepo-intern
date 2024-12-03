package no.nav.paw.kafkakeygenerator.api.recordkey.functions

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.http.HttpStatusCode.Companion.NotFound
import io.ktor.http.HttpStatusCode.Companion.OK
import no.nav.paw.kafkakeygenerator.api.recordkey.FailureResponseV1
import no.nav.paw.kafkakeygenerator.api.recordkey.Feilkode
import no.nav.paw.kafkakeygenerator.api.recordkey.RecordKeyLookupResponseV1
import no.nav.paw.kafkakeygenerator.api.recordkey.recordKeyLookupResponseV1
import no.nav.paw.kafkakeygenerator.api.v2.publicTopicKeyFunction
import no.nav.paw.kafkakeygenerator.vo.ArbeidssoekerId
import no.nav.paw.kafkakeygenerator.vo.CallId
import no.nav.paw.kafkakeygenerator.vo.Either
import no.nav.paw.kafkakeygenerator.vo.Failure
import no.nav.paw.kafkakeygenerator.vo.FailureCode
import no.nav.paw.kafkakeygenerator.vo.Identitetsnummer
import no.nav.paw.kafkakeygenerator.vo.left
import no.nav.paw.kafkakeygenerator.vo.right
import org.slf4j.LoggerFactory

class GetRecordKeyTest : FreeSpec({
    val logger = LoggerFactory.getLogger("test")
    val callId = CallId("callId")
    val arbeidssoeker1 = Identitetsnummer("12345678901") to ArbeidssoekerId(1)
    val arbeidssoeker2 = Identitetsnummer("12345678902") to ArbeidssoekerId(2)
    val map = mapOf(arbeidssoeker1, arbeidssoeker2)
    suspend fun mockFunction(callId: CallId, identitetsnummer: Identitetsnummer): Either<Failure, ArbeidssoekerId> {
        return map[identitetsnummer]?.let(::right) ?: left(Failure("feil", FailureCode.DB_NOT_FOUND, null))
    }
    "Når det finnes en arbeidssøkerId for en identitetsnummer, returneres tilhørende record key" {
        ::mockFunction.recordKey(
            logger = logger,
            callId = callId,
            ident = arbeidssoeker1.first
        ) should { result ->
            result.first shouldBe OK
            result.second.shouldBeInstanceOf<RecordKeyLookupResponseV1>() shouldBe recordKeyLookupResponseV1(
                publicTopicKeyFunction(
                    arbeidssoeker1.second
                )
            )
        }
    }

    "Når det ikke finnes en arbeidssøkerId for en identitetsnummer, returneres en feil, med kode ${Feilkode.UKJENT_REGISTERET}" {
        ::mockFunction.recordKey(
            logger = logger,
            callId = callId,
            ident = Identitetsnummer("1111111111111")
        ) should { result ->
            result.first shouldBe NotFound
            result.second.shouldBeInstanceOf<FailureResponseV1>().code shouldBe Feilkode.UKJENT_REGISTERET
        }
    }
})