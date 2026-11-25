package com.example.wallpaper

import android.app.WallpaperManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.model.SnowIntensity
import com.example.model.WinterWallpaperItem
import com.example.model.WinterWallpaperRepository
import com.example.simulation.WinterSceneRenderer
import com.example.simulation.WinterSceneState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed interface WallpaperActionResult {
    data class Success(val message: String) : WallpaperActionResult
    data class Error(val message: String) : WallpaperActionResult
}

object WallpaperInstaller {

    fun openLiveWallpaperSetter(
        context: Context,
        wallpaper: WinterWallpaperItem,
        intensity: SnowIntensity
    ): WallpaperActionResult {
        WinterWallpaperRepository.saveSelectedWallpaperId(context, wallpaper.id)
        WinterWallpaperRepository.saveSnowIntensity(context, intensity)

        val directIntent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
            putExtra(
                WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                ComponentName(context, WinterWallpaperService::class.java)
            )
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            context.startActivity(directIntent)
            WallpaperActionResult.Success("${wallpaper.title} jonli fon uchun ochildi")
        } catch (_: ActivityNotFoundException) {
            try {
                val chooserIntent = Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooserIntent)
                WallpaperActionResult.Success("Jonli fonlar ro'yxatidan 'Qorli Fon' ni tanlang")
            } catch (_: Exception) {
                WallpaperActionResult.Error("Bu qurilmada jonli fon tanlagich topilmadi. Rasm sifatida o'rnating.")
            }
        }
    }

    suspend fun setStaticWallpaper(
        context: Context,
        wallpaper: WinterWallpaperItem,
        state: WinterSceneState
    ): WallpaperActionResult = withContext(Dispatchers.IO) {
        try {
            val bitmap = renderSnapshotBitmap(context, wallpaper, state)
            WallpaperManager.getInstance(context).setBitmap(bitmap)
            bitmap.recycle()
            WallpaperActionResult.Success("${wallpaper.title} ekran foni qilib o'rnatildi!")
        } catch (e: Exception) {
            WallpaperActionResult.Error("Fon o'rnatishda xatolik: ${e.localizedMessage ?: "Noma'lum"}")
        }
    }

    suspend fun saveToGallery(
        context: Context,
        wallpaper: WinterWallpaperItem,
        state: WinterSceneState
    ): WallpaperActionResult = withContext(Dispatchers.IO) {
        try {
            val bitmap = renderSnapshotBitmap(context, wallpaper, state)
            val fileName = "QorliFon_${wallpaper.id}_${System.currentTimeMillis()}.png"
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/QorliFon")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: return@withContext WallpaperActionResult.Error("Galereyaga saqlab bo'lmadi")

            resolver.openOutputStream(uri)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            bitmap.recycle()
            WallpaperActionResult.Success("Rasm Galereyaga (Pictures/QorliFon) yuklandi!")
        } catch (e: Exception) {
            WallpaperActionResult.Error("Saqlashda xatolik: ${e.localizedMessage ?: "Noma'lum"}")
        }
    }

    private fun renderSnapshotBitmap(
        context: Context,
        wallpaper: WinterWallpaperItem,
        sourceState: WinterSceneState
    ): Bitmap {
        val w = 1080
        val h = 1620 // Exact 4x6 portrait ratio
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val renderer = WinterSceneRenderer(context)
        renderer.draw(
            canvas = canvas,
            width = w.toFloat(),
            height = h.toFloat(),
            density = 2.8f,
            wallpaper = wallpaper,
            state = sourceState
        )
        return bitmap
    }
}
