package com.icl.cervicalcancercare.patients

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
import com.icl.cervicalcancercare.R
import com.icl.cervicalcancercare.databinding.ActivityAddPatientBinding
import com.icl.cervicalcancercare.utils.Functions
import com.icl.cervicalcancercare.utils.ProgressDialogManager
import com.icl.cervicalcancercare.viewmodels.AddPatientViewModel
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.QuestionnaireResponse

class AddPatientActivity : AppCompatActivity() {

    private val viewModel: AddPatientViewModel by viewModels()
    private lateinit var binding:
            ActivityAddPatientBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAddPatientBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setSupportActionBar(binding.toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val titleName = Functions().getSharedPref("AddParentTitle", this@AddPatientActivity)
        supportActionBar.apply { title = titleName }


        updateArguments()
        if (savedInstanceState == null) {
            addQuestionnaireFragment()
        }
        observePatientSaveAction()

        supportFragmentManager.setFragmentResultListener(
            QuestionnaireFragment.SUBMIT_REQUEST_KEY,
            this@AddPatientActivity,
        ) { _, _ ->
            onSubmitAction()
        }
        supportFragmentManager.setFragmentResultListener(
            QuestionnaireFragment.CANCEL_REQUEST_KEY,
            this@AddPatientActivity,
        ) { _, _ ->
            onBackPressed()
        }

    }

    private fun onSubmitAction() {
        ProgressDialogManager.show(this, "Please Wait.....")
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
            saveCase(questionnaireFragment.getQuestionnaireResponse(), questionnaireResponseString)
        }
    }


    private fun showCancelScreenerQuestionnaireAlertDialog() {
        val builder = AlertDialog.Builder(this)
        builder.apply {
            setMessage(getString(R.string.cancel_questionnaire_message))
            setPositiveButton(getString(android.R.string.yes)) { _, _ ->
                this@AddPatientActivity.finish()
            }
            setNegativeButton(getString(android.R.string.no)) { _, _ -> }
        }
        val alertDialog = builder.create()
        alertDialog.show()
    }

    private fun saveCase(
        questionnaireResponse: QuestionnaireResponse,
        questionnaireResponseString: String
    ) {

        viewModel.savePatientData(
            questionnaireResponse,
            questionnaireResponseString,
            this@AddPatientActivity
        )

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
            showSuccessDialog(this@AddPatientActivity)

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
            this@AddPatientActivity.finish()
        }

        dialogView.findViewById<MaterialButton>(R.id.btn_finish).setOnClickListener {
            // handle finish action
            this@AddPatientActivity.finish()
            alertDialog.dismiss()
        }

        alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        alertDialog.show()
    }

    private fun updateArguments() {
        val json = Functions().getSharedPref("questionnaire", this@AddPatientActivity)
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