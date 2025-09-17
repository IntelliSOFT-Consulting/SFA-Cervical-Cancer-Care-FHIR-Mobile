package com.icl.cervicalcancercare.viewmodels

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.datacapture.mapping.ResourceMapper
import com.google.android.fhir.datacapture.validation.Invalid
import com.google.android.fhir.datacapture.validation.QuestionnaireResponseValidator
import com.google.android.fhir.get
import com.google.android.fhir.search.search
import com.ibm.icu.text.SimpleDateFormat
import com.icl.cervicalcancercare.fhir.FhirApplication
import com.icl.cervicalcancercare.models.QuestionnaireAnswer
import com.icl.cervicalcancercare.network.FormatterClass
import com.icl.cervicalcancercare.utils.readFileFromAssets
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.CodeableConcept
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Enumerations
import org.hl7.fhir.r4.model.Identifier
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Questionnaire
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.Resource
import org.json.JSONArray
import org.json.JSONObject
import java.util.Date
import java.util.UUID


class EditResponseViewModel(
    application: Application,
    private val questionnaireId: String,
    private val questionnaire: String
) :
    AndroidViewModel(application) {
    private val fhirEngine: FhirEngine = FhirApplication.fhirEngine(application.applicationContext)
    private val backgroundProcessingScope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO + CoroutineName("BackgroundProcessing")
    )
    val liveEditData = liveData { emit(prepareEditRecord()) }

    private suspend fun prepareEditRecord(): Pair<String, String> {
        // This is actually a QuestionnaireResponse, not a Patient
        val questionnaireResponse = fhirEngine.get<QuestionnaireResponse>(questionnaireId)

        // Read the original Questionnaire from assets
        val questionnaireJson =
            getApplication<Application>()
                .readFileFromAssets(questionnaire)
                .trimIndent()

        // Parse the Questionnaire
        val parser = FhirContext.forCached(FhirVersionEnum.R4).newJsonParser()
        val questionnaire =
            parser.parseResource(Questionnaire::class.java, questionnaireJson) as Questionnaire

        // Convert the existing QuestionnaireResponse to JSON string
        val questionnaireResponseJson = parser.encodeResourceToString(questionnaireResponse)

        return questionnaireJson to questionnaireResponseJson
    }


    val isResourcesSaved = MutableLiveData<Boolean>()

    /**
     * Update patient registration questionnaire response into the application database.
     *
     * @param questionnaireResponse patient registration questionnaire response
     */
    fun updatePatient(
        context: Context,
        questionnaireResponse: QuestionnaireResponse,
        questionnaire: Questionnaire,
        questionnaireResponseString: String
    ) {
        viewModelScope.launch {
            val resourceId = FormatterClass().getSharedPref("resourceId", context)
            if (QuestionnaireResponseValidator.validateQuestionnaireResponse(
                    questionnaire,
                    questionnaireResponse,
                    getApplication(),
                )
                    .values
                    .flatten()
                    .any { it is Invalid }
            ) {
                isResourcesSaved.value = false
                return@launch
            }

            val entry =
                ResourceMapper.extract(
                    questionnaire,
                    questionnaireResponse,
                ).entryFirstRep
            if (entry.resource !is Patient) {
                isResourcesSaved.value = false
                return@launch
            }

            val patient = entry.resource as Patient
            patient.id = resourceId
            patient.gender = Enumerations.AdministrativeGender.FEMALE
            val identifierSystem = Identifier()
            val typeCodeableConcept = CodeableConcept()
            val codingList = ArrayList<Coding>()
            val coding = Coding()
            coding.system = "system-creation"
            coding.code = "system_creation"
            coding.display = "System Creation"
            codingList.add(coding)
            typeCodeableConcept.coding = codingList

            identifierSystem.value = FormatterClass().formatCurrentDateTime(Date())
            identifierSystem.system = "system-creation"
            identifierSystem.type = typeCodeableConcept
            patient.identifier.add(identifierSystem)
            fhirEngine.update(patient)

            val subjectReference =
                Reference("Patient/$resourceId")
            questionnaireResponse.id = questionnaireId
            questionnaireResponse.subject = subjectReference
            questionnaireResponse.status =
                QuestionnaireResponse.QuestionnaireResponseStatus.COMPLETED
            fhirEngine.update(questionnaireResponse)
            isResourcesSaved.value = true
            launch(Dispatchers.IO) {
                extractLocationFromResponseJson(
                    fhirEngine,
                    "$resourceId",
                    questionnaireResponseString
                )
            }
            return@launch
        }
    }

    private fun extractLocationFromResponseJson(
        fhirEngine: FhirEngine,
        patientId: String,
        responseJson: String
    ) {
        try {
            val root = JSONObject(responseJson)
            val items = root.getJSONArray("item")

            var county: String? = null
            var subCounty: String? = null
            var ward: String? = null

            for (i in 0 until items.length()) {
                val prItem = items.getJSONObject(i)
                if (prItem.getString("linkId") == "PR") {
                    val prSubItems = prItem.getJSONArray("item")
                    for (j in 0 until prSubItems.length()) {
                        val subItem = prSubItems.getJSONObject(j)
                        if (subItem.getString("linkId") == "PR-address") {
                            val addressItems = subItem.getJSONArray("item")
                            for (k in 0 until addressItems.length()) {
                                val addressItem = addressItems.getJSONObject(k)
                                val linkId = addressItem.getString("linkId")
                                val answer = addressItem.getJSONArray("answer")
                                val valueReference =
                                    answer.getJSONObject(0).getJSONObject("valueReference")
                                val locationName = valueReference.getString("display")

                                when (linkId) {
                                    "county" -> county = locationName
                                    "sub_county" -> subCounty = locationName
                                    "ward" -> ward = locationName
                                }
                            }
                        }
                    }
                }
            }

            // TODO: You can save to DB or log to file or FHIR Location, etc.
            viewModelScope.launch {
                val searchResult =
                    fhirEngine.search<Patient> {
                        filter(Resource.RES_ID, { value = of(patientId) })
                    }
                if (searchResult.isNotEmpty()) {
                    searchResult.first().let {
                        val updatedPatient = it.resource.copy()
                        updatedPatient.id = patientId

                        updatedPatient.addressFirstRep.city = county
                        updatedPatient.addressFirstRep.district = subCounty
                        updatedPatient.addressFirstRep.state = ward

                        updatedPatient.addressFirstRep.addLine(ward)
                        updatedPatient.addressFirstRep.addLine(subCounty)
                        updatedPatient.addressFirstRep.addLine(county)

                        fhirEngine.update(updatedPatient)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            println("Registration Response ****** ${e.message}")
        }
    }

    private fun generateUuid(): String {
        return UUID.randomUUID().toString()
    }

    private fun startBackgroundProcessing(
        context: Context,
        questionnaireResponseString: String,
        questionnaire: String?
    ) {

        backgroundProcessingScope.launch {
            try {
                val patientId = FormatterClass().getSharedPref("patientId", context)
                val jsonObject = JSONObject(questionnaireResponseString)
                val extractedAnswers = extractStructuredAnswersOnlyFromItems(jsonObject)

                if (patientId != null) {
                    val patient = fhirEngine.get<Patient>(patientId)
                    val updatedPatient = patient.copy()
                    updatedPatient.id = patientId

                    fhirEngine.update(updatedPatient)
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun extractStructuredAnswersOnlyFromItems(json: JSONObject): List<QuestionnaireAnswer> {
        val results = mutableListOf<QuestionnaireAnswer>()

        fun processItems(items: JSONArray) {
            for (i in 0 until items.length()) {
                val item = items.getJSONObject(i)
                val linkId = item.optString("linkId", "")
                val text = item.optString("text", "")

                if (item.has("answer")) {
                    val answers = item.getJSONArray("answer")
                    val valueList = mutableListOf<String>()

                    for (j in 0 until answers.length()) {
                        val answerObj = answers.getJSONObject(j)

                        val value = when {
                            answerObj.has("valueString") -> answerObj.getString("valueString")
                            answerObj.has("valueInteger") -> answerObj.optString("valueInteger", "")
                            answerObj.has("valueDate") -> answerObj.optString("valueDate", "")
                            answerObj.has("valueDateTime") -> answerObj.optString(
                                "valueDateTime",
                                ""
                            )

                            answerObj.has("valueBoolean") -> answerObj.optString("valueBoolean", "")
                            answerObj.has("valueDecimal") -> answerObj.optString("valueDecimal", "")
                            answerObj.has("valueCoding") -> {
                                val coding = answerObj.getJSONObject("valueCoding")
                                coding.optString("display", coding.optString("code", ""))
                            }

                            answerObj.has("valueReference") -> {
                                val ref = answerObj.getJSONObject("valueReference")
                                ref.optString("display", ref.optString("reference", ""))
                            }

                            else -> null
                        }

                        if (!value.isNullOrBlank()) {
                            valueList.add(value)
                        }
                    }

                    if (valueList.isNotEmpty()) {
                        // Join multiple values with comma
                        results.add(QuestionnaireAnswer(linkId, text, valueList.joinToString(", ")))
                    }
                }

                if (item.has("item")) {
                    processItems(item.getJSONArray("item"))
                }
            }
        }

        if (json.has("item")) {
            processItems(json.getJSONArray("item"))
        }
        return results
    }


}

