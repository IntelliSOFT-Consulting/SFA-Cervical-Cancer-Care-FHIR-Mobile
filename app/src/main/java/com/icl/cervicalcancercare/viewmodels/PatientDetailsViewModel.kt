package com.icl.cervicalcancercare.viewmodels


import android.app.Application
import android.content.res.Resources
import android.icu.text.DateFormat
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.SearchResult
import com.google.android.fhir.datacapture.extensions.asStringValue
import com.google.android.fhir.datacapture.extensions.logicalId
import com.google.android.fhir.search.Order
import com.google.android.fhir.search.revInclude
import com.google.android.fhir.search.search
import com.icl.cervicalcancercare.models.PatientImpression
import com.icl.cervicalcancercare.models.PatientItem
import com.icl.cervicalcancercare.models.PatientSummary
import com.icl.cervicalcancercare.models.Reco
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.hl7.fhir.r4.model.ClinicalImpression
import org.hl7.fhir.r4.model.Coding
import org.hl7.fhir.r4.model.Condition
import org.hl7.fhir.r4.model.ContactPoint
import org.hl7.fhir.r4.model.Encounter
import org.hl7.fhir.r4.model.Observation
import org.hl7.fhir.r4.model.Patient
import org.hl7.fhir.r4.model.Resource
import org.hl7.fhir.r4.model.ResourceType
import kotlin.String

class PatientDetailsViewModel(
    application: Application,
    private val fhirEngine: FhirEngine,
    private val patientId: String,
) : AndroidViewModel(application) {
    val livePatientData = MutableLiveData<PatientSummary>()

    fun getPatientDetailData() {
        viewModelScope.launch {
            livePatientData.value = getPatientDetailDataModel()
        }
    }


    private suspend fun getPatientDetailDataModel(
    ): PatientSummary {
        val searchResult =
            fhirEngine.search<Patient> {
                filter(Resource.RES_ID, { value = of(patientId) })
                revInclude<Observation>(Observation.SUBJECT)
                revInclude<Condition>(Condition.SUBJECT)
                revInclude<Encounter>(Encounter.SUBJECT)
                revInclude<ClinicalImpression>(ClinicalImpression.SUBJECT)
            }
        var data = PatientSummary(
            basic = null,

            )

        searchResult.first().let {

            it.revIncluded?.get(ResourceType.ClinicalImpression to ClinicalImpression.SUBJECT.paramName)
                ?.let {
                    val impressions = it
                        .filterIsInstance<ClinicalImpression>()
                        .map { impression ->
                            // get the list of string as basis
                            val findings = impression.finding.map { finding ->
                                finding.basis
                            }
                            val code = impression.finding.map { dt ->
                                dt.itemCodeableConcept
                            }
                            // let's create a list of Reco
                            val recoList = mutableListOf<Reco>()
                            impression.finding.forEach { r ->
                                recoList.add(
                                    Reco(
                                        r?.itemCodeableConcept?.codingFirstRep?.display ?: "",
                                        r.basis
                                    )
                                )
                            }
                            PatientImpression(
                                impression.status?.display ?: "Unknown",
                                basis = findings,
                                summary = impression.summary ?: "No Summary provided",
                                updatedData = recoList
                            )
                        }

                    data = data.copy(impressions = impressions.reversed())
                }

            val patientId = if (it.resource.hasIdElement()) it.resource.idElement.idPart else ""
            val name = if (it.resource.hasName()) it.resource.name[0].nameAsSingleString else ""
            val gender =
                if (it.resource.hasGenderElement()) it.resource.genderElement.valueAsString else ""
            val dob =
                if (it.resource.hasBirthDateElement()) {
                    it.resource.birthDateElement.value.toInstant().atZone(ZoneId.systemDefault())
                        .toLocalDate()
                } else {
                    null
                }
            val phone =
                if (it.resource.hasTelecom()) it.resource.telecom.filter { telecom -> telecom.system == ContactPoint.ContactPointSystem.PHONE }
                    .map { telecom -> telecom.value }.firstOrNull() ?: "" else ""
            val email =
                if (it.resource.hasTelecom()) it.resource.telecom.filter { telecom -> telecom.system == ContactPoint.ContactPointSystem.EMAIL }
                    .map { telecom -> telecom.value }.firstOrNull() ?: "" else ""


            val isActive = it.resource.active
            val html: String = if (it.resource.hasText()) it.resource.text.div.valueAsString else ""
            val identificationType =
                if (it.resource.hasIdentifier()) it.resource.identifier[0].type.coding[0].display else "National ID Number"
            val identificationNumber =
                if (it.resource.hasIdentifier()) it.resource.identifier[0].value else ""
            val county = if (it.resource.hasAddress()) it.resource.addressFirstRep.city else ""
            val sub_county =
                if (it.resource.hasAddress()) it.resource.addressFirstRep.district else ""
            val ward = if (it.resource.hasAddress()) it.resource.addressFirstRep.state else ""

            data = data.copy(
                basic = PatientItem(
                    id = patientId,
                    resourceId = patientId,
                    name = name,
                    gender = gender,
                    phone = phone,
                    email = email,
                    dob = dob,
                    isActive = isActive,
                    html = html,
                    identificationType = identificationType,
                    identificationNumber = identificationNumber,
                    county = county,
                    sub_county = sub_county,
                    ward = ward
                )
            )

            it.revIncluded?.get(ResourceType.Encounter to Encounter.SUBJECT.paramName)?.let {
            }
        }

        return data
    }


}
