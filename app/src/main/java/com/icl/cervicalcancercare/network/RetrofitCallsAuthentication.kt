package com.icl.cervicalcancercare.network

import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.icl.cervicalcancercare.MainActivity
import com.icl.cervicalcancercare.R
import com.icl.cervicalcancercare.auth.LocationDownloaderActivity
import com.icl.cervicalcancercare.models.ExtractedData
import com.icl.cervicalcancercare.models.Login
import com.icl.cervicalcancercare.models.Payload
import com.icl.cervicalcancercare.models.UrlData
import com.icl.cervicalcancercare.utils.Functions
import com.icl.cervicalcancercare.viewmodels.AddPatientViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class RetrofitCallsAuthentication {
    fun loginUser(context: Context, dbSignIn: Login) {

        CoroutineScope(Dispatchers.Main).launch {
            val job = Job()
            CoroutineScope(Dispatchers.IO + job).launch { startLogin(context, dbSignIn) }.join()
        }
    }

    fun performAssessment(
        context: Context,
        result: ExtractedData,
        viewModel: AddPatientViewModel
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            val job = Job()
            val encounterId = Functions().generateUuid()
            CoroutineScope(Dispatchers.IO + job).launch {
                handleFhirProcessing(context, result, viewModel, encounterId)
                handleDataProcessing(context, result, viewModel, encounterId)
            }
                .join()
        }
    }

    fun performUpdatedAssessment(
        context: Context,
        result: Payload,
        viewModel: AddPatientViewModel
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            val job = Job()
            val encounterId = Functions().generateUuid()
            CoroutineScope(Dispatchers.IO + job).launch {
                //   handleFhirProcessing(context, result, viewModel,encounterId)
                handleUpdatedDataProcessing(context, result, viewModel, encounterId)
            }
                .join()
        }
    }

    fun handleUpdatedFhirProcessing(
        context: Context,
        result: Payload,
        viewModel: AddPatientViewModel,
        encounterId: String
    ) {
        val job = Job()
        CoroutineScope(Dispatchers.IO + job)
            .launch {
                //  viewModel.createPatientObservations(context, result,encounterId)
            }
    }

    fun handleFhirProcessing(
        context: Context,
        result: ExtractedData,
        viewModel: AddPatientViewModel,
        encounterId: String
    ) {
        val job = Job()
        CoroutineScope(Dispatchers.IO + job)
            .launch {
                viewModel.createPatientObservations(context, result, encounterId)
            }
    }

    fun extractAllSections(content: String): Map<String, String> {
        val regex = Regex("""\*\*\s*(.*?)\s*\*\*\n(.*?)(?=(\*\*|$))""", RegexOption.DOT_MATCHES_ALL)
        return regex.findAll(content).associate {
            val key = it.groups[1]?.value?.trim() ?: ""
            val value = it.groups[2]?.value?.trim() ?: ""
            key to value
        }
    }

    fun extractSection(content: String, sectionTitle: String): String? {
        val regex = Regex(
            """\*\*\s*${Regex.escape(sectionTitle)}\s*\*\*\n(.*?)(?=(\*\*|$))""",
            RegexOption.DOT_MATCHES_ALL
        )
        return regex.find(content)?.groups?.get(1)?.value?.trim()
    }

    private fun handleUpdatedDataProcessing(
        context: Context,
        data: Payload,
        viewModel: AddPatientViewModel,
        encounterId: String
    ) {

        val job1 = Job()
        CoroutineScope(Dispatchers.Main + job1).launch {

            val dialogView = LayoutInflater.from(context).inflate(R.layout.loading_dialog, null)
            val alertDialog = AlertDialog.Builder(context)
                .setView(dialogView)
                .setCancelable(false)
                .create()
            alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            alertDialog.show()
//            val progressDialog = ProgressDialog(context)
//            progressDialog.setTitle("Please wait..")
//            progressDialog.setMessage("Processing data...")
//            progressDialog.setCanceledOnTouchOutside(false)
//            progressDialog.show()

            var messageToast = ""
            val job = Job()
            CoroutineScope(Dispatchers.IO + job)
                .launch {
                    val formatter = FormatterClass()
                    val token = formatter.getSharedPref("access_token", context)
                    val baseUrl = context.getString(UrlData.BASE_URL.message)
                    val apiService =
                        RetrofitBuilder.getRetrofit(baseUrl, context).create(Interface::class.java)
                    try {

                        val apiInterface = apiService.processUpdatedData(data, "Bearer $token")
                        if (apiInterface.isSuccessful) {

                            val statusCode = apiInterface.code()
                            val body = apiInterface.body()

                            if (statusCode == 200 || statusCode == 201) {

                                if (body != null) {

                                    val response = body.response
                                    messageToast = "Request successful.."

                                    val input = extractAllSections(response)

                                    val resourceId =
                                        FormatterClass().getSharedPref("resourceId", context)
                                    viewModel.createUpdatedRecommendations(
                                        input,
                                        "$resourceId",
                                        data,
                                        encounterId
                                    )


                                    if (context is Activity) {
                                        context.finish()
                                    }
                                } else {
                                    messageToast = "Error: Body is null"
                                }
                            } else {
                                messageToast = "Error: The request was not successful"
                            }
                        } else {
                            apiInterface.errorBody()?.let {
                                messageToast =
                                    "Encountered problems processing data. Please try again."
                            }
                        }
                    } catch (e: Exception) {
                        messageToast = "Error encountered processing data, please try again"
                    }
                }
                .join()
            CoroutineScope(Dispatchers.Main).launch {
                if (alertDialog.isShowing) {
                    alertDialog.dismiss()
                }
                Toast.makeText(context, messageToast, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun handleDataProcessing(
        context: Context,
        data: ExtractedData,
        viewModel: AddPatientViewModel,
        encounterId: String
    ) {

        val job1 = Job()
        CoroutineScope(Dispatchers.Main + job1).launch {
            val progressDialog = ProgressDialog(context)
            progressDialog.setTitle("Please wait..")
            progressDialog.setMessage("Processing data...")
            progressDialog.setCanceledOnTouchOutside(false)
            progressDialog.show()

            var messageToast = ""
            val job = Job()
            CoroutineScope(Dispatchers.IO + job)
                .launch {
                    val formatter = FormatterClass()
                    val token = formatter.getSharedPref("access_token", context)
                    val baseUrl = context.getString(UrlData.BASE_URL.message)
                    val apiService =
                        RetrofitBuilder.getRetrofit(baseUrl, context).create(Interface::class.java)
                    try {

                        val apiInterface = apiService.processData(data, "Bearer $token")
                        if (apiInterface.isSuccessful) {

                            val statusCode = apiInterface.code()
                            val body = apiInterface.body()

                            if (statusCode == 200 || statusCode == 201) {

                                if (body != null) {

                                    val response = body.response
                                    messageToast = "Request successful.."

                                    val input = extractAllSections(response)

                                    val resourceId =
                                        FormatterClass().getSharedPref("resourceId", context)
                                    viewModel.createRecommendations(
                                        input,
                                        "$resourceId",
                                        data,
                                        encounterId
                                    )


                                    if (context is Activity) {
                                        context.finish()
                                    }
                                } else {
                                    messageToast = "Error: Body is null"
                                }
                            } else {
                                messageToast = "Error: The request was not successful"
                            }
                        } else {
                            apiInterface.errorBody()?.let {
                                messageToast =
                                    "Encountered problems processing data. Please try again."
                            }
                        }
                    } catch (e: Exception) {
                        messageToast = "Error encountered processing data, please try again"
                    }
                }
                .join()
            CoroutineScope(Dispatchers.Main).launch {
                progressDialog.dismiss()
                Toast.makeText(context, messageToast, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun startLogin(context: Context, dbSignIn: Login) {

        val job1 = Job()
        CoroutineScope(Dispatchers.Main + job1).launch {
            val progressDialog = ProgressDialog(context)
            progressDialog.setTitle("Please wait..")
            progressDialog.setMessage("Authentication in progress..")
            progressDialog.setCanceledOnTouchOutside(false)
            progressDialog.show()

            var messageToast = ""
            val job = Job()
            CoroutineScope(Dispatchers.IO + job)
                .launch {
                    val formatter = FormatterClass()
                    val baseUrl = context.getString(UrlData.BASE_URL.message)
                    val apiService =
                        RetrofitBuilder.getRetrofit(baseUrl, context).create(Interface::class.java)
                    try {

                        val apiInterface = apiService.signInUser(dbSignIn)
                        if (apiInterface.isSuccessful) {

                            val statusCode = apiInterface.code()
                            val body = apiInterface.body()

                            if (statusCode == 200 || statusCode == 201) {

                                if (body != null) {

                                    val access_token = body.access_token
                                    formatter.saveSharedPref("access_token", access_token, context)
                                    formatter.saveSharedPref("isLoggedIn", "true", context)
                                    formatter.saveSharedPref("userName", dbSignIn.username, context)
                                    messageToast = "Login successful.."
                                    val intent =
                                        Intent(context, LocationDownloaderActivity::class.java)
                                    intent.addFlags(
                                        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    )
                                    context.startActivity(intent)
                                    if (context is Activity) {
                                        context.finish()
                                    }
                                } else {
                                    messageToast = "Error: Body is null"
                                }
                            } else {
                                messageToast = "Error: The request was not successful"
                            }
                        } else {
                            apiInterface.errorBody()?.let {
                                messageToast =
                                    "Invalid login credentials. Please try again." // errorBody.getString("error")
                            }
                        }
                    } catch (e: Exception) {
                        messageToast = "Cannot login user.."
                    }
                }
                .join()
            CoroutineScope(Dispatchers.Main).launch {
                progressDialog.dismiss()
                Toast.makeText(context, messageToast, Toast.LENGTH_LONG).show()
            }
        }
    }
}
