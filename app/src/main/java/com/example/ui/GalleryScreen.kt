package com.example.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.SnowIntensity
import com.example.model.WinterWallpaperItem
import com.example.ui.theme.ArcticMidnight
import com.example.ui.theme.DeepWinterNavy
import com.example.ui.theme.FrameGold
import com.example.ui.theme.FrameSilver
import com.example.ui.theme.IcyCyan
import com.example.ui.theme.SnowWhite
import com.example.ui.theme.WarmCottageAmber
import kotlin.math.sin

@Composable
fun GalleryScreen(
    wallpapers: List<WinterWallpaperItem>,
    snowIntensity: SnowIntensity,
    onToggleIntensity: () -> Unit,
    onSelectWallpaper: (WinterWallpaperItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "gallery_snow")
    val snowPhase = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "snow_phase"
    )

    val ambientFlakes = remember {
        List(36) { idx ->
            Triple(
                ((idx * 173 + 29) % 100) / 100f,
                ((idx * 257 + 41) % 100) / 100f,
                1.5f + (idx % 4) * 0.9f
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(ArcticMidnight, DeepWinterNavy, Color(0xFF0E2547))
                )
            )
    ) {
        // Gentle drifting ambient snowflakes in background
        Canvas(modifier = Modifier.fillMaxSize()) {
            val t = snowPhase.value
            for ((i, flake) in ambientFlakes.withIndex()) {
                val y = ((flake.second + t * (1f + (i % 3) * 0.35f)) % 1f) * size.height
                val sway = sin((t * 6.28f + i).toDouble()).toFloat() * 14f
                val x = (flake.first * size.width + sway + size.width) % size.width
                drawCircle(
                    color = Color.White.copy(alpha = 0.25f + (i % 3) * 0.12f),
                    radius = flake.third.dp.toPx(),
                    center = Offset(x, y)
                )
            }
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            val maxContentWidth = if (maxWidth > 680.dp) 680.dp else maxWidth

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .widthIn(max = maxContentWidth)
                    .fillMaxSize()
                    .align(Alignment.TopCenter),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 12.dp,
                    bottom = 28.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                item(span = { GridItemSpan(2) }) {
                    GalleryHeader(
                        snowIntensity = snowIntensity,
                        onToggleIntensity = onToggleIntensity
                    )
                }

                itemsIndexed(
                    items = wallpapers,
                    key = { _, item -> item.id }
                ) { index, item ->
                    FramedWallpaperCard(
                        item = item,
                        index = index,
                        snowPhase = snowPhase.value,
                        onClick = { onSelectWallpaper(item) }
                    )
                }

                item(span = { GridItemSpan(2) }) {
                    Spacer(modifier = Modifier.navigationBarsPadding().height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun GalleryHeader(
    snowIntensity: SnowIntensity,
    onToggleIntensity: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(IcyCyan.copy(alpha = 0.28f), Color(0xFF1E4E8C))
                            )
                        )
                        .border(1.dp, IcyCyan.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.AcUnit,
                        contentDescription = "Qorli Fon belgisi",
                        tint = IcyCyan,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Qorli Fonlar",
                        style = MaterialTheme.typography.headlineSmall,
                        color = SnowWhite
                    )
                    Text(
                        text = "4×6 ramkali qishki jonli sahnalar",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onToggleIntensity)
                    .testTag("gallery_intensity_chip"),
                shape = RoundedCornerShape(50),
                color = Color(0xFF193763),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (snowIntensity == SnowIntensity.HEAVY) WarmCottageAmber else IcyCyan.copy(alpha = 0.55f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.AcUnit,
                        contentDescription = null,
                        tint = if (snowIntensity == SnowIntensity.HEAVY) WarmCottageAmber else IcyCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (snowIntensity == SnowIntensity.HEAVY) "Bo'ron" else "Odatiy",
                        style = MaterialTheme.typography.labelSmall,
                        color = SnowWhite
                    )
                }
            }
        }
    }
}

@Composable
private fun FramedWallpaperCard(
    item: WinterWallpaperItem,
    index: Int,
    snowPhase: Float,
    onClick: () -> Unit
) {
    val frameGradient = if (item.isFeatured) {
        Brush.linearGradient(listOf(FrameGold, FrameSilver, WarmCottageAmber.copy(alpha = 0.85f)))
    } else {
        Brush.linearGradient(listOf(FrameSilver, Color(0xFF7CA7D6), FrameSilver.copy(alpha = 0.8f)))
    }

    // Outer Ornate Picture Frame (Ramkali 4x6 kartochka)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(4f / 6f)
            .shadow(
                elevation = 14.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = IcyCyan.copy(alpha = 0.25f),
                spotColor = Color.Black
            )
            .clip(RoundedCornerShape(20.dp))
            .background(frameGradient)
            .border(
                width = 1.5.dp,
                color = Color.White.copy(alpha = 0.7f),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .semantics { contentDescription = "${item.title} jonli fonini ochish" }
            .testTag("wallpaper_card_${item.id}")
            .padding(6.dp)
    ) {
        // Inner Cream/Ice Matting Frame
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(15.dp))
                .background(Color(0xFF0A1628))
                .border(
                    width = 2.dp,
                    color = Color(0xFFEAF4FF).copy(alpha = 0.75f),
                    shape = RoundedCornerShape(15.dp)
                )
        ) {
            // Top Artwork inside the frame ("ustida rasmi bilan")
            if (item.drawableRes != null) {
                Image(
                    painter = painterResource(id = item.drawableRes),
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                ProceduralMiniPreview(item = item)
            }

            // Live falling snow shimmer inside the frame
            Canvas(modifier = Modifier.fillMaxSize()) {
                for (i in 0 until 14) {
                    val fx = ((i * 83 + index * 29) % 100) / 100f * size.width
                    val fy = (((i * 61) / 100f + snowPhase * (1.2f + (i % 3) * 0.4f)) % 1f) * size.height
                    drawCircle(
                        color = Color.White.copy(alpha = 0.65f),
                        radius = (1.4f + (i % 3) * 0.7f).dp.toPx(),
                        center = Offset(fx, fy)
                    )
                }
            }

            // Bottom subtle dark scrim for clean title legibility
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(92.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0xCC061020),
                                Color(0xF0050C1A)
                            )
                        )
                    )
            )

            // Top Badge ("1-chi Asosiy" / "3D Jonli")
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (item.isFeatured) WarmCottageAmber else Color(0xB309172E),
                    border = if (!item.isFeatured) {
                        androidx.compose.foundation.BorderStroke(0.8.dp, IcyCyan.copy(alpha = 0.6f))
                    } else null
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (item.isFeatured) Icons.Filled.AutoAwesome else Icons.Filled.AcUnit,
                            contentDescription = null,
                            tint = if (item.isFeatured) ArcticMidnight else IcyCyan,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (item.isFeatured) "3D Asosiy" else "#${index + 1}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (item.isFeatured) ArcticMidnight else SnowWhite
                        )
                    }
                }
            }

            // Bottom Nameplate + Play Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = SnowWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = item.subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFBAD6F2),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(IcyCyan.copy(alpha = 0.22f))
                        .border(1.dp, IcyCyan.copy(alpha = 0.7f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = IcyCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ProceduralMiniPreview(item: WinterWallpaperItem) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(item.skyTopColor), Color(item.skyBottomColor))
            )
        )
        // Moon
        drawCircle(
            color = Color(0xFFF8FAFC),
            radius = size.width * 0.10f,
            center = Offset(size.width * 0.75f, size.height * 0.20f)
        )
        // Stars
        for (i in 0 until 20) {
            val sx = ((i * 197 + 19) % 100) / 100f * size.width
            val sy = ((i * 313 + 37) % 55) / 100f * size.height
            drawCircle(
                color = Color.White.copy(alpha = 0.8f),
                radius = 1.6.dp.toPx(),
                center = Offset(sx, sy)
            )
        }
        // Mountain ridge
        val mountain = Path().apply {
            moveTo(0f, size.height * 0.72f)
            lineTo(size.width * 0.25f, size.height * 0.50f)
            lineTo(size.width * 0.55f, size.height * 0.62f)
            lineTo(size.width * 0.82f, size.height * 0.45f)
            lineTo(size.width, size.height * 0.58f)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(mountain, color = Color(item.hillBackColor))

        // Snow floor
        val snowFloor = Path().apply {
            moveTo(0f, size.height * 0.76f)
            quadraticTo(size.width * 0.5f, size.height * 0.70f, size.width, size.height * 0.75f)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(snowFloor, color = Color(0xFFEAF4FF))
    }
}
