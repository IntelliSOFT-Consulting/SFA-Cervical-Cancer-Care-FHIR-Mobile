package com.icl.cervicalcancercare.details

import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.CheckBox
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2
import com.google.android.fhir.FhirEngine
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.icl.cervicalcancercare.R
import com.icl.cervicalcancercare.adapters.ViewPagerAdapter
import com.icl.cervicalcancercare.databinding.ActivityPatientDetailsBinding
import com.icl.cervicalcancercare.fhir.FhirApplication
import com.icl.cervicalcancercare.models.PatientItem
import com.icl.cervicalcancercare.models.PatientSummary
import com.icl.cervicalcancercare.network.FormatterClass
import com.icl.cervicalcancercare.patients.EditPatientActivity
import com.icl.cervicalcancercare.utils.Functions
import com.icl.cervicalcancercare.viewmodels.PatientDetailsViewModel
import com.icl.cervicalcancercare.viewmodels.factories.PatientDetailsViewModelFactory
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.util.Locale

class PatientDetailsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPatientDetailsBinding
    private lateinit var fhirEngine: FhirEngine
    private lateinit var editId: String
    private lateinit var patientDetailsViewModel: PatientDetailsViewModel
    private val dobFormatter = DateTimeFormatter.ofPattern("d MMM uuuu", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityPatientDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applySystemBarAppearance()
        applyWindowInsets()
        setupToolbar()

        val patientId = Functions().getSharedPref("resourceId", this).orEmpty()

        fhirEngine = FhirApplication.fhirEngine(this)
        patientDetailsViewModel =
            ViewModelProvider(
                this,
                PatientDetailsViewModelFactory(
                    application,
                    fhirEngine,
                    patientId
                ),
            )[PatientDetailsViewModel::class.java]

        setupTabs()
        setupActions()

        editId = ""
        patientDetailsViewModel.getPatientDetailData()
        patientDetailsViewModel.livePatientData.observe(this) { data ->
            data?.let { renderPatientDetails(it, patientId) }
        }
    }

    override fun onResume() {
        super.onResume()
        applySystemBarAppearance()
        try {
            patientDetailsViewModel.getPatientDetailData()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = ""
            setDisplayHomeAsUpEnabled(true)
            setHomeButtonEnabled(true)
        }
        binding.toolbar.navigationIcon?.setTint(
            ContextCompat.getColor(this, R.color.white)
        )
    }

    private fun setupTabs() {
        val viewPager = findViewById<ViewPager2>(R.id.viewPager)
        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)

        viewPager.adapter = ViewPagerAdapter(this)
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.patient_details_tab_overview)
                1 -> getString(R.string.patient_details_tab_recommendations)
                2 -> getString(R.string.patient_details_tab_diagnosis)
                else -> getString(R.string.patient_details_tab_overview)
            }
        }.attach()
    }

    private fun setupActions() {
        binding.btnEditPatient.setOnClickListener {
            if (editId.isNotEmpty()) {
                FormatterClass().saveSharedPref(
                    "questionnaire",
                    "add-patient.json",
                    this
                )
                startActivity(
                    Intent(this, EditPatientActivity::class.java)
                        .putExtra("questionnaire_id", editId)
                )
            } else {
                comingSoon()
            }
        }

        binding.btnContactPatient.setOnClickListener {
            showContactOptions()
        }
    }

    private fun renderPatientDetails(data: PatientSummary, patientId: String) {
        editId = data.registrationResponse.find { item -> item.patientId == patientId }?.resourceId.orEmpty()

        val basic = data.basic ?: return
        binding.apply {
            patientName.text = basic.name.ifBlank { getString(R.string.patient_details_no_name) }
            tvPhone.text = basic.phone.orFallback(R.string.patient_details_no_phone)
            tvEmail.text = basic.email.orFallback(R.string.patient_details_no_email)
            tvDob.text = formatDob(basic.dob)
            tvIdentificationNumber.text =
                basic.identificationNumber.orFallback(R.string.patient_details_no_identifier)
            tvIdentificationType.text =
                basic.identificationType.orFallback(R.string.patient_details_identifier)
            tvAge.text = getFormattedAge(basic, resources)
                .ifBlank { getString(R.string.patient_details_unavailable) }

            runCatching {
                val initials = Functions().getInitials(patientName.text.toString())
                val avatarBitmap = Functions().createAvatar(initials)
                profileImage.setImageBitmap(avatarBitmap)
            }.onFailure { error ->
                error.printStackTrace()
            }
        }

        FormatterClass().saveSharedPref("county", basic.county.orEmpty(), this)
        FormatterClass().saveSharedPref("sub_county", basic.sub_county.orEmpty(), this)
        FormatterClass().saveSharedPref("ward", basic.ward.orEmpty(), this)
    }

    private fun comingSoon() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.coming_dialog, null)
        val alertDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialogView.findViewById<MaterialButton>(R.id.btn_cancel).setOnClickListener {
            alertDialog.dismiss()
        }

        dialogView.findViewById<MaterialButton>(R.id.btn_finish).setOnClickListener {
            alertDialog.dismiss()
        }

        alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        alertDialog.show()
    }

    private fun showContactOptions() {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_sheet_layout, null)

        val whatsappCheck = view.findViewById<CheckBox>(R.id.whatsappCheck)
        val emailCheck = view.findViewById<CheckBox>(R.id.emailCheck)
        val smsCheck = view.findViewById<CheckBox>(R.id.smsCheck)
        val sendBtn = view.findViewById<Button>(R.id.sendBtn)

        sendBtn.setOnClickListener {
            val selectedChannels = mutableListOf<String>()
            if (whatsappCheck.isChecked) selectedChannels.add(whatsappCheck.text.toString())
            if (emailCheck.isChecked) selectedChannels.add(emailCheck.text.toString())
            if (smsCheck.isChecked) selectedChannels.add(smsCheck.text.toString())

            if (selectedChannels.isEmpty()) {
                Toast.makeText(
                    this,
                    getString(R.string.patient_details_no_channel_selected),
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            Toast.makeText(
                this,
                getString(
                    R.string.patient_details_toast_sending,
                    selectedChannels.joinToString()
                ),
                Toast.LENGTH_SHORT
            ).show()
            comingSoon()
            dialog.dismiss()
        }

        dialog.setContentView(view)
        dialog.show()
    }

    private fun formatDob(dob: LocalDate?): String {
        return dob?.format(dobFormatter) ?: getString(R.string.patient_details_unavailable)
    }

    private fun String?.orFallback(fallback: Int): String {
        return if (this.isNullOrBlank()) getString(fallback) else this
    }

    private fun applyWindowInsets() {
        val initialPaddingLeft = binding.main.paddingLeft
        val initialPaddingTop = binding.main.paddingTop
        val initialPaddingRight = binding.main.paddingRight
        val initialPaddingBottom = binding.main.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                initialPaddingLeft + systemBars.left,
                initialPaddingTop + systemBars.top,
                initialPaddingRight + systemBars.right,
                initialPaddingBottom + systemBars.bottom
            )
            insets
        }
    }

    private fun applySystemBarAppearance() {
        val isNightMode =
            (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = !isNightMode
        }
    }

    private fun getFormattedAge(
        patientItem: PatientItem,
        resources: Resources,
    ): String {
        if (patientItem.dob == null) return ""
        return Period.between(patientItem.dob, LocalDate.now()).let {
            when {
                it.years > 0 -> resources.getQuantityString(R.plurals.ageYear, it.years, it.years)
                it.months > 0 -> resources.getQuantityString(
                    R.plurals.ageMonth,
                    it.months,
                    it.months
                )

                else -> resources.getQuantityString(R.plurals.ageDay, it.days, it.days)
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
