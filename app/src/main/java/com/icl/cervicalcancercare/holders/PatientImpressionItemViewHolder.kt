package com.icl.cervicalcancercare.holders

import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.icl.cervicalcancercare.R
import com.icl.cervicalcancercare.databinding.ItemPatientImpressionBinding
import com.icl.cervicalcancercare.models.PatientImpression
import java.util.Locale

class PatientImpressionItemViewHolder(
    private val binding: ItemPatientImpressionBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bindTo(
        patientItem: PatientImpression,
        onItemClicked: (PatientImpression) -> Unit,
    ) = with(binding) {
        textLabel.text = patientItem.summary
        detailText.text = buildSubtitle(patientItem)

        statusChip.isVisible = patientItem.status.isNotBlank()
        if (statusChip.isVisible) {
            statusChip.text = patientItem.status.toDisplayStatus()
        }

        itemView.setOnClickListener { onItemClicked(patientItem) }
    }

    private fun buildSubtitle(patientItem: PatientImpression): String {
        val context = binding.root.context
        val firstBasis = patientItem.basis.firstOrNull()?.trim().orEmpty()
        val actionCount = patientItem.updatedData.size

        return when {
            actionCount > 0 && firstBasis.isNotBlank() ->
                context.getString(R.string.recommendation_item_action_count, actionCount) +
                    " • " + firstBasis

            actionCount > 0 ->
                context.getString(R.string.recommendation_item_action_count, actionCount)

            firstBasis.isNotBlank() -> firstBasis
            else -> context.getString(R.string.recommendation_item_tap_to_review)
        }
    }

    private fun String.toDisplayStatus(): String {
        return lowercase(Locale.getDefault()).replaceFirstChar { char ->
            if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString()
        }
    }
}
