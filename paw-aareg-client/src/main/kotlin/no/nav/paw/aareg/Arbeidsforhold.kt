package no.nav.paw.aareg

import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.LocalDateTime

@Serializable
data class Arbeidsforhold(
    val arbeidsgiver: Arbeidsgiver,
    val opplysningspliktig: Opplysningspliktig,
    val arbeidsavtaler: List<Arbeidsavtale>,
    val ansettelsesperiode: Ansettelsesperiode,
    @Serializable(with = LocalDateTimeSerializer::class)
    val registrert: LocalDateTime
)

@Serializable
data class Arbeidsavtale(
    val stillingsprosent: Double?,
    val gyldighetsperiode: Periode
)

@Serializable
data class Ansettelsesperiode(
    val periode: Periode
)

@Serializable
data class Arbeidsgiver(
    val type: String,
    val organisasjonsnummer: String?
)

@Serializable
data class Periode(
    @Serializable(with = LocalDateSerializer::class)
    val fom: LocalDate,
    @Serializable(with = LocalDateSerializer::class)
    val tom: LocalDate? = null
)

@Serializable
data class Opplysningspliktig(
    val type: String,
    val organisasjonsnummer: String?
)
