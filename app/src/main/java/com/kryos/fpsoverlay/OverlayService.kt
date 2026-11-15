package com.kryos.fpsoverlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.*
import android.widget.TextView

class OverlayService : Service(), Choreographer.FrameCallback {

    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: View
    private lateinit var txtFps: TextView
    private lateinit var txtCustomText: TextView
    private lateinit var params: WindowManager.LayoutParams

    private var lastFrameTimeNanos: Long = 0
    private var frameCount = 0
    private var lastFpsUpdate = 0L

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundServiceNotification()
        initOverlayView()
        Choreographer.getInstance().postFrameCallback(this)
    }

    private fun startForegroundServiceNotification() {
        val channelId = "kryos_fps_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Kryos FPS Overlay Service",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }

        val notification: Notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId)
                .setContentTitle("Kryos FPS đang chạy")
                .setContentText("Đang hiển thị thông số FPS thời gian thực")
                .setSmallIcon(android.R.drawable.ic_menu_info_details)
                .build()
        } else {
            Notification.Builder(this)
                .setContentTitle("Kryos FPS đang chạy")
                .setSmallIcon(android.R.drawable.ic_menu_info_details)
                .build()
        }
        startForeground(1, notification)
    }

    private fun initOverlayView() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        overlayView = LayoutInflater.from(this).inflate(R.layout.overlay_view, null)

        txtFps = overlayView.findViewById(R.id.txtFps)
        txtCustomText = overlayView.findViewById(R.id.txtCustomText)

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 100
        }

        overlayView.setOnTouchListener(object : View.OnTouchListener {
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = initialX + (event.rawX - initialTouchX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager.updateViewLayout(overlayView, params)
                        return true
                    }
                }
                return false
            }
        })

        windowManager.addView(overlayView, params)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            val fpsVisible = it.getBooleanExtra("fps_visible", true)
            val textVisible = it.getBooleanExtra("text_visible", true)
            val customText = it.getStringExtra("custom_text") ?: "Kryos Gaming"
            val colorPos = it.getIntExtra("color_pos", 0)
            val fontSize = it.getFloatExtra("font_size", 16f)

            txtFps.visibility = if (fpsVisible) View.VISIBLE else View.GONE
            txtCustomText.visibility = if (textVisible) View.VISIBLE else View.GONE
            txtCustomText.text = customText
            txtFps.textSize = fontSize

            when (colorPos) {
                0 -> txtFps.setTextColor(Color.parseColor("#00FF66"))
                1 -> txtFps.setTextColor(Color.parseColor("#FF0033"))
                2 -> txtFps.setTextColor(Color.parseColor("#FFCC00"))
                3 -> txtFps.setTextColor(Color.parseColor("#FFFFFF"))
                4 -> txtFps.setTextColor(Color.parseColor("#B026FF"))
            }
        }
        return START_STICKY
    }

    override fun doFrame(frameTimeNanos: Long) {
        if (lastFrameTimeNanos != 0L) {
            frameCount++
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastFpsUpdate >= 500) {
                val fps = (frameCount * 1000L / (currentTime - lastFpsUpdate)).toInt()
                txtFps.text = "$fps FPS"
                frameCount = 0
                lastFpsUpdate = currentTime
            }
        }
        lastFrameTimeNanos = frameTimeNanos
        Choreographer.getInstance().postFrameCallback(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        Choreographer.getInstance().removeFrameCallback(this)
        if (::overlayView.isInitialized) {
            windowManager.removeView(overlayView)
        }
    }
}
