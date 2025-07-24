package com.icl.cervicalcancercare.details.child

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.utils.ColorTemplate
import com.icl.cervicalcancercare.assessment.AssessmentActivity
import com.icl.cervicalcancercare.databinding.FragmentOverviewBinding
import com.icl.cervicalcancercare.utils.Functions


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [OverviewFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class OverviewFragment : Fragment() {
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

    // let's create a binding
    private var _binding: FragmentOverviewBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment

        _binding = FragmentOverviewBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {

            btnAdd.apply {

                setOnClickListener {
                    Functions().saveSharedPref("questionnaire", "assessment.json", requireContext())
                    Functions().saveSharedPref("AddParentTitle", "Assessment", requireContext())
                    val intent = Intent(requireContext(), AssessmentActivity::class.java)
                    startActivity(intent)
                }
            }

            pieChart.apply {

                setUsePercentValues(true)
                description.isEnabled = false
                setExtraOffsets(5f, 10f, 5f, 5f)

                setDragDecelerationFrictionCoef(0.95f)

//                setCenterTextTypeface(tfLight)
                centerText = generateCenterSpannableText()

                isDrawHoleEnabled = true
                setHoleColor(Color.WHITE)

                setTransparentCircleColor(Color.WHITE)
                setTransparentCircleAlpha(110)

                holeRadius = 58f
                transparentCircleRadius = 61f

                setDrawCenterText(true)

                setRotationAngle(0f)

                // enable rotation of the chart by touch
                isRotationEnabled = true
                isHighlightPerTapEnabled = true


                // chart.setUnit(" €");
                // chart.setDrawUnitsInChart(true);

                // add a selection listener
//                setOnChartValueSelectedListener(this)
//
//                seekBarX.setProgress(4)
//                seekBarY.setProgress(10)
//
//                animateY(1400, Easing.EaseInOutQuad)
//
//
//                // chart.spin(2000, 0, 360);
//                val l: Legend = chart.getLegend()
//                l.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP)
//                l.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT)
//                l.setOrientation(Legend.LegendOrientation.VERTICAL)
//                l.setDrawInside(false)
//                l.setXEntrySpace(7f)
//                l.setYEntrySpace(0f)
//                l.setYOffset(0f)
//
//
//                // entry label styling
//                setEntryLabelColor(Color.WHITE)
//                setEntryLabelTypeface(tfRegular)
//                setEntryLabelTextSize(12f)
            }
        }


    }

    private fun generateCenterSpannableText(): CharSequence {
        val s = SpannableString("MPAndroidChart\ndeveloped by Philipp Jahoda")
        s.setSpan(RelativeSizeSpan(1.7f), 0, 14, 0)
        s.setSpan(StyleSpan(Typeface.NORMAL), 14, s.length - 15, 0)
        s.setSpan(ForegroundColorSpan(Color.GRAY), 14, s.length - 15, 0)
        s.setSpan(RelativeSizeSpan(.8f), 14, s.length - 15, 0)
        s.setSpan(StyleSpan(Typeface.ITALIC), s.length - 14, s.length, 0)
        s.setSpan(ForegroundColorSpan(ColorTemplate.getHoloBlue()), s.length - 14, s.length, 0)
        return s
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment OverviewFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            OverviewFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}