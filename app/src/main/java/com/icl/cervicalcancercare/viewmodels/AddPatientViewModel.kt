package com.icl.cervicalcancercare.viewmodels

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.datacapture.mapping.ResourceMapper
import com.google.android.fhir.datacapture.validation.Invalid
import com.google.android.fhir.datacapture.validation.QuestionnaireResponseValidator
import com.icl.cervicalcancercare.fhir.FhirApplication
import com.icl.cervicalcancercare.models.ExtractedData
import com.icl.cervicalcancercare.network.FormatterClass
import com.icl.cervicalcancercare.patients.AddPatientActivity.Companion.QUESTIONNAIRE_FILE_PATH_KEY
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.BooleanType
import org.hl7.fhir.r4.model.ClinicalImpression
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Encounter
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.IntegerType
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.StringType
import java.util.Date
import java.util.UUID

class AddPatientViewModel(application: Application, private val state: SavedStateHandle) :
    AndroidViewModel(application) {

    private var _questionnaireJson: String? = null
    val questionnaireJson: String
        get() = fetchQuestionnaireJson()

    val isPatientSaved = MutableLiveData<Boolean>()

    private val questionnaire: Questionnaire
        get() =
            FhirContext.forCached(FhirVersionEnum.R4).newJsonParser()
                .parseResource(questionnaireJson)
                    as Questionnaire

    private var fhirEngine: FhirEngine = FhirApplication.fhirEngine(application.applicationContext)

    /**
     * Saves patient registration questionnaire response into the application database.
     *
     * @param questionnaireResponse patient registration questionnaire response
     */


    fun createRecommendations(
        input: Map<String, String>,
        patientId: String,
        dataSource: ExtractedData,
        encounterId: String
    ) {
        viewModelScope.launch {

            val subjectReference = Reference("Patient/$patientId")
            val encounterReference = Reference("Encounter/$encounterId")
            val data = ClinicalImpression()
            data.subject = subjectReference
            data.encounter = encounterReference
            data.summary = dataSource.user_question
            data.status = ClinicalImpression.ClinicalImpressionStatus.COMPLETED
            input.forEach { (key, value) ->
                val basis = key + "\n" + value
                val finding = ClinicalImpression.ClinicalImpressionFindingComponent()
                finding.basis = basis
                data.addFinding(finding)

            }

            fhirEngine.create(data)
        }
    }

    fun createPatientObservations(context: Context, result: ExtractedData, encounterId: String) {
        val resourceId =
            FormatterClass().getSharedPref("resourceId", context)
        viewModelScope.launch {

            val identifierSystem0 = Identifier()
            val typeCodeableConcept0 = CodeableConcept()
            val codingList0 = ArrayList<Coding>()
            val coding0 = Coding()
            coding0.system = "system-creation"
            coding0.code = "system_creation"
            coding0.display = "System Creation"
            codingList0.add(coding0)
            typeCodeableConcept0.coding = codingList0
            typeCodeableConcept0.text = FormatterClass().formatCurrentDateTime(Date())

            identifierSystem0.value = FormatterClass().formatCurrentDateTime(Date())
            identifierSystem0.system = "system-creation"
            identifierSystem0.type = typeCodeableConcept0

            val subjectReference = Reference("Patient/$resourceId")
            val enc = Encounter()
            enc.subject = subjectReference
            enc.id = encounterId
            enc.status = Encounter.EncounterStatus.INPROGRESS
            enc.reasonCodeFirstRep.codingFirstRep.code = "assessment"
            enc.identifier.add(identifierSystem0)

            fhirEngine.create(enc)

            val observations: List<Observation> = createObservations(result)
            observations.forEach {
                println("Observation Payload: ${it.code.text}  → ${it.value}")
                val ob = it
                ob.subject = subjectReference
                ob.encounter = Reference("Encounter/$encounterId")
                fhirEngine.create(it)
            }
        }
    }


    fun createObservations(data: ExtractedData): List<Observation> {
        val observations = mutableListOf<Observation>()

        fun createObservation(codeText: String, categoryCode: String, value: Any?): Observation? {
            if (value == null) return null

            val obs = Observation().apply {
                id = generateUuid()
                status = Observation.ObservationStatus.FINAL
                category = listOf(
                    CodeableConcept().apply {
                        coding = listOf(
                            Coding().apply {
                                system = "http://hl7.org/fhir/observation-category"
                                code = categoryCode
                            }
                        )
                    }
                )
                code = CodeableConcept().apply {
                    coding.apply {
                        add(
                            Coding().apply {
                                system = "http://hl7.org/fhir/observation-category"
                                code = categoryCode
                            }
                        )
                    }
                    text = codeText
                }

                this.value = when (value) {
                    is String -> StringType(value)
                    is Int -> IntegerType(value)
                    is Boolean -> BooleanType(value)
                    is List<*> -> StringType(value.joinToString(", ") { it.toString() })
                    else -> StringType(value.toString())
                }
            }

            return obs
        }

        with(data) {
            createObservation(
                "Patient Age",
                "patient_age",
                patient_age
            )?.let { observations.add(it) }
            createObservation("Parity", "parity", parity)?.let { observations.add(it) }
            createObservation(
                "Menopausal Status",
                "menopausal_status",
                menopausal_status
            )?.let { observations.add(it) }
            createObservation("HIV Status", "hiv_status", hiv_status)?.let { observations.add(it) }
            createObservation(
                "ART Adherence",
                "art_adherence",
                art_adherence
            )?.let { observations.add(it) }
            createObservation(
                "Comorbidities",
                "comorbidities",
                comorbidities
            )?.let { observations.add(it) }
            createObservation(
                "Medications",
                "medications",
                medications
            )?.let { observations.add(it) }
            createObservation("Allergies", "allergies", allergies)?.let { observations.add(it) }
            createObservation(
                "User Question",
                "user_question",
                user_question
            )?.let { observations.add(it) }

            screening_history.forEach { (key, value) ->
                createObservation(
                    key.replaceFirstChar { it.uppercaseChar() },
                    "screening_history",
                    value
                )?.let {
                    observations.add(it)
                }
            }

            clinical_findings.forEach { (key, value) ->
                val label = key.replace("_", " ").replaceFirstChar { it.uppercaseChar() }
                createObservation(label, "clinical_findings", value)?.let { observations.add(it) }
            }

            prior_treatment.forEach { (key, value) ->
                val label = "Prior ${key.replace("_", " ").replaceFirstChar { it.uppercaseChar() }}"
                createObservation(label, "prior_treatment", value)?.let { observations.add(it) }
            }
        }

        return observations
    }

    fun savePatientData(
        questionnaireResponse: QuestionnaireResponse,
        questionnaireResponseString: String,
        context: Context
    ) {
        viewModelScope.launch {
            if (QuestionnaireResponseValidator.validateQuestionnaireResponse(
                    questionnaire,
                    questionnaireResponse,
                    getApplication(),
                )
                    .values
                    .flatten()
                    .any { it is Invalid }
            ) {
                isPatientSaved.value = false
                return@launch
            }

            val entry =
                ResourceMapper.extract(
                    questionnaire,
                    questionnaireResponse,
                )
                    .entryFirstRep
            if (entry.resource !is Patient) {
                isPatientSaved.value = false
                return@launch
            }
            val patient = entry.resource as Patient
            patient.id = generateUuid()
            fhirEngine.create(patient)
            isPatientSaved.value = true


        }
    }


    private fun fetchQuestionnaireJson(): String {
        _questionnaireJson?.let {
            return it
        }
        _questionnaireJson =
            getApplication<Application>().assets.open(state[QUESTIONNAIRE_FILE_PATH_KEY]!!)
                .bufferedReader().use { it.readText() }
        return _questionnaireJson!!
    }

    private fun generateUuid(): String {
        return UUID.randomUUID().toString()
    }
}