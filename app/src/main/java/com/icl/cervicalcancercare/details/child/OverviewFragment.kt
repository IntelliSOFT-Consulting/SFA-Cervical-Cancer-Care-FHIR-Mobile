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
import androidx.viewpager2.widget.ViewPager2
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.android.gms.common.util.DeviceProperties.isTablet
import com.icl.cervicalcancercare.R
import com.icl.cervicalcancercare.assessment.AssessmentActivity
import com.icl.cervicalcancercare.databinding.FragmentOverviewBinding
import com.icl.cervicalcancercare.models.PieItem
import com.icl.cervicalcancercare.utils.Functions
import kotlin.collections.withIndex


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


//        displayProjectedRates()

        binding.apply {

            btnViewRecommendation.apply {
                setOnClickListener {
                    val viewPager = requireActivity().findViewById<ViewPager2>(R.id.viewPager)
                    val nextPage = viewPager.currentItem + 1
                    if (nextPage < (viewPager.adapter?.itemCount ?: 0)) {
                        viewPager.setCurrentItem(nextPage, true)
                    }
                }
            }
        }
    }

    private fun displayProjectedRates() {
        val pie: MutableList<PieItem> = mutableListOf()
        pie.add(
            PieItem(
                "90",
                "Survival",
                "#1EAF5F"
            )
        )
        pie.add(
            PieItem(
                "90",
                "Survival",
                "#1EAF5F"
            )
        )

        val pieShades: ArrayList<Int> = ArrayList()
        val entries = ArrayList<PieEntry>()
        for ((i, entry) in pie.withIndex()) {
            entries.add(PieEntry(entry.value.toFloat(), entry.label))
            pieShades.add(Color.parseColor(entry.color))
        }

        val ourSet = PieDataSet(entries, "")
        val data = PieData(ourSet)

        ourSet.sliceSpace = 1f
        ourSet.colors = pieShades
        data.setValueTextColor(Color.WHITE)
        data.setValueTextSize(10f)
        binding.apply {

            pieChart.data = data
            pieChart.legend.setDrawInside(false)
            pieChart.legend.isEnabled = true

            pieChart.legend.orientation = Legend.LegendOrientation.HORIZONTAL
            pieChart.legend.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
            pieChart.legend.horizontalAlignment = Legend.LegendHorizontalAlignment.LEFT


            pieChart.legend.isWordWrapEnabled = true
            pieChart.legend.xEntrySpace = 10f
            pieChart.legend.yEntrySpace = 10f
            pieChart.legend.yOffset = 10f
            pieChart.legend.xOffset = 10f
            pieChart.extraTopOffset = 15f
            pieChart.extraBottomOffset = 15f
            pieChart.extraLeftOffset = 0f
            pieChart.extraRightOffset = 50f
            pieChart.animateY(1400, Easing.EaseInOutQuad)
            pieChart.isDrawHoleEnabled = false
            pieChart.description.isEnabled = false
            pieChart.setDrawEntryLabels(false)
            pieChart.invalidate()
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