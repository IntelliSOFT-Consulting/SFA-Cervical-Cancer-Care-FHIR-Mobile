package com.icl.cervicalcancercare.holders

import android.content.res.Resources
import com.icl.cervicalcancercare.databinding.ItemPatientBinding
import com.icl.cervicalcancercare.models.PatientItem
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.icl.cervicalcancercare.R
import com.icl.cervicalcancercare.utils.Functions
import java.time.LocalDate
import java.time.Period

class PatientItemViewHolder(binding: ItemPatientBinding) :
    RecyclerView.ViewHolder(binding.root) {
    val profilePic: ImageView = binding.profileImage
    val name: TextView = binding.nameText
    val dobAge: TextView = binding.dobText
    val nationalId: TextView = binding.idText

    fun bindTo(
        patientItem: PatientItem,
        onItemClicked: (PatientItem) -> Unit,
    ) {
        this.name.text = patientItem.name
        this.dobAge.text = " DOB: ${patientItem.dob}  (${
            getFormattedAge(
                patientItem,
                this.dobAge.context.resources
            )
        })"
        this.nationalId.text =
            " ${patientItem.identificationType} :  ${patientItem.identificationNumber}"
        try {
            val initials = Functions().getInitials(patientItem.name)
            val avatarBitmap = Functions().createAvatar(initials)
            this.profilePic.setImageBitmap(avatarBitmap)
        } catch (e: Exception) {
            e.printStackTrace()
        }
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