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
import com.icl.cervicalcancercare.patients.AddPatientActivity.Companion.QUESTIONNAIRE_FILE_PATH_KEY
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.ClinicalImpression
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.Reference
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
        dataSource: ExtractedData
    ) {
        viewModelScope.launch {

            val subjectReference = Reference("Patient/$patientId")
            val data = ClinicalImpression()
            data.subject = subjectReference
            data.summary = dataSource.user_question
            data.status = ClinicalImpression.ClinicalImpressionStatus.COMPLETED
            input.forEach { (key, value) ->
                Log.d("Server Response Recommendation", "Key: $key, Value: $value")
                val basis = key + "\n" + value
                val finding = ClinicalImpression.ClinicalImpressionFindingComponent()
                finding.basis = basis
                data.addFinding(finding)

            }

            fhirEngine.create(data)
        }
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