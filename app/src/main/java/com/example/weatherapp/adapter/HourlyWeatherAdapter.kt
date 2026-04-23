package com.example.weatherapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.weatherapp.R
import com.google.android.material.card.MaterialCardView
import androidx.core.graphics.toColorInt

data class HourlyWeatherItem(
    val time: String,
    val temperature: Double,
    val weatherCode: Int,
    val isNow: Boolean = false,
    val isDay: Boolean = true   // ✅ Added: drives day vs night icon selection
)

class HourlyWeatherAdapter(
    private var items: List<HourlyWeatherItem>
) : RecyclerView.Adapter<HourlyWeatherAdapter.HourlyViewHolder>() {

    inner class HourlyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val card: MaterialCardView = itemView.findViewById(R.id.cardHourlyItem)
        val tvTime: TextView = itemView.findViewById(R.id.tvHourlyTime)
        val ivIcon: com.airbnb.lottie.LottieAnimationView = itemView.findViewById(R.id.ivHourlyIcon)
        val tvTemp: TextView = itemView.findViewById(R.id.tvHourlyTemp)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HourlyViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_hourly_weather, parent, false)
        return HourlyViewHolder(view)
    }

    override fun onBindViewHolder(holder: HourlyViewHolder, position: Int) {
        val item = items[position]

        // Set time label
        if (item.isNow) {
            holder.tvTime.text = holder.itemView.context.getString(R.string.label_now)
            holder.card.setCardBackgroundColor(
                holder.itemView.context.getColor(R.color.selected_bg_for_hourly_item_selected_card)
            )
            holder.tvTime.setTextColor(
                holder.itemView.context.getColor(R.color.color1_black)
            )
            holder.tvTemp.setTextColor(
                holder.itemView.context.getColor(R.color.color1_black)
            )
        } else {
            holder.tvTime.text = item.time
            holder.card.setCardBackgroundColor(
                "#fed0a8".toColorInt()
            )
            holder.tvTime.setTextColor(
                "#a7a4aa".toColorInt()
            )
            holder.tvTemp.setTextColor(
                "#353747".toColorInt()
            )
        }

        // Set temperature
        holder.tvTemp.text = "${item.temperature.toInt()}°"

        holder.ivIcon.setAnimation(getWeatherLottieRes(item.weatherCode, item.isDay))
        holder.ivIcon.playAnimation()
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<HourlyWeatherItem>) {
        items = newItems
        notifyDataSetChanged()
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
}