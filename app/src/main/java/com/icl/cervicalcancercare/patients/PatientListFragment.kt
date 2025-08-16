package com.icl.cervicalcancercare.patients

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.fhir.FhirEngine
import com.google.android.material.button.MaterialButton
import com.icl.cervicalcancercare.R
import com.icl.cervicalcancercare.adapters.PatientAdapter
import com.icl.cervicalcancercare.auth.LoginActivity
import com.icl.cervicalcancercare.databinding.FragmentFirstBinding
import com.icl.cervicalcancercare.details.PatientDetailsActivity
import com.icl.cervicalcancercare.fhir.FhirApplication
import com.icl.cervicalcancercare.models.PatientItem
import com.icl.cervicalcancercare.network.FormatterClass
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

            val user = FormatterClass().getSharedPref("userName", requireContext())
            if (user != null) {
                tvUserName.text = user.replaceFirstChar { it.uppercaseChar() }
            }
            val userName = tvUserName.text.toString()

            val initials = Functions().getInitials(userName)
            val avatarBitmap = Functions().createAvatar(initials)
            imgUserProfile.setImageBitmap(avatarBitmap)


            settingsIcon.setOnClickListener {
                confirmAction()
            }
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
        val imm =
            requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        binding.searchInput.apply {
            addTextChangedListener(
                onTextChanged = { text, _, _, _ ->
                    patientListViewModel.setPatientGivenName(text.toString())
                },
            )
            setOnFocusChangeListener { view, hasFocus ->
                if (!hasFocus) {
                    imm.hideSoftInputFromWindow(view.windowToken, 0)
                }
            }
        }
        val adapter1 = PatientAdapter(this::onPatientItemClicked)
        patientListViewModel.patientCount.observe(viewLifecycleOwner) {
//            binding.patientCount.text = "$it Patient(s)"
        }
        patientListViewModel.liveSearchedPatients.observe(viewLifecycleOwner) {
            binding.progressBar.visibility = View.GONE
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

    private fun confirmAction() {
        val dialogView = layoutInflater.inflate(R.layout.confirm_dialog, null)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()

        val confirmButton = dialogView.findViewById<MaterialButton>(R.id.confirmButton)
        val cancelButton = dialogView.findViewById<MaterialButton>(R.id.cancelButton)
        val messageText = dialogView.findViewById<TextView>(R.id.dialogMessage)
        val dialogTitle = dialogView.findViewById<TextView>(R.id.dialogTitle)

        dialogTitle.text = "Log Out?"
        messageText.text = "Are you sure you want to Log Out?"

        confirmButton.setOnClickListener {
            // handle confirm action

            FormatterClass().deleteSharedPref("access_token", requireContext())
            FormatterClass().deleteSharedPref("isLoggedIn", requireContext())
            val intent = Intent(context, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            dialog.dismiss()
        }

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()

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

        Functions().saveSharedPref("full_name", data.name, requireContext())

        Functions().saveSharedPref("phone", data.phone, requireContext())
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