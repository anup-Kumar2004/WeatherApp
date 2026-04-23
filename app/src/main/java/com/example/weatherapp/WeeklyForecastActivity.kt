package com.example.weatherapp

import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import androidx.core.view.WindowCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.weatherapp.adapter.WeeklyForecastAdapter
import com.example.weatherapp.model.WeeklyForecastItem
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.Locale

class WeeklyForecastActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_weekly_forecast)

        // Status bar + navigation bar color setup
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            // API 35+ (Android 15+) — edge-to-edge enforced by system, gradient shows through
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowCompat.getInsetsController(window, window.decorView).apply {
                isAppearanceLightStatusBars = true
                isAppearanceLightNavigationBars = true
            }
        } else {
            // API 28–34 (Android 9 to 14) — set colors directly
            WindowCompat.setDecorFitsSystemWindows(window, false)
            @Suppress("DEPRECATION")
            window.statusBarColor = "#fee5ca".toColorInt()
            @Suppress("DEPRECATION")
            window.navigationBarColor = "#fdbf93".toColorInt()
            WindowCompat.getInsetsController(window, window.decorView).apply {
                isAppearanceLightStatusBars = true
                isAppearanceLightNavigationBars = true
            }
        }

        val rootView = findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.rootWeekly)
        // Note: your weekly forecast root layout id — check activity_weekly_forecast.xml
        // If it has no id, we'll add one in the next step
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, insets ->
            val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, systemBars.top, 0, systemBars.bottom)
            insets
        }


        // ── Unpack Intent data ────────────────────────────────────────────────
        // These are the raw arrays passed from MainActivity
        val times         = intent.getStringArrayExtra("times") ?: emptyArray()
        val weatherCodes  = intent.getIntArrayExtra("weatherCodes") ?: intArrayOf()
        val tempMaxList   = intent.getDoubleArrayExtra("tempMax") ?: doubleArrayOf()
        val tempMinList   = intent.getDoubleArrayExtra("tempMin") ?: doubleArrayOf()
        val precipitation = intent.getDoubleArrayExtra("precipitation") ?: doubleArrayOf()
        val windSpeed     = intent.getDoubleArrayExtra("windSpeed") ?: doubleArrayOf()
        val humidity      = intent.getIntArrayExtra("humidity") ?: intArrayOf()

        // ── Safety check ──────────────────────────────────────────────────────
        // We need at least 7 days of data. If somehow it arrived incomplete,
        // we just finish() gracefully rather than crash with an index error.
        if (times.size < 7 || weatherCodes.size < 7) {
            finish()
            return
        }

        // ── Build the list ────────────────────────────────────────────────────
        val items = mutableListOf<WeeklyForecastItem>()

        // Index 0 = today (skip — already shown on main screen)
        // Index 1 = tomorrow → FeaturedDay
        val tomorrowHumidityIndex = (1 * 24) + 12  // noon of tomorrow in hourly array
        val tomorrowHumidity = if (humidity.size > tomorrowHumidityIndex) {
            humidity[tomorrowHumidityIndex]
        } else {
            0
        }

        items.add(
            WeeklyForecastItem.FeaturedDay(
                label         = "Tomorrow",
                tempMin       = tempMinList[1],
                tempMax       = tempMaxList[1],
                weatherCode   = weatherCodes[1],
                precipitation = precipitation[1],
                windSpeed     = windSpeed[1],
                humidity      = tomorrowHumidity
            )
        )

        // Index 2–7 → SimpleDay rows
        // We parse the date string to get the day name ("Wednesday" etc.)
        val inputFormat  = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outputFormat = SimpleDateFormat("EEEE", Locale.getDefault()) // full day name

        for (i in 2 until minOf(times.size, 8)) {
            val dayName = try {
                val date = inputFormat.parse(times[i])
                outputFormat.format(date!!)
            } catch (_: Exception) {
                "Day $i"  // safe fallback if parsing fails
            }

            items.add(
                WeeklyForecastItem.SimpleDay(
                    dayName     = dayName,
                    tempMin     = tempMinList[i],
                    tempMax     = tempMaxList[i],
                    weatherCode = weatherCodes[i]
                )
            )
        }

        // ── Wire up RecyclerView ──────────────────────────────────────────────
        val rvWeekly = findViewById<RecyclerView>(R.id.rvWeekly)
        rvWeekly.layoutManager = LinearLayoutManager(this)
        rvWeekly.adapter = WeeklyForecastAdapter(items)

        // ── Back button ───────────────────────────────────────────────────────
        // finish() is all you need — Android handles the back stack automatically.
        // No need to start any new Activity.
        findViewById<MaterialCardView>(R.id.btnBack).setOnClickListener {
            finish()
        }
    }
}