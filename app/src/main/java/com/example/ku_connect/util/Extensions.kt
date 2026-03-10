package com.example.ku_connect.util

import android.content.Context
import android.graphics.Color
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

object Extensions {
    fun String.toInitial(): String = this.trim().firstOrNull()?.uppercase() ?: "?"

    fun String.toAvatarColors(targetColor: String): Pair<Int, Int> {
        val colors = listOf(
            Pair("#C8E6C9", "#2E7D32"),
            Pair("#BBDEFB", "#1565C0"),
            Pair("#F8BBD9", "#880E4F"),
            Pair("#FFF9C4", "#F57F17"),
            Pair("#E1BEE7", "#6A1B9A"),
            Pair("#FFE0B2", "#E65100"),
        )

        val match = colors.find { it.first == targetColor }

        return if (match != null) {
            Pair(Color.parseColor(match.first), Color.parseColor(match.second))
        } else {
            val index = abs(this.hashCode()) % colors.size
            Pair(
                Color.parseColor(colors[index].first),
                Color.parseColor(colors[index].second)
            )
        }
    }

    fun Date.toRelativeTime(): String {
        val now = System.currentTimeMillis()
        val diff = now - this.time
        return when {
            diff < 60_000 -> "เมื่อกี้"
            diff < 3_600_000 -> "${diff / 60_000} นาทีที่แล้ว"
            diff < 86_400_000 -> "${diff / 3_600_000} ชั่วโมงที่แล้ว"
            diff < 604_800_000 -> "${diff / 86_400_000} วันที่แล้ว"
            else -> SimpleDateFormat("d MMM yyyy", Locale("th")).format(this)
        }
    }

    fun Context.showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}