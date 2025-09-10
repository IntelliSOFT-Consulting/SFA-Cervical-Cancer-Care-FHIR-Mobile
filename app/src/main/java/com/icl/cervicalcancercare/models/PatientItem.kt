package com.icl.cervicalcancercare.models

import com.icl.cervicalcancercare.R
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import org.hl7.fhir.r4.model.Location
import java.time.LocalDate

data class Login(
    val username: String,
    val password: String,
)

data class LoginResponse(
    val access_token: String,
    val token_type: String
)

data class FhirBundle(
    val resourceType: String,
    val id: String,
    val type: String,
    val link: List<FhirLink>?,
    val entry: List<FhirEntry>?
)

data class FhirLink(
    val relation: String,
    val url: String
)

data class FhirEntry(
    val fullUrl: String,
    val resource: LocationResource,
    val search: SearchInfo
)

data class LocationResource(
    val resourceType: String,
    val id: String,
    val meta: MetaResponse,
    val name: String,
    val type: List<LocationType>,
    val partOf: PartOf? = null // optional
)

data class MetaResponse(
    val versionId: String,
    val lastUpdated: String,
    val source: String
)

data class LocationType(
    val coding: List<LocationCoding>
)

data class LocationCoding(
    val system: String,
    val code: String,
    val display: String
)

data class PartOf(
    val reference: String,
    val display: String
)

data class SearchInfo(
    val mode: String
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
    val patient_age: String?,
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
    val isActive: Boolean,
    val html: String,
    val identificationType: String? = null,
    val identificationNumber: String? = null,
    val county: String? = "",
    val sub_county: String? = "",
    val ward: String? = ""
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
    val basis: List<String> = emptyList(),
    val updatedData: List<Reco> = emptyList()
)

data class Reco(
    val question: String,
    val answer: String
)

data class Payload(
    val meta: Meta,
    val client_facility: ClientFacility,
    val client_identification: ClientIdentification,
    val family_history: FamilyHistory,
    val personal_history: PersonalHistory,
    val ncd_risk_factors: NcdRiskFactors,
    val reproductive_health: ReproductiveHealth,
    val hiv: Hiv,
    val measurements: Measurements,
    val cervical_screening: CervicalScreening,
    val breast_screening: BreastScreening,
    val clinical_findings: ClinicalFindings,
    val medications_allergies: MedicationsAllergies,
    val prior_treatment: PriorTreatment,
    val llm_request: LlmRequest
)

data class Meta(val submitted_at: String, val source_app_version: String)

data class ClientFacility(
    val date: String?,
    val county: String?,
    val sub_county: String?,
    val facility_name: String?,
    val service_provider_name: String?
)

data class ClientIdentification(
    val patient_id: String,
    val full_name: String?,
    val age_years: String,
    val phone_number: String?,
    val residence: Residence
)

data class Residence(val county: String?, val sub_county: String?, val ward: String?)

data class FamilyHistory(
    val breast_cancer: String,
    val hypertension: String,
    val diabetes: String,
    val mental_health_disorders: String,
    val notes: String?
)

data class PersonalHistory(
    val hypertension: Diagnosis,
    val diabetes: Diagnosis
)

data class Diagnosis(val diagnosed: String, val on_treatment: String)

data class NcdRiskFactors(val smoking: String, val alcohol: String)

data class ReproductiveHealth(
    val gravida: String?,
    val parity: String,
    val age_at_first_sex: String?,
    val contraception: Contraception,
    val number_of_sex_partners: String?,
    val menopausal_status: String
)

data class Contraception(val uses_contraception: String, val method: String?)

data class Hiv(
    val status: String,
    val on_art: String,
    val art_start_date: String?,
    val adherence: String
)

data class Measurements(
    val weight_kg: Double?,
    val height_cm: Double?,
    val bmi: Double?,
    val waist_circumference_cm: Double?,
    val bp: BloodPressure
)

data class BloodPressure(
    val reading_1: BpReading,
    val reading_2: BpReading
)

data class BpReading(val systolic: String?, val diastolic: String?)

data class CervicalScreening(
    val type_of_visit: String,
    val hpv_testing: HpvTesting,
    val via_testing: ViaTesting,
    val pap_smear: PapSmear,
    val pre_cancer_treatment: PreCancerTreatment
)

data class HpvTesting(
    val done: String,
    val sample_date: String?,
    val self_sampling: String,
    val result: String,
    val action: String
)

data class ViaTesting(val done: String, val result: String, val action: String)
data class PapSmear(val done: String, val result: String, val action: String)

data class PreCancerTreatment(
    val cryotherapy: TreatmentStatus,
    val thermal_ablation: TreatmentStatus,
    val leep: TreatmentStatus
)

data class TreatmentStatus(
    val status: String,
    val single_visit_approach: String,
    val if_not_done: String?,
    val postponed_reason: String?
)

data class BreastScreening(
    val cbe: String,
    val ultrasound: BreastExam,
    val mammography: BreastExam,
    val action: String
)

data class BreastExam(val done: String, val birads: String?)
data class BreastAction(val referred: String?, val follow_up: String?)

data class ClinicalFindings(
    val presenting_symptoms: List<String>,
    val lesion_visible: Boolean,
    val lesion_description: String,
    val cancer_stage: String
)

data class MedicationsAllergies(
    val comorbidities: List<String>,
    val current_medications: List<String>,
    val allergies: List<String>
)

data class PriorTreatment(
    val cryotherapy: Boolean,
    val leep: Boolean,
    val radiation: Boolean,
    val chemotherapy: Boolean
)

data class LlmRequest(val use_case: String, val user_question: String)