package com.icl.cervicalcancercare.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.search.Order
import com.google.android.fhir.search.StringFilterModifier
import com.google.android.fhir.search.count
import com.google.android.fhir.search.search
import com.icl.cervicalcancercare.models.PatientItem
import java.time.ZoneId
import kotlinx.coroutines.launch
import org.hl7.fhir.r4.model.ContactPoint
import org.hl7.fhir.r4.model.Patient

/**
 * The ViewModel helper class for PatientItemRecyclerViewAdapter, that is responsible for preparing
 * data for UI.
 */
class PatientListViewModel(application: Application, private val fhirEngine: FhirEngine) :
    AndroidViewModel(application) {
    val liveSearchedPatients = MutableLiveData<List<PatientItem>>()
    val patientCount = MutableLiveData<Long>()

    init {
        updatePatientListAndPatientCount({ getSearchResults() }, { searchedPatientCount() })
    }

    fun searchPatientsByName(nameQuery: String) {
        updatePatientListAndPatientCount({ getSearchResults(nameQuery) }, { count(nameQuery) })
    }

    /**
     * [updatePatientListAndPatientCount] calls the search and count lambda and updates the live data
     * values accordingly. It is initially called when this [ViewModel] is created. Later its called
     * by the client every time search query changes or data-sync is completed.
     */
    private fun updatePatientListAndPatientCount(
        search: suspend () -> List<PatientItem>,
        count: suspend () -> Long,
    ) {
        viewModelScope.launch {
            liveSearchedPatients.value = search()
            patientCount.value = count()
        }
    }

    /**
     * Returns count of all the [Patient] who match the filter criteria unlike [getSearchResults]
     * which only returns a fixed range.
     */
    private suspend fun count(nameQuery: String = ""): Long {
        return fhirEngine.count<Patient> {
            if (nameQuery.isNotEmpty()) {
                filter(
                    Patient.NAME,
                    {
                        modifier = StringFilterModifier.CONTAINS
                        value = nameQuery
                    },
                )
            }
        }
    }

    private suspend fun getSearchResults(nameQuery: String = ""): List<PatientItem> {
        val patients: MutableList<PatientItem> = mutableListOf()
        fhirEngine
            .search<Patient> {
                if (nameQuery.isNotEmpty()) {
                    filter(
                        Patient.NAME,
                        {
                            modifier = StringFilterModifier.CONTAINS
                            value = nameQuery
                        },
                    )
                }
                sort(Patient.GIVEN, Order.ASCENDING)
                count = 100
                from = 0
            }
            .mapIndexed { index, fhirPatient -> fhirPatient.resource.toPatientItem(index + 1) }
            .let { patients.addAll(it) }

        return patients
    }


    class PatientListViewModelFactory(
        private val application: Application,
        private val fhirEngine: FhirEngine,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(PatientListViewModel::class.java)) {
                return PatientListViewModel(application, fhirEngine) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }

    private var patientGivenName: String? = null
    private var patientFamilyName: String? = null

    fun setPatientGivenName(givenName: String) {
        patientGivenName = givenName
        searchPatientsByParameter()
    }

    fun setPatientFamilyName(familyName: String) {
        patientFamilyName = familyName
        searchPatientsByParameter()
    }

    private fun searchPatientsByParameter() {
        viewModelScope.launch {
            liveSearchedPatients.value = searchPatients()
            patientCount.value = searchedPatientCount()
        }
    }

    private suspend fun searchPatients(): List<PatientItem> {
        val patients =
            fhirEngine
                .search<Patient> {
                    filter(
                        Patient.GIVEN,
                        {
                            modifier = StringFilterModifier.CONTAINS
                            this.value = patientGivenName ?: ""
                        },
                    )
                    filter(
                        Patient.FAMILY,
                        {
                            modifier = StringFilterModifier.CONTAINS
                            this.value = patientFamilyName ?: ""
                        },
                    )
                    sort(Patient.GIVEN, Order.ASCENDING)
                    count = 100
                    from = 0
                }
                .mapIndexed { index, fhirPatient -> fhirPatient.resource.toPatientItem(index + 1) }
                .toMutableList()

        return patients
    }

    private suspend fun searchedPatientCount(): Long {
        return fhirEngine.count<Patient> {
            filter(
                Patient.GIVEN,
                {
                    modifier = StringFilterModifier.CONTAINS
                    this.value = patientGivenName ?: ""
                },
            )
            filter(
                Patient.FAMILY,
                {
                    modifier = StringFilterModifier.CONTAINS
                    this.value = patientFamilyName ?: ""
                },
            )
        }
    }
}

internal fun Patient.toPatientItem(position: Int): PatientItem {
    // Show nothing if no values available for gender and date of birth.
    val patientId = if (hasIdElement()) idElement.idPart else ""
    val name = if (hasName()) name[0].nameAsSingleString else ""
    val gender = if (hasGenderElement()) genderElement.valueAsString else ""
    val dob =
        if (hasBirthDateElement()) {
            birthDateElement.value.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
        } else {
            null
        }
    val phone =
        if (hasTelecom()) telecom.filter { telecom -> telecom.system == ContactPoint.ContactPointSystem.PHONE }
            .map { telecom -> telecom.value }.firstOrNull() ?: "" else ""
    val email =
        if (hasTelecom()) telecom.filter { telecom -> telecom.system == ContactPoint.ContactPointSystem.EMAIL }
            .map { telecom -> telecom.value }.firstOrNull() ?: "" else ""

    val isActive = active
    val html: String = if (hasText()) text.div.valueAsString else ""
    val identificationType =
        if (hasIdentifier()) identifier[0].type.coding[0].display else "National ID Number"
    val identificationNumber =
        if (hasIdentifier()) identifier[0].value else ""

    val county = if (hasAddress()) addressFirstRep.city else ""
    val sub_county = if (hasAddress()) addressFirstRep.district else ""
    val ward = if (hasAddress()) addressFirstRep.state else ""


    return PatientItem(
        id = position.toString(),
        resourceId = patientId,
        name = name,
        gender = gender ?: "",
        dob = dob,
        phone = phone ?: "",
        email = email ?: "",
        isActive = isActive,
        html = html,
        identificationType = identificationType,
        identificationNumber = identificationNumber,
        county = county,
        sub_county = sub_county,
        ward = ward
    )
}