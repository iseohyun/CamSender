package com.example.camsender

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.ImageCapture
import androidx.core.content.ContextCompat
import com.example.camsender.camera.CameraHelper
import com.example.camsender.databinding.ActivityMainBinding
import com.example.camsender.network.NsdHelper
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var nsdHelper: NsdHelper
    private lateinit var cameraHelper: CameraHelper

    private var targetServerIp: String? = null
    private var targetServerPort: Int? = null

    private val requiredPermissions = mutableListOf(
        Manifest.permission.CAMERA,
        Manifest.permission.INTERNET,
        Manifest.permission.ACCESS_WIFI_STATE,
        Manifest.permission.CHANGE_WIFI_MULTICAST_STATE
    ).apply {
        if (Build.VERSION.SDK_INT >= 31) {
            add("android.permission.NEARBY_WIFI_DEVICES")
        }
    }.toTypedArray()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            cameraHelper.startCamera()
            nsdHelper.startDiscovery()
        } else {
            Toast.makeText(this, "Permissions required for core features", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraHelper = CameraHelper(this, this, binding.previewView)
        
        nsdHelper = NsdHelper(this).apply {
            listener = object : NsdHelper.OnServerFoundListener {
                override fun onServerFound(ip: String, port: Int) {
                    runOnUiThread {
                        targetServerIp = ip
                        targetServerPort = port
                        binding.tvServerStatus.text = "Server Found: $ip:$port"
                        binding.etServerIp.setText(ip)
                    }
                }

                override fun onServerLost() {
                    runOnUiThread {
                        targetServerIp = null
                        targetServerPort = null
                        binding.tvServerStatus.text = "Server Lost. Searching..."
                    }
                }
            }
        }

        if (allPermissionsGranted()) {
            cameraHelper.startCamera()
        } else {
            requestPermissionLauncher.launch(requiredPermissions)
        }

        setupUI()
    }

    override fun onResume() {
        super.onResume()
        if (allPermissionsGranted()) {
            nsdHelper.startDiscovery()
        }
    }

    override fun onPause() {
        super.onPause()
        nsdHelper.stopDiscovery()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraHelper.stopCamera()
    }

    private fun setupUI() {
        binding.btnCapture.setOnClickListener {
            if (targetServerIp == null) {
                Toast.makeText(this, "Server not connected", Toast.LENGTH_SHORT).show()
                // Proceed anyway for testing if needed? No, let's follow the rule.
                // return@setOnClickListener
            }
            
            cameraHelper.takePicture(object : CameraHelper.OnImageSavedListener {
                override fun onImageSaved(file: File) {
                    Toast.makeText(this@MainActivity, "Saved: ${file.name}", Toast.LENGTH_SHORT).show()
                    Log.d("MainActivity", "Image captured: ${file.absolutePath}")
                    // Milestone 4: Upload file
                }

                override fun onError(exception: Exception) {
                    Toast.makeText(this@MainActivity, "Capture failed: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }

        binding.btnConnect.setOnClickListener {
            val ip = binding.etServerIp.text.toString()
            if (ip.isNotEmpty()) {
                targetServerIp = ip
                targetServerPort = 8443
                binding.tvServerStatus.text = "Server (Manual): $ip:$targetServerPort"
            }
        }

        binding.btnFlash.setOnClickListener {
            val mode = cameraHelper.toggleFlash()
            val icon = when (mode) {
                ImageCapture.FLASH_MODE_ON -> android.R.drawable.ic_menu_compass // Replace with flash icons if available
                ImageCapture.FLASH_MODE_AUTO -> android.R.drawable.ic_menu_mylocation
                else -> android.R.drawable.ic_menu_close_clear_cancel
            }
            binding.btnFlash.setImageResource(icon)
            val modeText = when (mode) {
                ImageCapture.FLASH_MODE_ON -> "ON"
                ImageCapture.FLASH_MODE_AUTO -> "AUTO"
                else -> "OFF"
            }
            Toast.makeText(this, "Flash: $modeText", Toast.LENGTH_SHORT).show()
        }
    }

    private fun allPermissionsGranted() = requiredPermissions.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }
}
