package com.icl.cervicalcancercare.details

import android.content.res.Resources
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2
import com.google.android.fhir.FhirEngine
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.icl.cervicalcancercare.R
import com.icl.cervicalcancercare.adapters.ViewPagerAdapter
import com.icl.cervicalcancercare.databinding.ActivityAddPatientBinding
import com.icl.cervicalcancercare.databinding.ActivityPatientDetailsBinding
import com.icl.cervicalcancercare.fhir.FhirApplication
import com.icl.cervicalcancercare.models.PatientItem
import com.icl.cervicalcancercare.patients.AddPatientActivity
import com.icl.cervicalcancercare.utils.Functions
import com.icl.cervicalcancercare.viewmodels.PatientDetailsViewModel
import com.icl.cervicalcancercare.viewmodels.factories.PatientDetailsViewModelFactory
import java.time.LocalDate
import java.time.Period

class PatientDetailsActivity : AppCompatActivity() {
    private lateinit var binding:
            ActivityPatientDetailsBinding
    private lateinit var fhirEngine: FhirEngine
    private lateinit var patientDetailsViewModel: PatientDetailsViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityPatientDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
//            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
//            insets
//        }
        setSupportActionBar(binding.toolbar)
        val patientId = Functions().getSharedPref("resourceId", this@PatientDetailsActivity)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val titleName = Functions().getSharedPref("AddParentTitle", this@PatientDetailsActivity)
        supportActionBar.apply { title = "" }

        fhirEngine = FhirApplication.fhirEngine(this)
        patientDetailsViewModel =
            ViewModelProvider(
                this,
                PatientDetailsViewModelFactory(
                    this@PatientDetailsActivity.application, fhirEngine, "$patientId"
                ),
            )
                .get(PatientDetailsViewModel::class.java)


        val viewPager = findViewById<ViewPager2>(R.id.viewPager)
        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)

        viewPager.adapter = ViewPagerAdapter(this)


        patientDetailsViewModel.getPatientDetailData()
        patientDetailsViewModel.livePatientData.observe(this) { data ->
            if (data != null) {

                data.impressions.forEach {
                    println("Here is is impression:: ${it.status}")
                    val dd = it.basis
                    dd.forEach { k ->
                        println("Here is is impression:: $k")
                    }
                }
                binding.apply {
                    if (data.basic != null) {
                        val basic = data.basic
                        patientName.text = basic.name
                        tvPhone.text = basic.phone
                        tvDob.text = "${basic.dob}"
                        tvAge.text = getFormattedAge(basic, tvAge.context.resources)
                        try {
                            val initials = Functions().getInitials(basic.name)
                            val avatarBitmap = Functions().createAvatar(initials)
                            profileImage.setImageBitmap(avatarBitmap)

                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Overview"
                1 -> "Recommendations"
                2 -> "Diagnosis"
                else -> "Overview"
            }
        }.attach()
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