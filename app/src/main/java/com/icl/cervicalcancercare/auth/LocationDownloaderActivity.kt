package com.icl.cervicalcancercare.auth

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.icl.cervicalcancercare.MainActivity
import com.icl.cervicalcancercare.R
import com.icl.cervicalcancercare.databinding.ActivityLocationDownloaderBinding
import com.icl.cervicalcancercare.fhir.LocationDownloadedWorker
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LocationDownloaderActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLocationDownloaderBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLocationDownloaderBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.apply {
            animationView.playAnimation()
            cancelButton.setOnClickListener {
                // Handle cancellation logic here
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val workRequest = OneTimeWorkRequestBuilder<LocationDownloadedWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED) // Ensure network is available
                    .build()
            )
            .build()

        WorkManager.getInstance(this@LocationDownloaderActivity)
            .enqueueUniqueWork(
                "LocationDownloadWorker",  // unique name to avoid duplicates
                ExistingWorkPolicy.KEEP,   // KEEP = don't run again if already enqueued
                workRequest
            )

        WorkManager.getInstance(this@LocationDownloaderActivity)
            .getWorkInfoByIdLiveData(workRequest.id)
            .observe(this@LocationDownloaderActivity) { workInfo ->
                when (workInfo?.state) {
                    WorkInfo.State.RUNNING -> println("Worker running")
                    WorkInfo.State.SUCCEEDED -> println("Worker finished successfully")
                    WorkInfo.State.FAILED -> println("Worker failed")
                    else -> {}
                }
            }

        lifecycleScope.launch {
            delay(3000)
            val intent = Intent(this@LocationDownloaderActivity, MainActivity::class.java)
            startActivity(intent)
            this@LocationDownloaderActivity.finish()
        }


    }
}