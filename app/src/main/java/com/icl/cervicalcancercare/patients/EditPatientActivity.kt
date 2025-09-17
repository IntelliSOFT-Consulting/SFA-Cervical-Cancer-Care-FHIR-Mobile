package com.icl.cervicalcancercare.patients

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.commit
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.context.FhirVersionEnum
import com.google.android.fhir.datacapture.QuestionnaireFragment
import com.icl.cervicalcancercare.R
import com.icl.cervicalcancercare.SecondFragment.Companion.QUESTIONNAIRE_FRAGMENT_TAG
import com.icl.cervicalcancercare.databinding.ActivityEditPatientBinding
import com.icl.cervicalcancercare.network.FormatterClass
import com.icl.cervicalcancercare.utils.Functions
import com.icl.cervicalcancercare.viewmodels.EditPatientViewModel
import com.icl.cervicalcancercare.viewmodels.EditResponseViewModel
import com.icl.cervicalcancercare.viewmodels.factories.EditResponseViewModelFactory
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.Questionnaire
import kotlin.getValue

class EditPatientActivity : AppCompatActivity() {
    private lateinit var viewModel: EditResponseViewModel
    private lateinit var binding: ActivityEditPatientBinding
    var questionnaireJsonString: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityEditPatientBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        val questionnaireId: String = savedInstanceState?.getString("questionnaire_id")
            ?: intent.getStringExtra("questionnaire_id")
            ?: ""

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val titleName = Functions().getSharedPref("AddParentTitle", this@EditPatientActivity)
        supportActionBar.apply { title = "Edit Patient" }
        val questionnaire =
            FormatterClass().getSharedPref("questionnaire", this@EditPatientActivity)

        questionnaireJsonString = getStringFromAssets("$questionnaire")

        val factory = EditResponseViewModelFactory(
            application = application,
            questionnaireId = questionnaireId,
            questionnaire = "$questionnaire"
        )
        viewModel = ViewModelProvider(this, factory)[EditResponseViewModel::class.java]

        updateArguments()

        supportFragmentManager.setFragmentResultListener(
            QuestionnaireFragment.SUBMIT_REQUEST_KEY,
            this@EditPatientActivity,
        ) { _, _ ->
            onSubmitAction()
        }
        supportFragmentManager.setFragmentResultListener(
            QuestionnaireFragment.CANCEL_REQUEST_KEY,
            this@EditPatientActivity,
        ) { _, _ ->
            onBackPressed()
        }

        viewModel.liveEditData.observe(this) { addQuestionnaireFragment(it) }
        viewModel.isResourcesSaved.observe(this) {
            if (!it) {
                Toast.makeText(
                    this@EditPatientActivity,
                    R.string.inputs_missing,
                    Toast.LENGTH_SHORT
                ).show()
                return@observe
            }
            Toast.makeText(
                this@EditPatientActivity,
                R.string.message_patient_updated,
                Toast.LENGTH_SHORT
            )
                .show()
            this.finish()
        }
    }
    private fun getStringFromAssets(fileName: String): String {
        return assets.open(fileName).bufferedReader().use { it.readText() }
    }

    private fun addQuestionnaireFragment(pair: Pair<String, String>) {
        lifecycleScope.launch {
            supportFragmentManager.commit {
                add(
                    R.id.add_patient_container,
                    QuestionnaireFragment.builder()
                        .setQuestionnaire(pair.first)
                        .setQuestionnaireResponse(pair.second)
                        .build(),
                    QUESTIONNAIRE_FRAGMENT_TAG,
                )
            }
        }
    }

    private fun onSubmitAction() {
        lifecycleScope.launch {
            val questionnaireFragment =
                supportFragmentManager.findFragmentByTag(AddPatientActivity.Companion.QUESTIONNAIRE_FRAGMENT_TAG)
                        as QuestionnaireFragment

            val questionnaireResponse = questionnaireFragment.getQuestionnaireResponse()
            // Print the response to the log
            val jsonParser = FhirContext.forCached(FhirVersionEnum.R4).newJsonParser()
            val questionnaireResponseString =
                jsonParser.encodeResourceToString(questionnaireResponse)
            val questionnaire =
                jsonParser.parseResource(questionnaireJsonString) as Questionnaire

            viewModel.updatePatient(
                context = this@EditPatientActivity,
                questionnaireResponse = questionnaireFragment.getQuestionnaireResponse(),
                questionnaire = questionnaire,
                questionnaireResponseString = questionnaireResponseString
            )
        }
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

    private fun updateArguments() {
        val json = Functions().getSharedPref("questionnaire", this@EditPatientActivity)
        intent.putExtra(QUESTIONNAIRE_FILE_PATH_KEY, json)
    }

    private fun showCancelScreenerQuestionnaireAlertDialog() {
        val builder = AlertDialog.Builder(this)
        builder.apply {
            setMessage(getString(R.string.cancel_questionnaire_message))
            setPositiveButton(getString(android.R.string.yes)) { _, _ ->
                this@EditPatientActivity.finish()
            }
            setNegativeButton(getString(android.R.string.no)) { _, _ -> }
        }
        val alertDialog = builder.create()
        alertDialog.show()
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

