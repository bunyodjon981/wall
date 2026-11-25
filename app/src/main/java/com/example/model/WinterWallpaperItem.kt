package com.example.model

import android.content.Context
import androidx.annotation.DrawableRes
import com.example.R

data class WinterWallpaperItem(
    val id: String,
    val title: String,
    val subtitle: String,
    @DrawableRes val drawableRes: Int?,
    val skyTopColor: Int,
    val skyBottomColor: Int,
    val hillBackColor: Int,
    val hillFrontColor: Int,
    val accentGlowColor: Int,
    val isFeatured: Boolean = false
)

enum class SnowIntensity(val label: String, val flakeCount: Int, val speedMultiplier: Float, val accumulationRate: Float) {
    NORMAL("Odatiy qor", 145, 1.0f, 1.0f),
    HEAVY("Kuchli bo'ron", 240, 1.45f, 1.85f)
}

object WinterWallpaperRepository {
    private const val PREFS_NAME = "winter_wallpaper_prefs"
    private const val KEY_WALLPAPER_ID = "selected_wallpaper_id"
    private const val KEY_HEAVY_SNOW = "heavy_snow_mode"

    val wallpapers: List<WinterWallpaperItem> = listOf(
        WinterWallpaperItem(
            id = "winter_village",
            title = "Qorli Qishloq",
            subtitle = "3D bolalar • Qor mashinasi",
            drawableRes = R.drawable.img_winter_village,
            skyTopColor = 0xFF0A1730.toInt(),
            skyBottomColor = 0xFF1E3F6E.toInt(),
            hillBackColor = 0xFF1A3154.toInt(),
            hillFrontColor = 0xFF284B7A.toInt(),
            accentGlowColor = 0xFFFFB74D.toInt(),
            isFeatured = true
        ),
        WinterWallpaperItem(
            id = "aurora_forest",
            title = "Shimoliy Yog'du",
            subtitle = "Yashil osmon • Muzli o'rmon",
            drawableRes = R.drawable.img_aurora_forest,
            skyTopColor = 0xFF051824.toInt(),
            skyBottomColor = 0xFF0F4C5C.toInt(),
            hillBackColor = 0xFF0B2E3B.toInt(),
            hillFrontColor = 0xFF144552.toInt(),
            accentGlowColor = 0xFF5EEAD4.toInt()
        ),
        WinterWallpaperItem(
            id = "frost_castle",
            title = "Muzli Qal'a",
            subtitle = "Chiroqli ko'prik • Oqshom",
            drawableRes = R.drawable.img_frost_castle,
            skyTopColor = 0xFF191B3A.toInt(),
            skyBottomColor = 0xFF3E4C7A.toInt(),
            hillBackColor = 0xFF252D52.toInt(),
            hillFrontColor = 0xFF34416E.toInt(),
            accentGlowColor = 0xFFFDE68A.toInt()
        ),
        WinterWallpaperItem(
            id = "sunny_alps",
            title = "Quyoshli Alplar",
            subtitle = "Tonggi nur • Oppoq tog'lar",
            drawableRes = R.drawable.img_sunny_alps,
            skyTopColor = 0xFF357DED.toInt(),
            skyBottomColor = 0xFF9CD8FF.toInt(),
            hillBackColor = 0xFF6B9AC4.toInt(),
            hillFrontColor = 0xFF8BBCE3.toInt(),
            accentGlowColor = 0xFFFFD166.toInt()
        ),
        WinterWallpaperItem(
            id = "starry_pine",
            title = "Yulduzli Archa",
            subtitle = "Tungi qor • Tinch osmon",
            drawableRes = null,
            skyTopColor = 0xFF060E21.toInt(),
            skyBottomColor = 0xFF183661.toInt(),
            hillBackColor = 0xFF112645.toInt(),
            hillFrontColor = 0xFF1A3A66.toInt(),
            accentGlowColor = 0xFF7DD3FC.toInt()
        ),
        WinterWallpaperItem(
            id = "cozy_dusk",
            title = "Oqshom Qori",
            subtitle = "Shafaq rangi • Issiq chiroq",
            drawableRes = null,
            skyTopColor = 0xFF1E1B3A.toInt(),
            skyBottomColor = 0xFF6A4C93.toInt(),
            hillBackColor = 0xFF2C274C.toInt(),
            hillFrontColor = 0xFF3F386B.toInt(),
            accentGlowColor = 0xFFFF9F1C.toInt()
        )
    )

    fun getById(id: String?): WinterWallpaperItem {
        return wallpapers.firstOrNull { it.id == id } ?: wallpapers.first()
    }

    fun saveSelectedWallpaperId(context: Context, id: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_WALLPAPER_ID, id)
            .apply()
    }

    fun getSelectedWallpaperId(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_WALLPAPER_ID, wallpapers.first().id)
            ?: wallpapers.first().id
    }

    fun saveSnowIntensity(context: Context, intensity: SnowIntensity) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_HEAVY_SNOW, intensity == SnowIntensity.HEAVY)
            .apply()
    }

    fun getSnowIntensity(context: Context): SnowIntensity {
        val heavy = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_HEAVY_SNOW, false)
        return if (heavy) SnowIntensity.HEAVY else SnowIntensity.NORMAL
    }
}
