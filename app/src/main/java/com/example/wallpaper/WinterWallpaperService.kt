package com.example.wallpaper

import android.graphics.Canvas
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.MotionEvent
import android.view.SurfaceHolder
import com.example.model.WinterWallpaperRepository
import com.example.simulation.WinterSceneRenderer
import com.example.simulation.WinterSceneState

class WinterWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine {
        return WinterEngine()
    }

    inner class WinterEngine : Engine() {
        private val handler = Handler(Looper.getMainLooper())
        private val sceneState = WinterSceneState()
        private lateinit var renderer: WinterSceneRenderer

        private var visible = false
        private var width = 0f
        private var height = 0f
        private var lastFrameNanos = 0L

        private val frameRunnable = object : Runnable {
            override fun run() {
                drawFrame()
                if (visible) {
                    handler.postDelayed(this, 16L)
                }
            }
        }

        override fun onCreate(surfaceHolder: SurfaceHolder?) {
            super.onCreate(surfaceHolder)
            renderer = WinterSceneRenderer(applicationContext)
            setTouchEventsEnabled(true)
        }

        override fun onSurfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            this.width = width.toFloat()
            this.height = height.toFloat()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            this.visible = visible
            handler.removeCallbacks(frameRunnable)
            if (visible) {
                lastFrameNanos = System.nanoTime()
                sceneState.intensity = WinterWallpaperRepository.getSnowIntensity(applicationContext)
                handler.post(frameRunnable)
            }
        }

        override fun onTouchEvent(event: MotionEvent?) {
            super.onTouchEvent(event)
            if (event != null && width > 0f && height > 0f) {
                if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_MOVE) {
                    sceneState.brushSnowAt(
                        xNorm = (event.x / width).coerceIn(0f, 1f),
                        yNorm = (event.y / height).coerceIn(0f, 1f)
                    )
                }
            }
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder?) {
            super.onSurfaceDestroyed(holder)
            visible = false
            handler.removeCallbacks(frameRunnable)
        }

        override fun onDestroy() {
            super.onDestroy()
            visible = false
            handler.removeCallbacks(frameRunnable)
        }

        private fun drawFrame() {
            val holder = surfaceHolder ?: return
            if (width <= 0f || height <= 0f) return

            val now = System.nanoTime()
            val dt = if (lastFrameNanos == 0L) 0.016f else (now - lastFrameNanos) / 1_000_000_000f
            lastFrameNanos = now

            sceneState.intensity = WinterWallpaperRepository.getSnowIntensity(applicationContext)
            sceneState.update(dt)

            val wallpaperId = WinterWallpaperRepository.getSelectedWallpaperId(applicationContext)
            val wallpaper = WinterWallpaperRepository.getById(wallpaperId)
            val density = applicationContext.resources.displayMetrics.density

            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas()
                if (canvas != null) {
                    renderer.draw(
                        canvas = canvas,
                        width = width,
                        height = height,
                        density = density,
                        wallpaper = wallpaper,
                        state = sceneState
                    )
                }
            } finally {
                if (canvas != null) {
                    try {
                        holder.unlockCanvasAndPost(canvas)
                    } catch (_: IllegalArgumentException) {
                    }
                }
            }
        }
    }
}
