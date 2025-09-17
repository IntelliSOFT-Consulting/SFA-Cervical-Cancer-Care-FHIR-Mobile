package com.icl.cervicalcancercare.viewmodels.factories

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.icl.cervicalcancercare.viewmodels.EditResponseViewModel

class EditResponseViewModelFactory (
    private val application: Application,
    private val questionnaireId: String,
    private val questionnaire: String
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EditResponseViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return EditResponseViewModel(
                application,
                questionnaireId,
                questionnaire
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
