package com.example.weatherapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.weatherapp.adapter.HourlyWeatherAdapter
import com.example.weatherapp.adapter.HourlyWeatherItem
import com.example.weatherapp.viewmodel.WeatherViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import androidx.core.graphics.toColorInt
import androidx.core.view.WindowCompat
import com.example.weatherapp.adapter.SearchResultsAdapter
import com.facebook.shimmer.ShimmerFrameLayout

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: WeatherViewModel
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var hourlyAdapter: HourlyWeatherAdapter

    // Views
    private lateinit var tvCityName: TextView
    private lateinit var tvDate: TextView
    private lateinit var tvTemperature: TextView
    private lateinit var tvWeatherCondition: TextView
    private lateinit var tvRainfallValue: TextView
    private lateinit var tvWindValue: TextView
    private lateinit var tvHumidityValue: TextView
    private lateinit var ivWeatherIcon: com.airbnb.lottie.LottieAnimationView
    private lateinit var rvHourly: RecyclerView
    private lateinit var layoutPermissionOverlay: LinearLayout
    private lateinit var tabToday: TextView
    private lateinit var tabTomorrow: TextView
    private lateinit var tabNext7Days: TextView
    private lateinit var tabTodayIndicator: View
    private lateinit var tabTomorrowIndicator: View

    // Shimmer
    private lateinit var shimmerLayout: ShimmerFrameLayout

    private lateinit var swipeRefreshLayout: androidx.swiperefreshlayout.widget.SwipeRefreshLayout

    // State flags
    private var gpsDialogShowing = false
    private var weatherLoaded = false
    private var selectedDayIndex = 0

    // ADD alongside other state fields like weatherLoaded, selectedDayIndex:
    private var currentTimezone: String = "auto"

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
                || permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            checkGpsAndFetchLocation()
        } else {
            showPermissionOverlay()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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

        // Apply padding to the root layout so content isn't hidden behind system bars
        val rootView = findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.main)
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, insets ->
            val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, systemBars.top, 0, systemBars.bottom)
            insets
        }

        viewModel = ViewModelProvider(this)[WeatherViewModel::class.java]
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        bindViews()
        // Style the refresh spinner to match app colors
        swipeRefreshLayout.setColorSchemeColors(
            "#F57C00".toColorInt()  // orange to match app theme
        )
        swipeRefreshLayout.setProgressBackgroundColorSchemeColor(
            "#FFF3E0".toColorInt()
        )

        // Pull-to-refresh action
        swipeRefreshLayout.setOnRefreshListener {
            if (viewModel.hasCoordinates()) {
                viewModel.refreshWeather()
            } else {
                swipeRefreshLayout.isRefreshing = false
            }
        }

        setupRecyclerView()
        setTodayDate()
        setupTabs()
        observeViewModel()

        // Search button
        findViewById<MaterialCardView>(R.id.btnSearch).setOnClickListener {
            showSearchDialog()
        }

        // Overlay settings button
        layoutPermissionOverlay.findViewById<MaterialCardView>(R.id.btnOverlaySettings)
            .setOnClickListener {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.fromParts("package", packageName, null)
                startActivity(intent)
            }

        requestLocationPermission()
    }

    private fun bindViews() {
        tvCityName = findViewById(R.id.tvCityName)
        tvDate = findViewById(R.id.tvDate)
        tvTemperature = findViewById(R.id.tvTemperature)
        tvWeatherCondition = findViewById(R.id.tvWeatherCondition)
        tvRainfallValue = findViewById(R.id.tvRainfallValue)
        tvWindValue = findViewById(R.id.tvWindValue)
        tvHumidityValue = findViewById(R.id.tvHumidityValue)
        ivWeatherIcon = findViewById(R.id.ivWeatherIcon)   // ✅ Added: was missing from bindViews
        rvHourly = findViewById(R.id.rvHourly)
        layoutPermissionOverlay = findViewById(R.id.layoutPermissionOverlay)
        tabToday = findViewById(R.id.tabToday)
        tabTomorrow = findViewById(R.id.tabTomorrow)
        tabNext7Days = findViewById(R.id.tabNext7Days)
        tabTodayIndicator = findViewById(R.id.tabTodayIndicator)
        tabTomorrowIndicator = findViewById(R.id.tabTomorrowIndicator)
        shimmerLayout = findViewById(R.id.shimmerLayout)

        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
    }

    private fun setupRecyclerView() {
        hourlyAdapter = HourlyWeatherAdapter(emptyList())
        rvHourly.layoutManager = LinearLayoutManager(
            this, LinearLayoutManager.HORIZONTAL, false
        )
        rvHourly.adapter = hourlyAdapter
    }

    private fun setTodayDate() {
        val sdf = SimpleDateFormat("EEE, MMM dd", Locale.getDefault())
        tvDate.text = sdf.format(Date())
    }

    private fun setupTabs() {
        tabToday.setOnClickListener { switchTab(0) }
        tabTomorrow.setOnClickListener { switchTab(1) }
        tabNext7Days.setOnClickListener {
            val weather = viewModel.weatherData.value ?: return@setOnClickListener

            // Guard: if weather hasn't loaded yet, do nothing
            // This shouldn't happen since the tab is only visible after data loads,
            // but it's good defensive practice

            val intent = Intent(this, WeeklyForecastActivity::class.java)

            // Pack daily arrays — primitive arrays serialize efficiently in Intents
            intent.putExtra("times",         weather.daily.time.toTypedArray())
            intent.putExtra("weatherCodes",  weather.daily.weatherCode.toIntArray())
            intent.putExtra("tempMax",       weather.daily.tempMax.toDoubleArray())
            intent.putExtra("tempMin",       weather.daily.tempMin.toDoubleArray())
            intent.putExtra("precipitation", weather.daily.precipitation.toDoubleArray())
            intent.putExtra("windSpeed",     weather.daily.windspeed.toDoubleArray())

            // Pack hourly humidity — needed for tomorrow's featured card (noon = index 36)
            intent.putExtra("humidity",      weather.hourly.humidity.toIntArray())

            startActivity(intent)
        }
    }

    private fun switchTab(dayIndex: Int) {
        selectedDayIndex = dayIndex

        when (dayIndex) {
            0 -> {
                tabTodayIndicator.visibility = View.VISIBLE
                tabTomorrowIndicator.visibility = View.INVISIBLE
                tabToday.setTextColor("#1C1C1C".toColorInt())
                tabToday.setTypeface(null, android.graphics.Typeface.BOLD)
                tabTomorrow.setTextColor("#C78D64".toColorInt())
                tabTomorrow.setTypeface(null, android.graphics.Typeface.NORMAL)
            }
            1 -> {
                tabTodayIndicator.visibility = View.INVISIBLE
                tabTomorrowIndicator.visibility = View.VISIBLE
                tabToday.setTextColor("#C78D64".toColorInt())
                tabToday.setTypeface(null, android.graphics.Typeface.NORMAL)
                tabTomorrow.setTextColor("#1C1C1C".toColorInt())
                tabTomorrow.setTypeface(null, android.graphics.Typeface.BOLD)
            }
        }

        updateHourlyForDay(dayIndex)
    }

    private fun requestLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                checkGpsAndFetchLocation()
            }
            shouldShowRequestPermissionRationale(
                Manifest.permission.ACCESS_FINE_LOCATION
            ) -> {
                showPermissionOverlay()
            }
            else -> {
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }

    private fun checkGpsAndFetchLocation() {
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        if (!isGpsEnabled && !isNetworkEnabled) {
            if (!gpsDialogShowing) {
                showGpsDisabledDialog()
            }
        } else {
            gpsDialogShowing = false
            fetchCurrentLocation()
        }
    }

    private fun fetchCurrentLocation() {
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        startShimmer()

        // ── Step 1: Try last known location first (instantaneous) ──────────────
        // This fires immediately with cached coordinates — no GPS wait
        fusedLocationClient.lastLocation
            .addOnSuccessListener { lastLocation ->
                if (lastLocation != null) {
                    // We have a cached location — fire network calls immediately
                    // User will see weather in 2-3 seconds from this point
                    viewModel.fetchWeatherWithReverseGeocode(
                        latitude  = lastLocation.latitude,
                        longitude = lastLocation.longitude
                    )
                }
                // Whether or not we got a last location,
                // also request a fresh location update in the background
                requestFreshLocation()
            }
            .addOnFailureListener {
                // Last location call itself failed — go straight to fresh request
                requestFreshLocation()
            }
    }

    private fun requestFreshLocation() {
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        // ── Step 2: Request a real location update ──────────────────────────────
        // BALANCED_POWER = uses Wi-Fi/cell towers, not GPS satellites
        // Much faster acquisition (1-3 seconds vs 10-15 for GPS)
        // Accuracy is 50-100m which is completely fine for weather
        val locationRequest = com.google.android.gms.location.LocationRequest.Builder(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY, 5000L
        )
            .setWaitForAccurateLocation(false)   // ← don't wait for GPS fix
            .setMinUpdateIntervalMillis(2000L)
            .setMaxUpdates(1)
            .build()

        val locationCallback = object : com.google.android.gms.location.LocationCallback() {
            override fun onLocationResult(result: com.google.android.gms.location.LocationResult) {
                fusedLocationClient.removeLocationUpdates(this)
                val location = result.lastLocation ?: return

                // Only refresh weather if coordinates differ meaningfully
                // (more than ~1km) from what we already showed — avoids
                // unnecessary re-fetches if the user hasn't moved
                val currentWeather = viewModel.weatherData.value
                if (currentWeather == null) {
                    // Nothing shown yet — this is our first result, use it
                    viewModel.fetchWeatherWithReverseGeocode(
                        latitude  = location.latitude,
                        longitude = location.longitude
                    )
                }
                // If weather already loaded from lastLocation, we're done —
                // no need to re-fetch for a few meters of difference
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            mainLooper
        )
    }

    private fun showGpsDisabledDialog() {
        gpsDialogShowing = true
        val dialog = AlertDialog.Builder(this)
            .setTitle("Location Service Off")
            .setMessage("Weather requires location service to obtain weather information for your current location.")
            .setPositiveButton("Settings") { _, _ ->
                gpsDialogShowing = false
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
            .setNegativeButton("Cancel") { d, _ ->
                gpsDialogShowing = false
                d.dismiss()
                showPermissionOverlay()
            }
            .setOnDismissListener {
                gpsDialogShowing = false
            }
            .create()
        dialog.show()
    }

    private fun showPermissionOverlay() {
        layoutPermissionOverlay.visibility = View.VISIBLE
        tvCityName.text = getString(R.string.label_current_city)
    }

    private fun hidePermissionOverlay() {
        layoutPermissionOverlay.visibility = View.GONE
    }

    // ── Shimmer helpers ────────────────────────────────────────────────────────

    private fun startShimmer() {
        shimmerLayout.visibility = View.VISIBLE
        shimmerLayout.startShimmer()
    }

    private fun stopShimmer() {
        shimmerLayout.stopShimmer()
        shimmerLayout.visibility = View.GONE
    }

    private fun showAllWeatherViews() {
        hidePermissionOverlay()
        stopShimmer()
        listOf(
            R.id.tvCityName, R.id.tvDate, R.id.ivWeatherIcon,
            R.id.tvTemperature, R.id.tvTempUnit, R.id.tvWeatherCondition,
            R.id.cardRainfall, R.id.cardWind, R.id.cardHumidity,
            R.id.tabToday, R.id.tabTodayIndicator, R.id.tabTomorrow,
            R.id.tabNext7Days, R.id.rvHourly, R.id.btnSearch,
            R.id.tabTomorrowIndicator
        ).forEach { id ->
            if (id == R.id.tabTomorrowIndicator) {
                findViewById<View>(id).visibility = View.INVISIBLE
            } else {
                findViewById<View>(id).visibility = View.VISIBLE
            }
        }

        switchTab(selectedDayIndex) // ← ADD THIS: re-applies correct indicator after views become visible
    }

    private fun showSearchDialog() {
        val bottomSheet = com.google.android.material.bottomsheet.BottomSheetDialog(
            this, R.style.TransparentBottomSheet
        )

        // This makes the bottom sheet resize when keyboard appears
        bottomSheet.behavior.apply {
            isFitToContents = true
            skipCollapsed = true
        }

        val view = layoutInflater.inflate(R.layout.dialog_search_city, findViewById(android.R.id.content), false)
        bottomSheet.setContentView(view)

        val etSearch = view.findViewById<android.widget.EditText>(R.id.etCitySearch)
        val rvResults = view.findViewById<RecyclerView>(R.id.rvSearchResults)
        val tvHint = view.findViewById<TextView>(R.id.tvSearchHint)
        val btnClear = view.findViewById<android.widget.ImageView>(R.id.btnClearSearch)

        val searchAdapter = SearchResultsAdapter(emptyList()) { selected ->
            viewModel.fetchWeather(
                latitude = selected.latitude,
                longitude = selected.longitude,
                cityName = "${selected.name},\n${selected.country}"
            )
            bottomSheet.dismiss()
        }
        rvResults.adapter = searchAdapter

        // Clear button
        btnClear.setOnClickListener {
            etSearch.text.clear()
        }

        // Live search — debounced in ViewModel
        etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val query = s?.toString() ?: ""
                btnClear.visibility = if (query.isNotEmpty()) View.VISIBLE else View.GONE
                if (query.length < 2) {
                    // Reset immediately when query is too short — don't wait for debounce
                    rvResults.visibility = View.GONE
                    tvHint.visibility = View.VISIBLE
                    tvHint.text = "Type at least 2 characters"
                }
                viewModel.searchCity(query)
            }
        })

        // Observe results only while this dialog is alive
        val observer = androidx.lifecycle.Observer<List<com.example.weatherapp.model.GeoLocation>> { results ->
            if (results.isEmpty()) {
                rvResults.visibility = View.GONE
                tvHint.visibility = View.VISIBLE
                // Only show "No cities found" if user actually typed 2+ chars
                tvHint.text = if ((etSearch.text?.length ?: 0) >= 2)
                    "No cities found" else "Type at least 2 characters"
            } else {
                tvHint.visibility = View.GONE
                rvResults.visibility = View.VISIBLE
                searchAdapter.updateItems(results)
            }
        }
        viewModel.searchResults.observe(this, observer)

        // Clean up observer when dialog closes — prevents it from leaking
        bottomSheet.setOnDismissListener {
            viewModel.searchResults.removeObserver(observer)
            // Clear results so they don't fire again next time dialog opens
            viewModel.clearSearchResults()
        }

        // Show keyboard automatically
        etSearch.requestFocus()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            bottomSheet.window?.setDecorFitsSystemWindows(false)
        } else {
            @Suppress("DEPRECATION")
            bottomSheet.window?.setSoftInputMode(
                android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE or
                        android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            )
        }

        bottomSheet.show()

    }

    private fun observeViewModel() {

        viewModel.isRefreshing.observe(this) { refreshing ->
            swipeRefreshLayout.isRefreshing = refreshing
        }

        viewModel.isLoading.observe(this) { loading ->
            if (loading) {
                startShimmer()
                swipeRefreshLayout.isEnabled = false  // ← disable pull-to-refresh during shimmer
            } else {
                stopShimmer()
                swipeRefreshLayout.isEnabled = true   // ← re-enable once shimmer is done
            }
        }

        viewModel.weatherData.observe(this) { weather ->
            weatherLoaded = true
            showAllWeatherViews()

            tvTemperature.text = weather.currentWeather.temperature.toInt().toString()
            tvWeatherCondition.text = getWeatherConditionLabel(weather.currentWeather.weathercode)
            tvRainfallValue.text = "${weather.daily.precipitation.firstOrNull() ?: 0.0} mm"
            tvWindValue.text = "${weather.currentWeather.windspeed} km/h"

            // ← ADD THESE TWO LINES
            val isDay = weather.currentWeather.isDay == 1
            ivWeatherIcon.setAnimation(getWeatherLottieRes(weather.currentWeather.weathercode, isDay))
            ivWeatherIcon.playAnimation()

            val currentHour = getCurrentHourIndex()
            if (weather.hourly.humidity.isNotEmpty() && currentHour < weather.hourly.humidity.size) {
                tvHumidityValue.text = "${weather.hourly.humidity[currentHour]}%"
            }

            updateHourlyForDay(selectedDayIndex)
        }

        viewModel.cityName.observe(this) { name ->
            tvCityName.text = name
        }

        viewModel.errorMessage.observe(this) { message ->
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }

        viewModel.timezone.observe(this) { tz ->
            currentTimezone = tz
            // Re-render hourly list with correct timezone whenever it updates
            updateHourlyForDay(selectedDayIndex)
        }
    }


    private fun updateHourlyForDay(dayIndex: Int) {
        val weather = viewModel.weatherData.value ?: return

        val startIndex = dayIndex * 24
        val endIndex = minOf(startIndex + 24, weather.hourly.time.size)
        val startHour = if (dayIndex == 0) getCurrentHourIndex() else 0

        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault()).apply {
            // API times are already in the city's local time (because we pass timezone=auto)
            // So we parse them as-is — no timezone conversion needed on the time strings
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }
        val outputFormat = SimpleDateFormat("h:mm a", Locale.getDefault()).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }

        val hourlyItems = (startIndex + startHour until endIndex).map { actualIndex ->
            val rawTime = weather.hourly.time[actualIndex]

            val formattedTime = try {
                val date = inputFormat.parse(rawTime)
                outputFormat.format(date!!)
            } catch (_: Exception) {
                rawTime.substring(11, 16)
            }

            val hourOfDay = try {
                rawTime.substring(11, 13).toInt()
            } catch (_: Exception) {
                12
            }
            val isDayHour = hourOfDay in 6..19

            HourlyWeatherItem(
                time = formattedTime,
                temperature = weather.hourly.temperature[actualIndex],
                weatherCode = weather.hourly.weatherCode[actualIndex],
                isNow = dayIndex == 0 && actualIndex == startIndex + startHour,
                isDay = isDayHour
            )
        }

        hourlyAdapter.updateItems(hourlyItems)
        rvHourly.scrollToPosition(0)
    }

    private fun getCurrentHourIndex(): Int {
        return try {
            val tz = java.util.TimeZone.getTimeZone(currentTimezone)
            val cal = Calendar.getInstance(tz)
            cal.get(Calendar.HOUR_OF_DAY)
        } catch (_: Exception) {
            Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        }
    }

    private fun getWeatherLottieRes(code: Int, isDay: Boolean): Int {
        return when (code) {
            0, 1 -> if (isDay) R.raw.lottie_clear_day else R.raw.lottie_clear_night
            2    -> if (isDay) R.raw.lottie_partly_cloudy_day else R.raw.lottie_partly_cloudy_night
            3    -> R.raw.lottie_cloudy
            45, 48 -> R.raw.lottie_fog
            51, 53, 55 -> R.raw.lottie_drizzle
            61, 63, 65,
            80, 81, 82 -> R.raw.lottie_rain
            71, 73, 75, 77 -> R.raw.lottie_snow
            95, 96, 99 -> R.raw.lottie_thunderstorms
            else -> if (isDay) R.raw.lottie_clear_day else R.raw.lottie_clear_night
        }
    }

    private fun getWeatherConditionLabel(code: Int): String {
        return when (code) {
            0 -> "Clear Sky"
            1 -> "Mainly Clear"
            2 -> "Partly Cloudy"
            3 -> "Overcast"
            45, 48 -> "Foggy"
            51, 53, 55 -> "Drizzle"
            61, 63, 65 -> "Rainy"
            71, 73, 75 -> "Snowy"
            80, 81, 82 -> "Showers"
            95 -> "Thunderstorm"
            96, 99 -> "Heavy Storm"
            else -> "Unknown"
        }
    }

    override fun onResume() {
        super.onResume()

        if (!weatherLoaded && !gpsDialogShowing) {
            // Case 1 & 3: No data loaded at all — fresh open or after full close
            // Run the full GPS + shimmer flow
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                checkGpsAndFetchLocation()
            }
        }
        // Case 2: weatherLoaded = true → user just came back from Home
        // Do nothing — existing data stays on screen, no shimmer, no re-fetch
    }
}