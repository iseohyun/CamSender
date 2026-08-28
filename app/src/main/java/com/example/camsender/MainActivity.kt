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
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.camsender.camera.CameraHelper
import com.example.camsender.databinding.ActivityMainBinding
import com.example.camsender.model.TransferJob
import com.example.camsender.network.NsdHelper
import com.example.camsender.network.TransferManager
import com.example.camsender.ui.TransferJobAdapter
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
    private lateinit var jobAdapter: TransferJobAdapter

    private var targetServerIp: String? = null
    private var targetServerPort: Int? = null

    private val prefs by lazy { getSharedPreferences("camsender_prefs", MODE_PRIVATE) }

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
            Toast.makeText(this, "필수 권한이 거부되었습니다.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupDrawer()
        initServices()
        
        if (allPermissionsGranted()) {
            startCameraAndNsd()
        } else {
            requestPermissionLauncher.launch(requiredPermissions)
        }

        setupUI()
        observeTransferJobs()
    }

    private fun setupDrawer() {
        // Set drawer width to 95% of screen width
        val displayMetrics = resources.displayMetrics
        val drawerParams = binding.drawerContent.layoutParams
        drawerParams.width = (displayMetrics.widthPixels * 0.95).toInt()
        binding.drawerContent.layoutParams = drawerParams

        // Setup RecyclerView in Drawer
        jobAdapter = TransferJobAdapter(
            onRetry = { transferManager.retryJob(it.id) },
            onHold = { job, hold -> transferManager.holdJob(job.id, hold) },
            onRemove = { transferManager.removeJob(it.id) }
        )
        binding.rvDrawerTransfers.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = jobAdapter
        }
    }

    private fun initServices() {
        val okHttpClient = OkHttpClient()
        transferManager = TransferManager(okHttpClient)
        cameraHelper = CameraHelper(this, this, binding.previewView)
        
        nsdHelper = NsdHelper(this).apply {
            listener = object : NsdHelper.OnServerFoundListener {
                override fun onServerFound(ip: String, port: Int, apiPath: String?, version: String?) {
                    addIntroLog("서버 발견! 주소: $ip:$port")
                    addIntroLog("상세 정보: API=$apiPath, Version=$version")
                    runOnUiThread {
                        connectToServer(ip, port, apiPath, version)
                    }
                }

                override fun onServerLost() {
                    addIntroLog("서버 연결 유실됨")
                    runOnUiThread {
                        targetServerIp = null
                        targetServerPort = null
                        binding.tvDrawerServerInfo.text = "서버 연결 끊김. 탐색 중..."
                        binding.tvMainStatus.text = "연결 끊김"
                        binding.searchingOverlay.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    private fun startCameraAndNsd() {
        addIntroLog("카메라 초기화 중...")
        cameraHelper.startCamera()
        addIntroLog("네트워크 탐색 시작 (mDNS)...")
        nsdHelper.startDiscovery()
    }

    private fun connectToServer(ip: String, port: Int, apiPath: String? = null, version: String? = null) {
        addIntroLog("서버 연결 시퀀스 시작: $ip:$port")
        targetServerIp = ip
        targetServerPort = port
        val infoText = "Server: $ip:$port (v${version ?: "unknown"})"
        binding.tvDrawerServerInfo.text = infoText
        binding.tvMainStatus.text = "연결됨: $ip"
        
        // Save to preferences
        addIntroLog("서버 주소 저장 중...")
        prefs.edit().putString("last_ip", ip).putInt("last_port", port).apply()
        
        apiPath?.let { 
            addIntroLog("커스텀 API 경로 설정: $it")
            transferManager.setApiPath(it) 
        }
        
        addIntroLog("미전송 파일 복구 시도 중...")
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

        // Pre-fill with current connection OR last known connection
        val lastIp = targetServerIp ?: prefs.getString("last_ip", "")
        val lastPort = targetServerPort ?: prefs.getInt("last_port", 8443)

        etIp.setText(lastIp)
        etPort.setText(lastPort.toString())

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
                    // Sort by latest first
                    val sortedJobs = jobs.sortedByDescending { it.timestamp }
                    jobAdapter.submitList(sortedJobs)

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

        binding.btnFlash.setOnClickListener {
            val mode = cameraHelper.toggleFlash()
            val icon = when (mode) {
                ImageCapture.FLASH_MODE_ON -> android.R.drawable.ic_menu_compass
                ImageCapture.FLASH_MODE_AUTO -> android.R.drawable.ic_menu_mylocation
                else -> android.R.drawable.ic_menu_close_clear_cancel
            }
            binding.btnFlash.setImageResource(icon)
        }

        binding.btnOpenDrawer.setOnClickListener {
            binding.drawerLayout.openDrawer(binding.drawerContent)
        }

        binding.btnDrawerManualConfig.setOnClickListener {
            showManualConfigDialog()
        }
    }

    private fun allPermissionsGranted() = requiredPermissions.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }
}
