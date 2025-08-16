package com.icl.cervicalcancercare.assessment

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import com.google.android.fhir.datacapture.QuestionnaireFragment
import com.google.android.material.button.MaterialButton
import com.google.gson.Gson
import com.icl.cervicalcancercare.R
import com.icl.cervicalcancercare.databinding.ActivityAssessmentBinding
import com.icl.cervicalcancercare.models.BloodPressure
import com.icl.cervicalcancercare.models.BpReading
import com.icl.cervicalcancercare.models.BreastAction
import com.icl.cervicalcancercare.models.BreastExam
import com.icl.cervicalcancercare.models.BreastScreening
import com.icl.cervicalcancercare.models.CervicalScreening
import com.icl.cervicalcancercare.models.ClientFacility
import com.icl.cervicalcancercare.models.ClientIdentification
import com.icl.cervicalcancercare.models.ClinicalFindings
import com.icl.cervicalcancercare.models.Contraception
import com.icl.cervicalcancercare.models.Diagnosis
import com.icl.cervicalcancercare.models.ExtractedData
import com.icl.cervicalcancercare.models.FamilyHistory
import com.icl.cervicalcancercare.models.Hiv
import com.icl.cervicalcancercare.models.HpvTesting
import com.icl.cervicalcancercare.models.LlmRequest
import com.icl.cervicalcancercare.models.Measurements
import com.icl.cervicalcancercare.models.MedicationsAllergies
import com.icl.cervicalcancercare.models.Meta
import com.icl.cervicalcancercare.models.NcdRiskFactors
import com.icl.cervicalcancercare.models.PapSmear
import com.icl.cervicalcancercare.models.Payload
import com.icl.cervicalcancercare.models.PersonalHistory
import com.icl.cervicalcancercare.models.PreCancerTreatment
import com.icl.cervicalcancercare.models.PriorTreatment
import com.icl.cervicalcancercare.models.ReproductiveHealth
import com.icl.cervicalcancercare.models.Residence
import com.icl.cervicalcancercare.models.TreatmentStatus
import com.icl.cervicalcancercare.models.ViaTesting
import com.icl.cervicalcancercare.network.FormatterClass
import com.icl.cervicalcancercare.network.RetrofitCallsAuthentication
import com.icl.cervicalcancercare.utils.Functions
import com.icl.cervicalcancercare.utils.ProgressDialogManager
import com.icl.cervicalcancercare.viewmodels.AddPatientViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.QuestionnaireResponse
import org.json.JSONObject
import java.io.File
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.getValue

class AssessmentActivity : AppCompatActivity() {
    private val viewModel: AddPatientViewModel by viewModels()
    private var retrofitCallsAuthentication = RetrofitCallsAuthentication()
    private lateinit var binding:
            ActivityAssessmentBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAssessmentBinding.inflate(layoutInflater)
        setContentView(binding.root)
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
//            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
//            insets
//        }

        setSupportActionBar(binding.toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val titleName = Functions().getSharedPref("AddParentTitle", this@AssessmentActivity)
        supportActionBar.apply { title = titleName }


        updateArguments()
        if (savedInstanceState == null) {
            addQuestionnaireFragment()
        }
        observePatientSaveAction()

        supportFragmentManager.setFragmentResultListener(
            QuestionnaireFragment.SUBMIT_REQUEST_KEY,
            this@AssessmentActivity,
        ) { _, _ ->
            onSubmitAction()
        }
        supportFragmentManager.setFragmentResultListener(
            QuestionnaireFragment.CANCEL_REQUEST_KEY,
            this@AssessmentActivity,
        ) { _, _ ->
            onBackPressed()
        }
    }

    fun extractedResponse(flattened: MutableList<Pair<String, String>>, key: String): String {
        return flattened.firstOrNull { it.first == key }?.second
            ?: ""
    }

    private fun onSubmitAction() {
        lifecycleScope.launch {
            val questionnaireFragment =
                supportFragmentManager.findFragmentByTag(QUESTIONNAIRE_FRAGMENT_TAG)
                        as QuestionnaireFragment

            val questionnaireResponse = questionnaireFragment.getQuestionnaireResponse()
            // Print the response to the log
            val jsonParser = FhirContext.forCached(FhirVersionEnum.R4).newJsonParser()
            val questionnaireResponseString =
                jsonParser.encodeResourceToString(questionnaireResponse)
            Log.e("Response", questionnaireResponseString)
            println("Response $questionnaireResponseString")
//            extractClinicalDataAsync(questionnaireResponseString)
            File(filesDir, "response.json").writeText(questionnaireResponseString)

            val flattened = mutableListOf<Pair<String, String>>()
            flattenResponseItems(questionnaireResponse.item, flattened)
            flattened.forEach { (linkId, value) ->
                println("Flatten:::: $linkId -> $value")
            }
            val nowUtc = OffsetDateTime.now(ZoneOffset.UTC)
            val isoString = nowUtc.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

            Log.d("Date", isoString)
            val payload = Payload(
                meta = Meta(
                    submitted_at = isoString,
                    source_app_version = "demo-1.0.0"
                ),
                client_facility = ClientFacility(
                    date = extractedResponse(flattened, "date"),
                    county = extractedResponse(flattened, "county"),
                    sub_county = extractedResponse(flattened, "sub_county"),
                    facility_name = extractedResponse(flattened, "facility_name"),
                    service_provider_name = extractedResponse(flattened, "provider_name")
                ),
                client_identification = ClientIdentification(
                    patient_id = Functions().getSharedPref(
                        "resourceId",
                        this@AssessmentActivity
                    ) ?: "",
                    full_name = Functions().getSharedPref("full_name", this@AssessmentActivity)
                        ?: "",
                    age_years = calculatePatientAgeInYears(),
                    phone_number = Functions().getSharedPref("phone", this@AssessmentActivity)
                        ?: "",
                    residence = Residence(
                        county = null,
                        sub_county = null,
                        ward = null
                    )
                ),
                family_history = FamilyHistory(
                    breast_cancer = extractedResponse(flattened, "family_history_breast_cancer"),
                    hypertension = extractedResponse(flattened, "family_history_hypertension"),
                    diabetes = extractedResponse(flattened, "family_history_diabetes"),
                    mental_health_disorders = extractedResponse(
                        flattened,
                        "family_history_mental_health"
                    ),
                    notes = extractedResponse(flattened, "family_history_mental_health_specify")
                ),
                personal_history = PersonalHistory(
                    hypertension = Diagnosis(
                        diagnosed = extractedResponse(
                            flattened,
                            "personal_hypertension"
                        ),
                        on_treatment = extractedResponse(
                            flattened,
                            "personal_hypertension_treatment"
                        )
                    ),
                    diabetes = Diagnosis(
                        diagnosed = extractedResponse(
                            flattened,
                            "personal_diabetes"
                        ),
                        on_treatment = extractedResponse(flattened, "personal_diabetes_treatment")
                    )
                ),
                ncd_risk_factors = NcdRiskFactors(
                    smoking = extractedResponse(flattened, "smoking"),
                    alcohol = extractedResponse(flattened, "alcohol_consumption")
                ),
                reproductive_health = ReproductiveHealth(
                    gravida = extractedResponse(flattened, "number_of_pregnancies").toInt(),
                    parity = extractedResponse(flattened, "number_of_births").toInt(),
                    age_at_first_sex = extractedResponse(
                        flattened,
                        "age_first_sexual_intercourse"
                    ).toInt(),
                    contraception = Contraception(
                        uses_contraception = extractedResponse(flattened, "contraception"),
                        method = extractedResponse(flattened, "contraception_specify")
                    ),
                    number_of_sex_partners = extractedResponse(
                        flattened,
                        "number_of_sexual_partners"
                    ).toInt(),
                    menopausal_status = "pre-menopausal"
                ),
                hiv = Hiv(
                    status = extractedResponse(flattened, "hiv_status"),
                    on_art = extractedResponse(flattened, "on_arv_treatment"),
                    art_start_date = extractedResponse(flattened, "arv_start_date"),
                    adherence = "good"
                ),
                measurements = Measurements(
                    weight_kg = extractedResponse(flattened, "weight_kg").toDouble(),
                    height_cm = extractedResponse(flattened, "height_cm").toDouble(),
                    bmi = extractedResponse(flattened, "bmi").toDouble(),
                    waist_circumference_cm = extractedResponse(
                        flattened,
                        "waist_circumference_cm"
                    ).toDouble(),
                    bp = BloodPressure(
                        reading_1 = BpReading(
                            systolic = extractedResponse(
                                flattened,
                                "first_reading_systolic"
                            ).toInt(),
                            diastolic = extractedResponse(
                                flattened,
                                "first_reading_diastolic"
                            ).toInt()
                        ),
                        reading_2 = BpReading(
                            extractedResponse(
                                flattened,
                                "second_reading_systolic"
                            ).toInt(),
                            extractedResponse(flattened, "second_reading_diastolic").toInt()
                        )
                    )
                ),
                cervical_screening = CervicalScreening(
                    type_of_visit = extractedResponse(flattened, "type_of_visit"),
                    hpv_testing = HpvTesting(
                        done = extractedResponse(flattened, "hpv_testing_status"),
                        sample_date = extractedResponse(flattened, "hpv_sample_date"),
                        self_sampling = extractedResponse(flattened, "self_sampling"),
                        result = extractedResponse(
                            flattened, "hpv_results"
                        ),
                        action = listOf(extractedResponse(flattened, "hpv_action"))
                    ),
                    via_testing = ViaTesting(
                        done = extractedResponse(flattened, "via_testing_status"),
                        result = extractedResponse(flattened, "via_testing_results"),
                        action = listOf(extractedResponse(flattened, "via_testing_action"))
                    ),
                    pap_smear = PapSmear(
                        done = extractedResponse(flattened, "pap_smear_done"),
                        result = extractedResponse(flattened, "pap_smear_results"),
                        action = listOf(extractedResponse(flattened, "pap_smear_action"))
                    ),
                    pre_cancer_treatment = PreCancerTreatment(
                        cryotherapy = TreatmentStatus(
                            status = extractedResponse(
                                flattened,
                                "cryotherapy_status"
                            ),
                            single_visit_approach = extractedResponse(flattened, "cryotherapy_sva"),
                            if_not_done = extractedResponse(flattened, "cryotherapy_reason"),
                            extractedResponse(flattened, "cryotherapy_postponed_reason")
                        ),
                        thermal_ablation = TreatmentStatus(
                            status = extractedResponse(flattened, "thermal_status"),
                            single_visit_approach = extractedResponse(flattened, "thermal_sva"),
                            if_not_done = extractedResponse(flattened, "thermal_reason"),
                            postponed_reason = extractedResponse(
                                flattened,
                                "thermal_postponed_reason"
                            )
                        ),
                        leep = TreatmentStatus(
                            status = extractedResponse(flattened, "leep_status"),
                            single_visit_approach = extractedResponse(flattened, "leep_sva"),
                            if_not_done = extractedResponse(flattened, "leep_reason"),
                            postponed_reason = extractedResponse(flattened, "leep_postponed_reason")
                        )
                    )
                ),
                breast_screening = BreastScreening(
                    cbe = extractedResponse(flattened, "breast_examination_cbe"),
                    ultrasound = BreastExam(
                        done = extractedResponse(
                            flattened,
                            "breast_ultrasound"
                        ), birads = extractedResponse(flattened, "ultrasound_result")
                    ),
                    mammography = BreastExam(
                        done = extractedResponse(flattened, "mammography"),
                        birads = extractedResponse(flattened, "mammography_result")
                    ),
                    action = BreastAction(
                        referred = extractedResponse(flattened, "breast_action"),
                        follow_up = extractedResponse(flattened, "breast_action")
                    )
                ),
                clinical_findings = ClinicalFindings(
                    presenting_symptoms = listOf("post-coital bleeding", "pelvic pain"),
                    lesion_visible = true,
                    lesion_description = "ulcerative lesion on cervix",
                    cancer_stage = "IB2"
                ),
                medications_allergies = MedicationsAllergies(
                    comorbidities = listOf("hypertension"),
                    current_medications = listOf("nifedipine"),
                    allergies = listOf("none")
                ),
                prior_treatment = PriorTreatment(
                    cryotherapy = false,
                    leep = true,
                    radiation = false,
                    chemotherapy = false
                ),
                llm_request = LlmRequest(
                    use_case = "clinical_decision_support",
                    user_question = flattened.firstOrNull { it.first == "user_question" }?.second
                        ?: ""
                )
            )
            retrofitCallsAuthentication.performUpdatedAssessment(
                this@AssessmentActivity,
                payload,
                viewModel
            )
        }
    }

    fun flattenResponseItems(
        items: List<QuestionnaireResponse.QuestionnaireResponseItemComponent>,
        result: MutableList<Pair<String, String>>
    ) {
        items.forEach { item ->
            // if the item has answers, convert the first answer to String (valueCoding / valueString / valueDecimal / etc)
            if (item.answer.isNotEmpty()) {
                val answer = item.answer.first()
                val value = when {
                    answer.hasValueCoding() -> answer.valueCoding.code
                    answer.hasValueStringType() -> answer.valueStringType.value
                    answer.hasValueDateType() -> answer.valueDateType.valueAsString
                    answer.hasValueIntegerType() -> answer.valueIntegerType.value.toString()
                    answer.hasValueDecimalType() -> answer.valueDecimalType.value.toString()
                    else -> ""
                }
                result.add(item.linkId to value)
            }

            // go deeper recursively
            if (item.item.isNotEmpty()) {
                flattenResponseItems(item.item, result)
            }
        }
    }

    fun extractClinicalDataAsync(jsonString: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val result = extractClinicalDataFromJson(jsonString)

            val updatedData = if (result.screening_history.isEmpty()) {
                result.copy(
                    screening_history = mapOf(
                        "last_screening_type" to " ",
                        "last_screening_result" to " ",
                        "date_of_last_screening" to "No Record",
                        "hpv_test_result" to "",
                        "pap_smear_result" to ""
                    )
                )
            } else {
                result
            }
            val resultJson = Gson().toJson(updatedData)
            Log.d("ExtractedData", resultJson)
            retrofitCallsAuthentication.performAssessment(
                this@AssessmentActivity,
                updatedData,
                viewModel
            )
        }
    }

    fun extractClinicalDataFromJson(jsonString: String): ExtractedData {
        val root = JSONObject(jsonString)
        val items = root.getJSONArray("item")

        val flatAnswers = mutableMapOf<String, Any>()
        val screeningHistory = mutableMapOf<String, String>()
        val clinicalFindings = mutableMapOf<String, Any>()
        val priorTreatment = mutableMapOf<String, Boolean>()

        for (i in 0 until items.length()) {
            val item = items.getJSONObject(i)
            when (item.getString("linkId")) {
                "parity" -> flatAnswers["parity"] =
                    item.getJSONArray("answer").getJSONObject(0).getInt("valueInteger")

                "menopausal_status" -> flatAnswers["menopausal_status"] =
                    item.getJSONArray("answer").getJSONObject(0).getJSONObject("valueCoding")
                        .getString("display")

                "hiv_status" -> flatAnswers["hiv_status"] =
                    item.getJSONArray("answer").getJSONObject(0).getJSONObject("valueCoding")
                        .getString("display")

                "art_adherence" -> flatAnswers["art_adherence"] =
                    item.getJSONArray("answer").getJSONObject(0).getJSONObject("valueCoding")
                        .getString("display")

                "screening_history" -> {
                    val subItems = item.optJSONArray("item")
                    if (subItems != null) {
                        for (j in 0 until subItems.length()) {
                            val subItem = subItems.optJSONObject(j) ?: continue
                            val linkId = subItem.optString("linkId", "")

                            if (subItem.has("answer")) {
                                val answerArray = subItem.optJSONArray("answer")
                                if (answerArray != null && answerArray.length() > 0) {
                                    val answer = answerArray.optJSONObject(0)
                                    if (answer != null) {
                                        val value = when {
                                            answer.has("valueDate") -> answer.optString(
                                                "valueDate",
                                                ""
                                            )

                                            answer.has("valueString") -> answer.optString(
                                                "valueString",
                                                ""
                                            )

                                            answer.has("valueCoding") -> {
                                                answer.optJSONObject("valueCoding")
                                                    ?.optString("display", "") ?: ""
                                            }

                                            else -> ""
                                        }
                                        screeningHistory[linkId] = value
                                    }
                                }
                            }
                        }
                    }
                }


                "clinical_findings" -> {
                    val subItems = item.getJSONArray("item")
                    for (j in 0 until subItems.length()) {
                        val subItem = subItems.getJSONObject(j)
                        val linkId = subItem.getString("linkId")
                        val answer = subItem.getJSONArray("answer").getJSONObject(0)
                        val value = when {
                            answer.has("valueString") -> answer.getString("valueString")
                            answer.has("valueBoolean") -> answer.getBoolean("valueBoolean")
                            else -> ""
                        }
                        if (linkId == "presenting_symptoms") {
                            try {
                                if (value != null) {
                                    clinicalFindings[linkId] =
                                        value.toString().split(",").map { it.trim() }
                                }
                            } catch (e: Exception) {
                                println("Error Encountered ${e.message}")
                            }
                        } else {
                            clinicalFindings[linkId] = value
                        }
                    }
                }

                "comorbidities" -> {
                    val value =
                        item.getJSONArray("answer").getJSONObject(0).getString("valueString")
                    flatAnswers["comorbidities"] = value.split(",").map { it.trim() }
                }

                "medications" -> {
                    val value =
                        item.getJSONArray("answer").getJSONObject(0).getString("valueString")
                    flatAnswers["medications"] = value.split(",").map { it.trim() }
                }

                "allergies" -> {
                    val value =
                        item.getJSONArray("answer").getJSONObject(0).getString("valueString")
                    flatAnswers["allergies"] = value.split(",").map { it.trim() }
                }

                "prior_treatment" -> {
                    val subItems = item.getJSONArray("item")
                    for (j in 0 until subItems.length()) {
                        val subItem = subItems.getJSONObject(j)
                        val linkId = subItem.getString("linkId")
                        val value = subItem.getJSONArray("answer").getJSONObject(0)
                            .getBoolean("valueBoolean")
                        priorTreatment[linkId] = value
                    }
                }

                "user_question" -> {
                    val answer =
                        item.getJSONArray("answer").getJSONObject(0).getString("valueString")
                    flatAnswers["user_question"] = answer
                }
            }
        }

        return ExtractedData(
            patient_age = calculatePatientAgeInYears(), // Not available in the provided JSON
            parity = flatAnswers["parity"] as? Int,
            menopausal_status = flatAnswers["menopausal_status"] as? String,
            hiv_status = flatAnswers["hiv_status"] as? String,
            art_adherence = flatAnswers["art_adherence"] as? String,
            screening_history = screeningHistory,
            clinical_findings = clinicalFindings,
            comorbidities = flatAnswers["comorbidities"] as? List<String> ?: emptyList(),
            medications = flatAnswers["medications"] as? List<String> ?: emptyList(),
            allergies = flatAnswers["allergies"] as? List<String> ?: emptyList(),
            prior_treatment = priorTreatment,
            user_question = flatAnswers["user_question"] as? String
        )
    }

    private fun calculatePatientAgeInYears(): Int {
        var age = 0
        val dob = Functions().getSharedPref("age", this@AssessmentActivity)
        if (dob != null) {
            age = dob.toInt()
        }
        return age
    }


    private fun showCancelScreenerQuestionnaireAlertDialog() {
        val builder = AlertDialog.Builder(this)
        builder.apply {
            setMessage(getString(R.string.cancel_questionnaire_message))
            setPositiveButton(getString(android.R.string.yes)) { _, _ ->
                this@AssessmentActivity.finish()
            }
            setNegativeButton(getString(android.R.string.no)) { _, _ -> }
        }
        val alertDialog = builder.create()
        alertDialog.show()
    }


    override fun onBackPressed() {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Exit")
            .setMessage("Are you sure you want to exit?")
            .setPositiveButton("Yes") { _, _ ->
                super.onBackPressed() // Exit the activity
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss() // Dismiss the dialog
            }
            .create()

        dialog.show()
    }

    private fun addQuestionnaireFragment() {
        supportFragmentManager.commit {
            add(
                R.id.add_patient_container,
                QuestionnaireFragment.builder()
                    .setQuestionnaire(viewModel.questionnaireJson)
                    .setShowCancelButton(true)
                    .setSubmitButtonText("Submit")
                    .build(),
                QUESTIONNAIRE_FRAGMENT_TAG,
            )
        }
    }

    private fun observePatientSaveAction() {
        viewModel.isPatientSaved.observe(this) {
            ProgressDialogManager.dismiss()

            if (!it) {
                Toast.makeText(this, "Please Enter all Required Fields.", Toast.LENGTH_SHORT).show()
                return@observe
            }
            showSuccessDialog(this@AssessmentActivity)

        }
    }

    fun showSuccessDialog(context: Context) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.success_dialog, null)
        val alertDialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialogView.findViewById<MaterialButton>(R.id.btn_cancel).setOnClickListener {
            alertDialog.dismiss()
            this@AssessmentActivity.finish()
        }

        dialogView.findViewById<MaterialButton>(R.id.btn_finish).setOnClickListener {
            // handle finish action
            this@AssessmentActivity.finish()
            alertDialog.dismiss()
        }

        alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        alertDialog.show()
    }

    private fun updateArguments() {
        val json = Functions().getSharedPref("questionnaire", this@AssessmentActivity)
        intent.putExtra(QUESTIONNAIRE_FILE_PATH_KEY, json)
    }

    override fun onSupportNavigateUp(): Boolean {
        showCancelScreenerQuestionnaireAlertDialog()
        return true
    }

    companion object {
        const val QUESTIONNAIRE_FILE_PATH_KEY = "questionnaire-file-path-key"
        const val QUESTIONNAIRE_FRAGMENT_TAG = "questionnaire-fragment-tag"
    }
}