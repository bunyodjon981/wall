package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.SnowIntensity
import com.example.model.WinterWallpaperItem
import com.example.model.WinterWallpaperRepository
import com.example.simulation.WinterSceneState
import com.example.wallpaper.WallpaperActionResult
import com.example.wallpaper.WallpaperInstaller
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MainUiState(
    val wallpapers: List<WinterWallpaperItem> = WinterWallpaperRepository.wallpapers,
    val selectedWallpaper: WinterWallpaperItem? = null,
    val snowIntensity: SnowIntensity = SnowIntensity.NORMAL,
    val hideHud: Boolean = false,
    val showInstallSheet: Boolean = false,
    val isInstalling: Boolean = false,
    val statusMessage: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(
        MainUiState(
            snowIntensity = WinterWallpaperRepository.getSnowIntensity(application)
        )
    )
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    val sceneState = WinterSceneState().apply {
        intensity = _uiState.value.snowIntensity
    }

    fun openWallpaper(item: WinterWallpaperItem) {
        WinterWallpaperRepository.saveSelectedWallpaperId(getApplication(), item.id)
        _uiState.update {
            it.copy(
                selectedWallpaper = item,
                hideHud = false,
                showInstallSheet = false
            )
        }
    }

    fun closeFullscreen() {
        _uiState.update {
            it.copy(
                selectedWallpaper = null,
                showInstallSheet = false,
                hideHud = false
            )
        }
    }

    fun toggleHud() {
        _uiState.update { it.copy(hideHud = !it.hideHud) }
    }

    fun summonSnowplow() {
        sceneState.triggerSnowplow()
        _uiState.update { it.copy(statusMessage = "Qor tozalash mashinasi yo'lga chiqdi!") }
    }

    fun toggleSnowIntensity() {
        val next = if (_uiState.value.snowIntensity == SnowIntensity.NORMAL) {
            SnowIntensity.HEAVY
        } else {
            SnowIntensity.NORMAL
        }
        sceneState.intensity = next
        WinterWallpaperRepository.saveSnowIntensity(getApplication(), next)
        _uiState.update {
            it.copy(
                snowIntensity = next,
                statusMessage = if (next == SnowIntensity.HEAVY) {
                    "Kuchli qor bo'roni yoqildi"
                } else {
                    "Odatiy qor yog'ishiga o'tildi"
                }
            )
        }
    }

    fun clearSnowNow() {
        sceneState.clearAllSnow()
        _uiState.update { it.copy(statusMessage = "Pastdagi qor tozalandi!") }
    }

    fun openInstallSheet() {
        _uiState.update { it.copy(showInstallSheet = true) }
    }

    fun dismissInstallSheet() {
        _uiState.update { it.copy(showInstallSheet = false) }
    }

    fun installLiveWallpaper() {
        val current = _uiState.value.selectedWallpaper ?: return
        val result = WallpaperInstaller.openLiveWallpaperSetter(
            context = getApplication(),
            wallpaper = current,
            intensity = _uiState.value.snowIntensity
        )
        val msg = when (result) {
            is WallpaperActionResult.Success -> result.message
            is WallpaperActionResult.Error -> result.message
        }
        _uiState.update { it.copy(showInstallSheet = false, statusMessage = msg) }
    }

    fun installStaticWallpaper() {
        val current = _uiState.value.selectedWallpaper ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isInstalling = true) }
            val result = WallpaperInstaller.setStaticWallpaper(
                context = getApplication(),
                wallpaper = current,
                state = sceneState
            )
            val msg = when (result) {
                is WallpaperActionResult.Success -> result.message
                is WallpaperActionResult.Error -> result.message
            }
            _uiState.update {
                it.copy(isInstalling = false, showInstallSheet = false, statusMessage = msg)
            }
        }
    }

    fun downloadToGallery() {
        val current = _uiState.value.selectedWallpaper ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isInstalling = true) }
            val result = WallpaperInstaller.saveToGallery(
                context = getApplication(),
                wallpaper = current,
                state = sceneState
            )
            val msg = when (result) {
                is WallpaperActionResult.Success -> result.message
                is WallpaperActionResult.Error -> result.message
            }
            _uiState.update {
                it.copy(isInstalling = false, showInstallSheet = false, statusMessage = msg)
            }
        }
    }

    fun consumeStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }
}
