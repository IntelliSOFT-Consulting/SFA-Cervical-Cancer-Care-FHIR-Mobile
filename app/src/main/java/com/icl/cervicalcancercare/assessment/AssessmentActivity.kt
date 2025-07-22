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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import com.google.android.fhir.datacapture.QuestionnaireFragment
import com.google.android.material.button.MaterialButton
import com.google.gson.Gson
import com.icl.cervicalcancercare.R
import com.icl.cervicalcancercare.databinding.ActivityAssessmentBinding
import com.icl.cervicalcancercare.models.ExtractedData
import com.icl.cervicalcancercare.network.RetrofitCallsAuthentication
import com.icl.cervicalcancercare.utils.Functions
import com.icl.cervicalcancercare.utils.ProgressDialogManager
import com.icl.cervicalcancercare.viewmodels.AddPatientViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
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
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

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
            Log.e("response", questionnaireResponseString)
            println("Response $questionnaireResponseString")
            extractClinicalDataAsync(questionnaireResponseString)

        }
    }

    fun extractClinicalDataAsync(jsonString: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val result = extractClinicalDataFromJson(jsonString)
            val resultJson = Gson().toJson(result)
            Log.d("ExtractedData", resultJson)
            retrofitCallsAuthentication.performAssessment(this@AssessmentActivity, result,viewModel)
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
                    val subItems = item.getJSONArray("item")
                    for (j in 0 until subItems.length()) {
                        val subItem = subItems.getJSONObject(j)
                        val linkId = subItem.getString("linkId")
                        val answer = subItem.getJSONArray("answer").getJSONObject(0)
                        screeningHistory[linkId] = when {
                            answer.has("valueDate") -> answer.getString("valueDate")
                            answer.has("valueString") -> answer.getString("valueString")
                            answer.has("valueCoding") -> answer.getJSONObject("valueCoding")
                                .getString("display")

                            else -> ""
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