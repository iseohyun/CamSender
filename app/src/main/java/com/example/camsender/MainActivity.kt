package com.example.camsender

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
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
import java.text.SimpleDateFormat
import java.util.*

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
            startCameraAndNsd()
        } else {
            Toast.makeText(this, "Permissions required for core features", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val okHttpClient = OkHttpClient()
        transferManager = TransferManager(okHttpClient)

        cameraHelper = CameraHelper(this, this, binding.previewView)
        
        nsdHelper = NsdHelper(this).apply {
            listener = object : NsdHelper.OnServerFoundListener {
                override fun onServerFound(ip: String, port: Int, apiPath: String?, version: String?) {
                    addIntroLog("서버 발견! 주소: $ip:$port")
                    runOnUiThread {
                        connectToServer(ip, port, apiPath, version)
                    }
                }

                override fun onServerLost() {
                    addIntroLog("서버 연결 유실됨")
                    runOnUiThread {
                        targetServerIp = null
                        targetServerPort = null
                        binding.tvServerStatus.text = "Server Lost. Searching..."
                        binding.searchingOverlay.visibility = View.VISIBLE
                    }
                }
            }
        }

        if (allPermissionsGranted()) {
            startCameraAndNsd()
        } else {
            requestPermissionLauncher.launch(requiredPermissions)
        }

        setupUI()
        observeTransferJobs()
    }

    private fun startCameraAndNsd() {
        addIntroLog("카메라 초기화 중...")
        cameraHelper.startCamera()
        addIntroLog("네트워크 탐색 시작 (mDNS)...")
        nsdHelper.startDiscovery()
    }

    private fun connectToServer(ip: String, port: Int, apiPath: String? = null, version: String? = null) {
        targetServerIp = ip
        targetServerPort = port
        binding.tvServerStatus.text = "Server Found: $ip:$port (v${version ?: "unknown"})"
        binding.etServerIp.setText(ip)
        
        apiPath?.let { transferManager.setApiPath(it) }
        transferManager.recoverPendingJobs(cacheDir, ip, port)

        addIntroLog("서버 연결 완료. 메인 화면으로 진입합니다.")
        binding.searchingOverlay.visibility = View.GONE
    }

    private fun addIntroLog(message: String) {
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        runOnUiThread {
            binding.tvIntroLog.append("[$timestamp] $message\n")
            binding.svIntroLog.post {
                binding.svIntroLog.fullScroll(View.FOCUS_DOWN)
            }
        }
    }

    private fun showManualConfigDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_manual_config, null)
        val etIp = dialogView.findViewById<EditText>(R.id.etDialogIp)
        val etPort = dialogView.findViewById<EditText>(R.id.etDialogPort)

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("연결") { _, _ ->
                val ip = etIp.text.toString()
                val port = etPort.text.toString().toIntOrNull() ?: 8443
                if (ip.isNotEmpty()) {
                    addIntroLog("수동 연결 시도: $ip:$port")
                    connectToServer(ip, port)
                }
            }
            .setNegativeButton("취소", null)
            .show()
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
        if (allPermissionsGranted() && targetServerIp == null) {
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
        binding.btnManualConfig.setOnClickListener {
            showManualConfigDialog()
        }

        binding.btnCapture.setOnClickListener {
            val ip = targetServerIp
            val port = targetServerPort
            
            if (ip == null || port == null) {
                Toast.makeText(this, "서버가 연결되지 않았습니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            cameraHelper.takePicture(object : CameraHelper.OnImageSavedListener {
                override fun onImageSaved(file: File) {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "이미지 저장 완료, 업로드 중...", Toast.LENGTH_SHORT).show()
                        transferManager.addJob(file, ip, port)
                    }
                }

                override fun onError(exception: Exception) {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "촬영 실패: ${exception.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            })
        }

        binding.btnConnect.setOnClickListener {
            val ip = binding.etServerIp.text.toString()
            if (ip.isNotEmpty()) {
                connectToServer(ip, targetServerPort ?: 8443)
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
