package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.SnowIntensity
import com.example.model.WinterWallpaperItem
import com.example.simulation.WinterSceneRenderer
import com.example.simulation.WinterSceneState
import com.example.ui.theme.ArcticMidnight
import com.example.ui.theme.DeepWinterNavy
import com.example.ui.theme.IcyCyan
import com.example.ui.theme.SnowWhite
import com.example.ui.theme.WarmCottageAmber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullscreenWallpaperScreen(
    wallpaper: WinterWallpaperItem,
    sceneState: WinterSceneState,
    snowIntensity: SnowIntensity,
    hideHud: Boolean,
    showInstallSheet: Boolean,
    isInstalling: Boolean,
    onBack: () -> Unit,
    onToggleHud: () -> Unit,
    onSummonPlow: () -> Unit,
    onToggleSnowIntensity: () -> Unit,
    onClearSnow: () -> Unit,
    onOpenInstallSheet: () -> Unit,
    onDismissInstallSheet: () -> Unit,
    onInstallLiveWallpaper: () -> Unit,
    onInstallStaticWallpaper: () -> Unit,
    onDownloadGallery: () -> Unit
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val density = LocalDensity.current.density
    val renderer = remember(context) { WinterSceneRenderer(context) }
    var frameTick by remember { mutableLongStateOf(0L) }

    // 60fps simulation loop
    LaunchedEffect(wallpaper.id) {
        var lastNanos = 0L
        while (true) {
            withFrameNanos { frameNanos ->
                val dt = if (lastNanos == 0L) 0.016f else (frameNanos - lastNanos) / 1_000_000_000f
                lastNanos = frameNanos
                sceneState.update(dt)
                frameTick = frameNanos
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Fullscreen interactive winter canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: continue
                            if (change.pressed) {
                                val xNorm = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                                val yNorm = (change.position.y / size.height.toFloat()).coerceIn(0f, 1f)
                                sceneState.brushSnowAt(xNorm, yNorm)
                            }
                        }
                    }
                }
                .testTag("winter_live_canvas")
        ) {
            // Read frameTick so Canvas invalidates at display refresh rate
            @Suppress("UNUSED_EXPRESSION")
            frameTick
            drawIntoCanvas { canvas ->
                renderer.draw(
                    canvas = canvas.nativeCanvas,
                    width = size.width,
                    height = size.height,
                    density = density,
                    wallpaper = wallpaper,
                    state = sceneState
                )
            }
        }

        // Top Floating Frosted Bar
        AnimatedVisibility(
            visible = !hideHud,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xAA071121),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("back_to_gallery_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Orqaga qaytish",
                            tint = SnowWhite
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color(0xAA071121),
                    border = BorderStroke(1.dp, IcyCyan.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(IcyCyan)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = wallpaper.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = SnowWhite,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = Color(0xAA071121),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                ) {
                    IconButton(
                        onClick = onToggleHud,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("toggle_hud_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.VisibilityOff,
                            contentDescription = "Tugmalarni yashirish",
                            tint = SnowWhite
                        )
                    }
                }
            }
        }

        // Restore HUD Button when hidden
        AnimatedVisibility(
            visible = hideHud,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0x88071121),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
            ) {
                IconButton(
                    onClick = onToggleHud,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("restore_hud_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Visibility,
                        contentDescription = "Tugmalarni ko'rsatish",
                        tint = SnowWhite
                    )
                }
            }
        }

        // Bottom Interactive Controls Dock
        AnimatedVisibility(
            visible = !hideHud,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 580.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color(0x99071121),
                    border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.16f))
                ) {
                    Text(
                        text = "Qorni qo'lingiz bilan tozalang yoki moshinani chaqiring",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFD9ECFF),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xCC09172E),
                    border = BorderStroke(1.dp, IcyCyan.copy(alpha = 0.28f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            QuickControlChip(
                                icon = Icons.Filled.LocalShipping,
                                label = "Moshina",
                                accent = WarmCottageAmber,
                                onClick = onSummonPlow,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("summon_plow_button")
                            )
                            QuickControlChip(
                                icon = Icons.Filled.AcUnit,
                                label = if (snowIntensity == SnowIntensity.HEAVY) "Bo'ron" else "Qor +",
                                accent = IcyCyan,
                                onClick = onToggleSnowIntensity,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("toggle_snow_button")
                            )
                            QuickControlChip(
                                icon = Icons.Filled.CleaningServices,
                                label = "Tozalash",
                                accent = Color(0xFF86EFAC),
                                onClick = onClearSnow,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("clear_snow_button")
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = onOpenInstallSheet,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("install_wallpaper_button"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = IcyCyan,
                                contentColor = ArcticMidnight
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Download,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Telefonga yuklash va o'rnatish",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showInstallSheet) {
        ModalBottomSheet(
            onDismissRequest = onDismissInstallSheet,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = DeepWinterNavy,
            contentColor = SnowWhite
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "${wallpaper.title} — Telefonga o'rnatish",
                    style = MaterialTheme.typography.titleLarge,
                    color = SnowWhite
                )
                Text(
                    text = "Qishki animatsiyani jonli fon sifatida o'rnating yoki rasmni galereyaga yuklang.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFB5CDE6)
                )

                if (isInstalling) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(color = IcyCyan, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Tayyorlanmoqda...", color = SnowWhite)
                    }
                } else {
                    InstallOptionRow(
                        icon = Icons.Filled.Wallpaper,
                        title = "Jonli fon sifatida o'rnatish (3D Animatsiya)",
                        subtitle = "Ekranda doimiy qor yog'adi, moshina va bolalar o'ynaydi",
                        accent = IcyCyan,
                        highlighted = true,
                        onClick = onInstallLiveWallpaper,
                        modifier = Modifier.testTag("apply_live_wallpaper_button")
                    )

                    InstallOptionRow(
                        icon = Icons.Filled.Image,
                        title = "Oddiy ekran foni qilib o'rnatish",
                        subtitle = "Hozirgi qishki ko'rinishni bosh ekranga o'rnatadi",
                        accent = WarmCottageAmber,
                        highlighted = false,
                        onClick = onInstallStaticWallpaper,
                        modifier = Modifier.testTag("apply_static_wallpaper_button")
                    )

                    InstallOptionRow(
                        icon = Icons.Filled.Download,
                        title = "Galereyaga yuklab olish (4×6 PNG)",
                        subtitle = "Pictures/QorliFon papkasiga saqlaydi",
                        accent = Color(0xFF86EFAC),
                        highlighted = false,
                        onClick = onDownloadGallery,
                        modifier = Modifier.testTag("download_gallery_button")
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickControlChip(
    icon: ImageVector,
    label: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF152C4E),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = accent,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = SnowWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun InstallOptionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accent: Color,
    highlighted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = if (highlighted) Color(0xFF16355E) else Color(0xFF122745),
        border = BorderStroke(
            width = if (highlighted) 1.5.dp else 1.dp,
            color = if (highlighted) accent else Color.White.copy(alpha = 0.14f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(alpha = 0.18f))
                    .border(1.dp, accent.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = SnowWhite
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFB5CDE6)
                )
            }
        }
    }
}
