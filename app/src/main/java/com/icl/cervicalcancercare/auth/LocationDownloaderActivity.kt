package com.icl.cervicalcancercare.auth

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.google.android.fhir.sync.Sync
import com.icl.cervicalcancercare.MainActivity
import com.icl.cervicalcancercare.R
import com.icl.cervicalcancercare.databinding.ActivityLocationDownloaderBinding
import com.icl.cervicalcancercare.fhir.FhirSyncWorker
import com.icl.cervicalcancercare.fhir.LocationDownloadedWorker
import com.icl.cervicalcancercare.network.FormatterClass
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

class LocationDownloaderActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLocationDownloaderBinding
    private val workManager by lazy { WorkManager.getInstance(this) }
    private val formatter = FormatterClass()

    private var hasNavigated = false
    private var activeWorkId: UUID? = null
    private var autoNavigateJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLocationDownloaderBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applySystemBarAppearance()

        applyWindowInsets()
        setUiActions()

        if (isInitialLocationSyncComplete()) {
            navigateToMain()
            return
        }

        observeLocationSyncWork()
        enqueueLocationSyncWork(replaceExisting = false)
        lifecycleScope.launch {
            Sync.oneTimeSync<FhirSyncWorker>(this@LocationDownloaderActivity)
        }

    }

    private fun applyWindowInsets() {
        val initialPaddingLeft = binding.main.paddingLeft
        val initialPaddingTop = binding.main.paddingTop
        val initialPaddingRight = binding.main.paddingRight
        val initialPaddingBottom = binding.main.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                initialPaddingLeft + systemBars.left,
                initialPaddingTop + systemBars.top,
                initialPaddingRight + systemBars.right,
                initialPaddingBottom + systemBars.bottom
            )
            insets
        }
    }

    private fun setUiActions() {
        binding.animationView.playAnimation()
        binding.cancelButton.setOnClickListener {
            navigateToMain()
        }
    }

    private fun enqueueLocationSyncWork(replaceExisting: Boolean) {
        val workRequest = OneTimeWorkRequestBuilder<LocationDownloadedWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        activeWorkId = workRequest.id

        workManager.enqueueUniqueWork(
            LOCATION_SYNC_WORK_NAME,
            if (replaceExisting) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP,
            workRequest
        )
    }

    private fun observeLocationSyncWork() {
        workManager.getWorkInfosForUniqueWorkLiveData(LOCATION_SYNC_WORK_NAME)
            .observe(this) { workInfos ->
                val workInfo = resolveWorkInfo(workInfos) ?: return@observe
                renderWorkState(workInfo.state)
            }
    }

    private fun resolveWorkInfo(workInfos: List<WorkInfo>): WorkInfo? {
        if (workInfos.isEmpty()) return null

        activeWorkId?.let { id ->
            workInfos.firstOrNull { it.id == id }?.let { return it }
        }

        return workInfos.firstOrNull { !it.state.isFinished } ?: workInfos.firstOrNull()
    }

    private fun renderWorkState(state: WorkInfo.State) {
        when (state) {
            WorkInfo.State.ENQUEUED -> {
                updateSyncUi(
                    statusRes = R.string.location_sync_enqueued,
                    detailsRes = R.string.location_sync_running_detail,
                    showAction = false,
                    showProgress = true,
                    keepAnimating = true
                )
            }

            WorkInfo.State.BLOCKED -> {
                updateSyncUi(
                    statusRes = R.string.location_sync_blocked,
                    detailsRes = R.string.location_sync_running_detail,
                    showAction = false,
                    showProgress = true,
                    keepAnimating = true
                )
            }

            WorkInfo.State.RUNNING -> {
                updateSyncUi(
                    statusRes = R.string.location_sync_running,
                    detailsRes = R.string.location_sync_running_detail,
                    showAction = false,
                    showProgress = true,
                    keepAnimating = true
                )
            }

            WorkInfo.State.SUCCEEDED -> {
                markInitialLocationSyncComplete()
                updateSyncUi(
                    statusRes = R.string.location_sync_success,
                    detailsRes = R.string.location_sync_success_detail,
                    showAction = false,
                    showProgress = false,
                    keepAnimating = false
                )
                scheduleAutoNavigate()
            }

            WorkInfo.State.FAILED,
            WorkInfo.State.CANCELLED -> {
                autoNavigateJob?.cancel()
                updateSyncUi(
                    statusRes = R.string.location_sync_failed,
                    detailsRes = R.string.location_sync_failed_detail,
                    showAction = true,
                    showProgress = false,
                    keepAnimating = false
                )
            }
        }
    }

    private fun updateSyncUi(
        statusRes: Int,
        detailsRes: Int,
        showAction: Boolean,
        showProgress: Boolean,
        keepAnimating: Boolean,
    ) {
        binding.syncingText.setText(statusRes)
        binding.syncDetailsText.setText(detailsRes)

        binding.syncProgressIndicator.visibility = if (showProgress) View.VISIBLE else View.GONE
        binding.cancelButton.visibility = if (showAction) View.VISIBLE else View.GONE

        if (keepAnimating) {
            binding.animationView.playAnimation()
        } else {
            binding.animationView.pauseAnimation()
        }
    }

    private fun scheduleAutoNavigate() {
        if (hasNavigated || autoNavigateJob?.isActive == true) return

        autoNavigateJob = lifecycleScope.launch {
            delay(700)
            navigateToMain()
        }
    }

    private fun navigateToMain() {
        if (hasNavigated) return

        hasNavigated = true
        autoNavigateJob?.cancel()

        startActivity(Intent(this@LocationDownloaderActivity, MainActivity::class.java))
        finish()
    }

    private fun isInitialLocationSyncComplete(): Boolean {
        return formatter.getSharedPref(PREF_KEY_INITIAL_LOCATION_SYNC_COMPLETED, this)
            .toBoolean()
    }

    private fun markInitialLocationSyncComplete() {
        formatter.saveSharedPref(PREF_KEY_INITIAL_LOCATION_SYNC_COMPLETED, "true", this)
    }

    companion object {
        private const val LOCATION_SYNC_WORK_NAME = "LocationDownloadWorker"
        const val PREF_KEY_INITIAL_LOCATION_SYNC_COMPLETED = "initial_location_sync_completed"
    }

    private fun applySystemBarAppearance() {
        val isNightMode =
            (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                    Configuration.UI_MODE_NIGHT_YES
        WindowCompat.getInsetsController(window, window.decorView)?.apply {
            isAppearanceLightStatusBars = !isNightMode
            isAppearanceLightNavigationBars = !isNightMode
        }
    }
}
