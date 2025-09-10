package com.icl.cervicalcancercare.fhir

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.android.fhir.FhirEngine
import com.google.android.fhir.search.search
import com.icl.cervicalcancercare.models.LocationResource
import com.icl.cervicalcancercare.network.Interface
import com.icl.cervicalcancercare.network.RetrofitBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.hl7.fhir.r4.model.Location
import org.hl7.fhir.r4.model.Reference
import org.hl7.fhir.r4.model.Resource


class LocationDownloadedWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {


    override suspend fun doWork(): Result {
        return try {

            val context: Context = applicationContext
            val fhirEngine = FhirApplication.Companion.fhirEngine(context)
            val initialUrl =
                "https://dsrfhir.intellisoftkenya.com/hapi/fhir/Location?_count=200" // first call
            fetchAllPages(initialUrl, context = context, fhirEngine)
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    private suspend fun fetchAllPages(url: String, context: Context, fhirEngine: FhirEngine) {
        val baseUrl = "https://dsrfhir.intellisoftkenya.com/hapi/fhir/"
        val apiService =
            RetrofitBuilder.getRetrofit(baseUrl, context).create(Interface::class.java)

        withContext(Dispatchers.IO) {
            var nextUrl: String? = url
            while (!nextUrl.isNullOrEmpty()) {
                // Fetch page safely in IO dispatcher
                val bundle = withContext(Dispatchers.IO) { apiService.fetchBundle(nextUrl) }

                // Process entries concurrently using map + awaitAll
                val jobs = bundle.entry?.map { entry ->
                    async(Dispatchers.IO) {
                        val loc = Location().apply {
                            id = entry.resource.id
                            name = entry.resource.name
                            partOf = entry.resource.partOf?.let { Reference(it.reference) }
                        }
                        try {
                            // Check if exists and create
                            val existing = fhirEngine.search<Location> {
                                filter(Resource.RES_ID, { value = of(entry.resource.id) })
                            }
                            if (existing.isEmpty()) {
                                fhirEngine.create(loc)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
                jobs?.awaitAll()
                nextUrl = bundle.link?.firstOrNull { it.relation == "next" }?.url
                // Optional: save batch to DB here
            }
            // Final action: save allLocations to DB or process as needed
        }
    }
}
