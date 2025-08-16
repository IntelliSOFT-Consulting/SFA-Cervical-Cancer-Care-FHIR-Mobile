package com.icl.cervicalcancercare.details.child

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.gson.Gson
import com.icl.cervicalcancercare.R
import com.icl.cervicalcancercare.databinding.ActivityRecommendationDetailsBinding
import com.icl.cervicalcancercare.models.PatientImpression

class RecommendationDetailsActivity : AppCompatActivity() {
    // lets use binding
    private lateinit var binding: ActivityRecommendationDetailsBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityRecommendationDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar.apply { title = "Recommendation" }
        val json = intent.getStringExtra("impression_json")
        val impression = Gson().fromJson(json, PatientImpression::class.java)

        binding.apply {

            if (impression.updatedData.isNotEmpty()) {
                impression.updatedData.forEach {
                    val fieldView = createCustomField(it.question, true)
                    val answerView = createCustomField(it.answer, false)
                    parentLayout.addView(fieldView)
                    parentLayout.addView(answerView)
                }
            } else {

                impression.basis.forEach {
                    // add a label for each basis
                    val fieldView = createCustomField(it, false)
                    parentLayout.addView(fieldView)
                }
            }
        }


    }

    private fun createCustomField(string: String, isBold: Boolean): View {
        // Create the main LinearLayout to hold the views
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(8, 8, 8, 8)
        }

        // Create the horizontal LinearLayout for the two TextViews
        val horizontalLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // First TextView (label)
        val label = TextView(this).apply {
            text = string
            textSize = if (isBold) 14f else 12f
            typeface = if (isBold) {
                ResourcesCompat.getFont(this@RecommendationDetailsActivity, R.font.intersemi)

            } else {
                ResourcesCompat.getFont(this@RecommendationDetailsActivity, R.font.inter)
            }
            layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            )
        }
        horizontalLayout.addView(label)

        // Add the horizontal layout with two TextViews to the main layout
        layout.addView(horizontalLayout)

        // Add a separator View (divider)
        val divider = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                1 // Divider thickness
            ).apply {
                topMargin = 8
                bottomMargin = 8
            }
            setBackgroundColor(android.graphics.Color.parseColor("#CCCCCC"))
        }
        layout.addView(divider)

        return layout
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}