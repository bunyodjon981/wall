package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.FullscreenWallpaperScreen
import com.example.ui.GalleryScreen
import com.example.ui.MainViewModel
import com.example.ui.theme.DeepWinterNavy
import com.example.ui.theme.IcyCyan
import com.example.ui.theme.QorliFonTheme
import com.example.ui.theme.SnowWhite

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            QorliFonTheme {
                val viewModel: MainViewModel = viewModel()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val snackbarHostState = remember { SnackbarHostState() }

                LaunchedEffect(uiState.statusMessage) {
                    val msg = uiState.statusMessage
                    if (msg != null) {
                        snackbarHostState.showSnackbar(msg)
                        viewModel.consumeStatusMessage()
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    AnimatedContent(
                        targetState = uiState.selectedWallpaper,
                        transitionSpec = {
                            (fadeIn() + scaleIn(initialScale = 0.96f)) togetherWith fadeOut()
                        },
                        label = "screen_transition"
                    ) { selected ->
                        if (selected == null) {
                            GalleryScreen(
                                wallpapers = uiState.wallpapers,
                                snowIntensity = uiState.snowIntensity,
                                onToggleIntensity = viewModel::toggleSnowIntensity,
                                onSelectWallpaper = viewModel::openWallpaper
                            )
                        } else {
                            FullscreenWallpaperScreen(
                                wallpaper = selected,
                                sceneState = viewModel.sceneState,
                                snowIntensity = uiState.snowIntensity,
                                hideHud = uiState.hideHud,
                                showInstallSheet = uiState.showInstallSheet,
                                isInstalling = uiState.isInstalling,
                                onBack = viewModel::closeFullscreen,
                                onToggleHud = viewModel::toggleHud,
                                onSummonPlow = viewModel::summonSnowplow,
                                onToggleSnowIntensity = viewModel::toggleSnowIntensity,
                                onClearSnow = viewModel::clearSnowNow,
                                onOpenInstallSheet = viewModel::openInstallSheet,
                                onDismissInstallSheet = viewModel::dismissInstallSheet,
                                onInstallLiveWallpaper = viewModel::installLiveWallpaper,
                                onInstallStaticWallpaper = viewModel::installStaticWallpaper,
                                onDownloadGallery = viewModel::downloadToGallery
                            )
                        }
                    }

                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .navigationBarsPadding()
                            .padding(top = 64.dp, start = 16.dp, end = 16.dp)
                    ) { data ->
                        Snackbar(
                            snackbarData = data,
                            containerColor = DeepWinterNavy,
                            contentColor = SnowWhite,
                            actionColor = IcyCyan
                        )
                    }
                }
            }
        }
    }
}
