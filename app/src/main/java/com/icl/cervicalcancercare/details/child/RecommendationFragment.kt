package com.icl.cervicalcancercare.details.child

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.fhir.FhirEngine
import com.google.gson.Gson
import com.icl.cervicalcancercare.R
import com.icl.cervicalcancercare.adapters.RecommendationsAdapter
import com.icl.cervicalcancercare.databinding.FragmentRecommendationBinding
import com.icl.cervicalcancercare.fhir.FhirApplication
import com.icl.cervicalcancercare.models.PatientImpression
import com.icl.cervicalcancercare.models.PatientItem
import com.icl.cervicalcancercare.utils.Functions
import com.icl.cervicalcancercare.viewmodels.PatientDetailsViewModel
import com.icl.cervicalcancercare.viewmodels.factories.PatientDetailsViewModelFactory

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [RecommendationFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class RecommendationFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    private lateinit var fhirEngine: FhirEngine
    private lateinit var patientDetailsViewModel: PatientDetailsViewModel
    // create a binding

    private var _binding: FragmentRecommendationBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
//        return inflater.inflate(R.layout.fragment_recommendation, container, false)

        _binding = FragmentRecommendationBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val patientId = Functions().getSharedPref("resourceId", requireContext())

        fhirEngine = FhirApplication.fhirEngine(requireContext())
        patientDetailsViewModel =
            ViewModelProvider(
                this,
                PatientDetailsViewModelFactory(
                    requireActivity().application, fhirEngine, "$patientId"
                ),
            )
                .get(PatientDetailsViewModel::class.java)
        val adapter1 = RecommendationsAdapter(this::onItemClicked)
        binding.apply {
            patientRecycler.adapter = adapter1
            outlinedButton.visibility = View.GONE
            patientRecycler.layoutManager = LinearLayoutManager(requireContext())
            patientRecycler.addItemDecoration(
                DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL).apply {
                    setDrawable(ColorDrawable(Color.LTGRAY))
                },
            )
        }
        patientDetailsViewModel.livePatientData.observe(viewLifecycleOwner) {
            it.impressions.forEach {
                println("Here is is impression Summary:: ${it.summary}")
            }
            adapter1.submitList(it.impressions)

        }
        patientDetailsViewModel.getPatientDetailData()
    }

    private fun onItemClicked(data: PatientImpression) {
        val json = Gson().toJson(data)
        val intent = Intent(requireContext(), RecommendationDetailsActivity::class.java)
        intent.putExtra("impression_json", json)
        startActivity(intent)

    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment RecommendationFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            RecommendationFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}