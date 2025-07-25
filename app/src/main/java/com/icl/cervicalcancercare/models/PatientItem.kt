package com.icl.cervicalcancercare.models

import com.icl.cervicalcancercare.R
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import java.time.LocalDate

data class Login(
    val username: String,
    val password: String,
)

data class LoginResponse(
    val access_token: String,
    val token_type: String
)


data class DataProcessingResponse(
    val response: String,
)

enum class UrlData(var message: Int) {
    BASE_URL(R.string.base_url),
}
data class PieItem(
    val value: String,
    val label: String,
    val color: String
)

data class ExtractedData(
    val patient_age: Int?,
    val parity: Int?,
    val menopausal_status: String?,
    val hiv_status: String?,
    val art_adherence: String?,
    val screening_history: Map<String, String>,
    val clinical_findings: Map<String, Any>,
    val comorbidities: List<String>,
    val medications: List<String>,
    val allergies: List<String>,
    val prior_treatment: Map<String, Boolean>,
    val user_question: String?
)

data class ObservationItem(
    val id: String,
    val code: String,
    val effective: String,
    val value: String,
) {
    override fun toString(): String = code
}

data class ConditionItem(
    val id: String,
    val code: String,
    val effective: String,
    val value: String,
) {
    override fun toString(): String = code
}

data class PatientItem(
    val id: String,
    val resourceId: String,
    val name: String,
    val gender: String,
    val dob: LocalDate? = null,
    val phone: String,
    val email: String,
    val city: String,
    val country: String,
    val isActive: Boolean,
    val html: String,
    val identificationType: String? = null,
    val identificationNumber: String? = null
) {
    override fun toString(): String = name
}

data class PatientSummary(
    val basic: PatientItem?,
    val impressions: List<PatientImpression> = emptyList()
)

//@Serializable
data class PatientImpression(
    val status: String,
    val summary: String,
    val basis: List<String> = emptyList()
)