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
import com.example.camsender.network.NsdHelper

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var nsdHelper: NsdHelper

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
            startCameraPreview()
            nsdHelper.startDiscovery()
        } else {
            Toast.makeText(this, "Permissions required for core features", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        nsdHelper = NsdHelper(this).apply {
            listener = object : NsdHelper.OnServerFoundListener {
                override fun onServerFound(ip: String, port: Int) {
                    runOnUiThread {
                        targetServerIp = ip
                        targetServerPort = port
                        binding.tvServerStatus.text = "Server Found: $ip:$port"
                        binding.etServerIp.setText(ip)
                        Toast.makeText(this@MainActivity, "Server discovered automatically", Toast.LENGTH_SHORT).show()
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
            startCameraPreview()
        } else {
            requestPermissionLauncher.launch(requiredPermissions)
        }

        setupUI()
    }

    override fun onResume() {
        super.onResume()
        if (allPermissionsGranted()) {
            binding.tvServerStatus.text = "Searching for CamSender server..."
            nsdHelper.startDiscovery()
        }
    }

    override fun onPause() {
        super.onPause()
        nsdHelper.stopDiscovery()
    }

    private fun setupUI() {
        binding.btnCapture.setOnClickListener {
            if (targetServerIp == null) {
                Toast.makeText(this, "Server not connected", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            Toast.makeText(this, "Capture (TBD)", Toast.LENGTH_SHORT).show()
        }

        binding.btnConnect.setOnClickListener {
            val ip = binding.etServerIp.text.toString()
            if (ip.isNotEmpty()) {
                targetServerIp = ip
                targetServerPort = 8443 // Default port for manual entry
                binding.tvServerStatus.text = "Server (Manual): $ip:$targetServerPort"
                Toast.makeText(this, "Server set manually to $ip", Toast.LENGTH_SHORT).show()
            }
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
