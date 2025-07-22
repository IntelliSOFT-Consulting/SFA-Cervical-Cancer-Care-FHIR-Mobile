package com.icl.cervicalcancercare.patients

import android.content.Intent
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.fhir.FhirEngine
import com.icl.cervicalcancercare.R
import com.icl.cervicalcancercare.adapters.PatientAdapter
import com.icl.cervicalcancercare.databinding.FragmentFirstBinding
import com.icl.cervicalcancercare.details.PatientDetailsActivity
import com.icl.cervicalcancercare.fhir.FhirApplication
import com.icl.cervicalcancercare.models.PatientItem
import com.icl.cervicalcancercare.utils.Functions
import com.icl.cervicalcancercare.viewmodels.PatientListViewModel
import java.time.LocalDate
import java.time.Period

/**
 * A simple [androidx.fragment.app.Fragment] subclass as the default destination in the navigation.
 */
class PatientListFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private lateinit var fhirEngine: FhirEngine
    private lateinit var patientListViewModel: PatientListViewModel
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onResume() {
        super.onResume()
        try {
            patientListViewModel.searchPatientsByName("")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {

            val userName = tvUserName.text.toString()

            val initials = Functions().getInitials(userName)
            val avatarBitmap = Functions().createAvatar(initials)
            imgUserProfile.setImageBitmap(avatarBitmap)
        }

        fhirEngine = FhirApplication.Companion.fhirEngine(requireContext())
        patientListViewModel =
            ViewModelProvider(
                this,
                PatientListViewModel.PatientListViewModelFactory(
                    requireActivity().application,
                    fhirEngine
                ),
            )
                .get(PatientListViewModel::class.java)

        val adapter1 = PatientAdapter(this::onPatientItemClicked)
        patientListViewModel.liveSearchedPatients.observe(viewLifecycleOwner) {
            adapter1.submitList(it)
        }
        binding.patientRecycler.apply {
            layoutManager = LinearLayoutManager(requireContext())
            addItemDecoration(
                DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL).apply {
                    setDrawable(ColorDrawable(Color.LTGRAY))
                },
            )
            adapter = adapter1
        }
    }

    fun getFormattedAge(
        patientItem: PatientItem,
    ): String {
        if (patientItem.dob == null) return "0"
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
    fun extractAgeNumber(ageString: String): Int {
        return ageString.trim().split(" ").firstOrNull()?.toIntOrNull() ?: 0
    }
    private fun onPatientItemClicked(data: PatientItem) {
        Functions().saveSharedPref("resourceId", data.resourceId, requireContext())

        // Get Patient Age and Save to preferences
        val age = getFormattedAge(
            data,
        )
        println("Current Age ${extractAgeNumber(age)}")
        Functions().saveSharedPref("age", extractAgeNumber(age).toString(), requireContext())
        startActivity(Intent(requireContext(), PatientDetailsActivity::class.java))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


}