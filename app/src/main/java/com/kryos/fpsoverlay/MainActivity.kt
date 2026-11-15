package com.kryos.fpsoverlay

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var switchOverlay: Switch
    private lateinit var btnHideController: Button
    private lateinit var switchFps: Switch
    private lateinit var switchText: Switch
    private lateinit var edtCustomText: EditText
    private lateinit var spinnerFpsColor: Spinner
    private lateinit var seekBarFpsSize: SeekBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        switchOverlay = findViewById(R.id.switchOverlay)
        btnHideController = findViewById(R.id.btnHideController)
        switchFps = findViewById(R.id.switchFps)
        switchText = findViewById(R.id.switchText)
        edtCustomText = findViewById(R.id.edtCustomText)
        spinnerFpsColor = findViewById(R.id.spinnerFpsColor)
        seekBarFpsSize = findViewById(R.id.seekBarFpsSize)

        setupSpinners()

        switchOverlay.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (!checkOverlayPermission()) {
                    requestOverlayPermission()
                    switchOverlay.isChecked = false
                } else {
                    startOverlayService()
                }
            } else {
                stopService(Intent(this, OverlayService::class.java))
            }
        }

        btnHideController.setOnClickListener {
            moveTaskToBack(true)
        }

        edtCustomText.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) { sendConfigToService() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        switchFps.setOnCheckedChangeListener { _, _ -> sendConfigToService() }
        switchText.setOnCheckedChangeListener { _, _ -> sendConfigToService() }
        seekBarFpsSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) { sendConfigToService() }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun setupSpinners() {
        val colors = arrayOf("Xanh Neon", "Đỏ", "Vàng", "Trắng", "Tím")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, colors)
        spinnerFpsColor.adapter = adapter
        spinnerFpsColor.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) { sendConfigToService() }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun checkOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
    }

    private fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        startActivityForResult(intent, 1001)
        Toast.makeText(this, "Vui lòng cấp quyền hiển thị trên ứng dụng khác", Toast.LENGTH_LONG).show()
    }

    private fun startOverlayService() {
        val intent = Intent(this, OverlayService::class.java).apply {
            putExtra("fps_visible", switchFps.isChecked)
            putExtra("text_visible", switchText.isChecked)
            putExtra("custom_text", edtCustomText.text.toString())
            putExtra("color_pos", spinnerFpsColor.selectedItemPosition)
            putExtra("font_size", seekBarFpsSize.progress + 10f)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun sendConfigToService() {
        if (switchOverlay.isChecked) {
            startOverlayService()
        }
    }
}
