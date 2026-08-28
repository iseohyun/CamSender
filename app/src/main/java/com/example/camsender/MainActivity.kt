package com.example.camsender

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.camsender.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

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
            startCameraPreview()
        } else {
            Toast.makeText(this, "Permissions required for core features", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (allPermissionsGranted()) {
            startCameraPreview()
        } else {
            requestPermissionLauncher.launch(requiredPermissions)
        }

        setupUI()
    }

    private fun setupUI() {
        binding.btnCapture.setOnClickListener {
            Toast.makeText(this, "Capture (TBD)", Toast.LENGTH_SHORT).show()
        }

        binding.btnConnect.setOnClickListener {
            val ip = binding.etServerIp.text.toString()
            binding.tvServerStatus.text = "Server IP: $ip"
            Toast.makeText(this, "Server set to $ip", Toast.LENGTH_SHORT).show()
        }
    }

    private fun allPermissionsGranted() = requiredPermissions.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun startCameraPreview() {
        // CameraX implementation will follow in Milestone 3
        binding.tvServerStatus.append("\n(Camera Ready)")
    }
}
