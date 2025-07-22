package com.icl.cervicalcancercare.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.icl.cervicalcancercare.databinding.ItemPatientBinding
import com.icl.cervicalcancercare.databinding.ItemPatientImpressionBinding
import com.icl.cervicalcancercare.holders.PatientImpressionItemViewHolder
import com.icl.cervicalcancercare.holders.PatientItemViewHolder
import com.icl.cervicalcancercare.models.PatientImpression
import com.icl.cervicalcancercare.models.PatientItem

class RecommendationsAdapter(
    private val onItemClicked: (PatientImpression) -> Unit,
) :
    ListAdapter<PatientImpression, PatientImpressionItemViewHolder>(PatientImpressionDiffCallback()) {

    class PatientImpressionDiffCallback : DiffUtil.ItemCallback<PatientImpression>() {
        override fun areItemsTheSame(
            oldItem: PatientImpression,
            newItem: PatientImpression,
        ): Boolean = oldItem.summary == newItem.summary

        override fun areContentsTheSame(
            oldItem: PatientImpression,
            newItem: PatientImpression,
        ): Boolean = oldItem.summary == newItem.summary
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): PatientImpressionItemViewHolder {
        return PatientImpressionItemViewHolder(
            ItemPatientImpressionBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            ),
        )
    }

    override fun onBindViewHolder(holder: PatientImpressionItemViewHolder, position: Int) {
        val item = currentList[position]
        holder.bindTo(item, onItemClicked)
    }
}
