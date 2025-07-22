package com.icl.cervicalcancercare.holders

import android.content.res.Resources
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.icl.cervicalcancercare.R
import com.icl.cervicalcancercare.databinding.ItemPatientBinding
import com.icl.cervicalcancercare.databinding.ItemPatientImpressionBinding
import com.icl.cervicalcancercare.models.PatientImpression
import com.icl.cervicalcancercare.models.PatientItem
import com.icl.cervicalcancercare.utils.Functions
import java.time.LocalDate
import java.time.Period


class PatientImpressionItemViewHolder(binding: ItemPatientImpressionBinding) :
    RecyclerView.ViewHolder(binding.root) {
    val textLabel: TextView = binding.textLabel

    fun bindTo(
        patientItem: PatientImpression,
        onItemClicked: (PatientImpression) -> Unit,
    ) {
        this.textLabel.text = patientItem.summary
        this.itemView.setOnClickListener { onItemClicked(patientItem) }

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
}