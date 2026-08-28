package com.example.camsender

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.ImageCapture
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.camsender.camera.CameraHelper
import com.example.camsender.databinding.ActivityMainBinding
import com.example.camsender.model.TransferJob
import com.example.camsender.network.NsdHelper
import com.example.camsender.network.TransferManager
import com.example.camsender.ui.TransferStatusBottomSheet
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var nsdHelper: NsdHelper
    private lateinit var cameraHelper: CameraHelper
    private lateinit var transferManager: TransferManager

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

        // Standard OkHttpClient (honors Network Security Config)
        val okHttpClient = OkHttpClient()
        transferManager = TransferManager(okHttpClient)

        cameraHelper = CameraHelper(this, this, binding.previewView)
        
        nsdHelper = NsdHelper(this).apply {
            listener = object : NsdHelper.OnServerFoundListener {
                override fun onServerFound(ip: String, port: Int, apiPath: String?, version: String?) {
                    runOnUiThread {
                        targetServerIp = ip
                        targetServerPort = port
                        binding.tvServerStatus.text = "Server Found: $ip:$port (v${version ?: "unknown"})"
                        binding.etServerIp.setText(ip)
                        
                        apiPath?.let { transferManager.setApiPath(it) }
                        
                        // Start recovery when server is found
                        transferManager.recoverPendingJobs(cacheDir, ip, port)
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
        observeTransferJobs()
    }

    private fun observeTransferJobs() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                transferManager.jobs.collectLatest { jobs ->
                    val activeOrFailedCount = jobs.count { 
                        it.status == TransferJob.Status.SENDING || 
                        it.status == TransferJob.Status.FAILED ||
                        it.status == TransferJob.Status.PENDING
                    }
                    if (activeOrFailedCount > 0) {
                        binding.tvBadge.visibility = View.VISIBLE
                        binding.tvBadge.text = activeOrFailedCount.toString()
                    } else {
                        binding.tvBadge.visibility = View.GONE
                    }
                }
            }
        }
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
            val ip = targetServerIp
            val port = targetServerPort
            
            if (ip == null || port == null) {
                Toast.makeText(this, "Server not connected", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            cameraHelper.takePicture(object : CameraHelper.OnImageSavedListener {
                override fun onImageSaved(file: File) {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Captured, uploading...", Toast.LENGTH_SHORT).show()
                        transferManager.addJob(file, ip, port)
                    }
                }

                override fun onError(exception: Exception) {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Capture failed: ${exception.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            })
        }

        binding.btnConnect.setOnClickListener {
            val ip = binding.etServerIp.text.toString()
            if (ip.isNotEmpty()) {
                targetServerIp = ip
                targetServerPort = 8443
                binding.tvServerStatus.text = "Server (Manual): $ip:$targetServerPort"
                transferManager.recoverPendingJobs(cacheDir, ip, targetServerPort!!)
            }
        }

        binding.btnFlash.setOnClickListener {
            val mode = cameraHelper.toggleFlash()
            val icon = when (mode) {
                ImageCapture.FLASH_MODE_ON -> android.R.drawable.ic_menu_compass
                ImageCapture.FLASH_MODE_AUTO -> android.R.drawable.ic_menu_mylocation
                else -> android.R.drawable.ic_menu_close_clear_cancel
            }
            binding.btnFlash.setImageResource(icon)
        }

        binding.fabTransferStatus.setOnClickListener {
            TransferStatusBottomSheet(transferManager).show(supportFragmentManager, "TransferStatus")
        }
    }

    private fun allPermissionsGranted() = requiredPermissions.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }
}
