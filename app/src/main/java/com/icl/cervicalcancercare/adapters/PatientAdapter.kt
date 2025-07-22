package com.icl.cervicalcancercare.adapters



import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.icl.cervicalcancercare.databinding.ItemPatientBinding
import com.icl.cervicalcancercare.holders.PatientItemViewHolder
import com.icl.cervicalcancercare.models.PatientItem

/** UI Controller helper class to monitor Patient viewmodel and display list of patients. */
class PatientAdapter(
    private val onItemClicked: (PatientItem) -> Unit,
) :
    ListAdapter<PatientItem, PatientItemViewHolder>(PatientItemDiffCallback()) {

    class PatientItemDiffCallback : DiffUtil.ItemCallback<PatientItem>() {
        override fun areItemsTheSame(
            oldItem: PatientItem,
            newItem: PatientItem,
        ): Boolean = oldItem.resourceId == newItem.resourceId

        override fun areContentsTheSame(
            oldItem: PatientItem,
            newItem: PatientItem,
        ): Boolean = oldItem.id == newItem.id
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PatientItemViewHolder {
        return PatientItemViewHolder(
            ItemPatientBinding.inflate(LayoutInflater.from(parent.context), parent, false),
        )
    }

    override fun onBindViewHolder(holder: PatientItemViewHolder, position: Int) {
        val item = currentList[position]
        holder.bindTo(item, onItemClicked)
    }
}
