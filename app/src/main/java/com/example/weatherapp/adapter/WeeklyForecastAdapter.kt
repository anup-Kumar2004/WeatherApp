package com.example.weatherapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.weatherapp.R
import com.example.weatherapp.model.WeeklyForecastItem

class WeeklyForecastAdapter(
    private val items: List<WeeklyForecastItem>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_FEATURED = 0
        private const val TYPE_SIMPLE = 1
    }

    // ── ViewHolders ───────────────────────────────────────────────────────────

    inner class FeaturedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvLabel: TextView = itemView.findViewById(R.id.tvFeaturedLabel)
        val tvTemp: TextView = itemView.findViewById(R.id.tvFeaturedTemp)
        val ivIcon: com.airbnb.lottie.LottieAnimationView = itemView.findViewById(R.id.ivFeaturedIcon)
        val tvPrecipitation: TextView = itemView.findViewById(R.id.tvFeaturedPrecipitation)
        val tvWind: TextView = itemView.findViewById(R.id.tvFeaturedWind)
        val tvHumidity: TextView = itemView.findViewById(R.id.tvFeaturedHumidity)
    }

    inner class SimpleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDayName: TextView = itemView.findViewById(R.id.tvSimpleDayName)
        val tvTemp: TextView = itemView.findViewById(R.id.tvSimpleTemp)
        val ivIcon: com.airbnb.lottie.LottieAnimationView = itemView.findViewById(R.id.ivSimpleIcon)
    }

    // ── Type resolution ───────────────────────────────────────────────────────

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is WeeklyForecastItem.FeaturedDay -> TYPE_FEATURED
            is WeeklyForecastItem.SimpleDay -> TYPE_SIMPLE
        }
    }

    // ── Inflation ─────────────────────────────────────────────────────────────

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_FEATURED -> FeaturedViewHolder(
                inflater.inflate(R.layout.item_weekly_featured, parent, false)
            )
            else -> SimpleViewHolder(
                inflater.inflate(R.layout.item_weekly_simple, parent, false)
            )
        }
    }

    // ── Binding ───────────────────────────────────────────────────────────────

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {

            is WeeklyForecastItem.FeaturedDay -> {
                val h = holder as FeaturedViewHolder
                h.tvLabel.text = item.label
                h.tvTemp.text = "${item.tempMin.toInt()}° / ${item.tempMax.toInt()}°"
                // ADD for FeaturedDay:
                h.ivIcon.setAnimation(getWeatherLottieRes(item.weatherCode))
                h.ivIcon.playAnimation()
                h.tvPrecipitation.text = "${item.precipitation} mm"
                h.tvWind.text = "${item.windSpeed} km/h"
                h.tvHumidity.text = "${item.humidity}%"
            }

            is WeeklyForecastItem.SimpleDay -> {
                val h = holder as SimpleViewHolder
                h.tvDayName.text = item.dayName
                h.tvTemp.text = "${item.tempMin.toInt()}° / ${item.tempMax.toInt()}°"
                // ADD for SimpleDay:
                h.ivIcon.setAnimation(getWeatherLottieRes(item.weatherCode))
                h.ivIcon.playAnimation()
            }
        }
    }

    override fun getItemCount(): Int = items.size

    // ── Icon resolver (always daytime for daily forecast) ─────────────────────

    private fun getWeatherLottieRes(code: Int): Int {
        return when (code) {
            0, 1 -> R.raw.lottie_clear_day
            2    -> R.raw.lottie_partly_cloudy_day
            3    -> R.raw.lottie_cloudy
            45, 48 -> R.raw.lottie_fog
            51, 53, 55 -> R.raw.lottie_drizzle
            61, 63, 65,
            80, 81, 82 -> R.raw.lottie_rain
            71, 73, 75, 77 -> R.raw.lottie_snow
            95, 96, 99 -> R.raw.lottie_thunderstorms
            else -> R.raw.lottie_clear_day
        }
    }
}