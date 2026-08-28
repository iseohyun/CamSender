package com.example.camsender.network

import android.util.Log
import com.example.camsender.model.TransferJob
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException

class TransferManager(private val sslManager: DynamicSslManager) {

    private val _jobs = MutableStateFlow<List<TransferJob>>(emptyList())
    val jobs: StateFlow<List<TransferJob>> = _jobs

    private var currentApiPath: String = "/upload"

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun setApiPath(path: String) {
        Log.d("TransferManager", "API Path updated to: $path")
        currentApiPath = if (path.startsWith("/")) path else "/$path"
    }

    fun addJob(file: File, ip: String, port: Int) {
        val job = TransferJob(file = file, targetIp = ip, targetPort = port)
        synchronized(_jobs) {
            _jobs.value = _jobs.value + job
        }
        processNext()
    }

    fun recoverPendingJobs(cacheDir: File, ip: String, port: Int) {
        val files = cacheDir.listFiles { _, name -> name.startsWith("IMG_") && name.endsWith(".jpg") }
        files?.forEach { file ->
            val alreadyExists = _jobs.value.any { it.file.absolutePath == file.absolutePath }
            if (!alreadyExists) {
                val job = TransferJob(file = file, targetIp = ip, targetPort = port, status = TransferJob.Status.FAILED, errorMessage = "이전 세션에서 복구됨")
                synchronized(_jobs) {
                    _jobs.value = _jobs.value + job
                }
            }
        }
    }

    fun retryJob(jobId: String) {
        updateJobStatus(jobId, TransferJob.Status.PENDING)
        processNext()
    }

    fun holdJob(jobId: String, hold: Boolean) {
        updateJobStatus(jobId, if (hold) TransferJob.Status.HOLD else TransferJob.Status.PENDING)
        if (!hold) processNext()
    }

    fun removeJob(jobId: String) {
        synchronized(_jobs) {
            _jobs.value = _jobs.value.filter { it.id != jobId }
        }
    }

    private fun updateJobStatus(jobId: String, status: TransferJob.Status, error: String? = null) {
        synchronized(_jobs) {
            _jobs.value = _jobs.value.map {
                if (it.id == jobId) it.copy(status = status, errorMessage = error) else it
            }
        }
    }

    private fun processNext() {
        val nextJob = _jobs.value.find { it.status == TransferJob.Status.PENDING } ?: return
        
        scope.launch {
            uploadFile(nextJob)
        }
    }

    private suspend fun isServerHealthy(client: OkHttpClient, ip: String, port: Int): Pair<Boolean, String?> {
        val url = "https://$ip:$port/health"
        Log.d("TransferManager", "Attempting Health Check: $url")
        val request = Request.Builder().url(url).build()
        return try {
            client.newCall(request).execute().use { response ->
                Log.d("TransferManager", "Health Check Response: ${response.code}")
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val json = JSONObject(body)
                    val status = json.optString("status", "")
                    val storageReady = json.optJSONObject("storage")?.optBoolean("ready", false) ?: false
                    if (status == "running" && storageReady) {
                        Log.d("TransferManager", "Server is Healthy and Ready")
                        Pair(true, null)
                    } else {
                        val msg = "서버 상태 비정상 ($status) 또는 저장소 준비 안 됨"
                        Pair(false, msg)
                    }
                } else {
                    Pair(false, "헬스체크 실패 (HTTP ${response.code})")
                }
            }
        } catch (e: Exception) {
            Pair(false, "서버 연결 불가: ${e.localizedMessage}")
        }
    }

    private suspend fun uploadFile(job: TransferJob) {
        updateJobStatus(job.id, TransferJob.Status.SENDING)

        // 1. Get Strict Client
        val client = sslManager.getStrictClient(job.targetIp) ?: run {
            updateJobStatus(job.id, TransferJob.Status.FAILED, "서버 페어링 필요 (인증서 없음)")
            return
        }

        // 2. Health Check
        val (healthy, healthError) = isServerHealthy(client, job.targetIp, job.targetPort)
        if (!healthy) {
            updateJobStatus(job.id, TransferJob.Status.FAILED, healthError)
            return
        }

        // 3. Upload
        val url = "https://${job.targetIp}:${job.targetPort}$currentApiPath"
        val requestBody = job.file.asRequestBody("image/jpeg".toMediaType())
        val multipartBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", job.file.name, requestBody)
            .build()

        val request = Request.Builder().url(url).post(multipartBody).build()

        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Log.d("TransferManager", "Upload successful: ${job.file.name}")
                    updateJobStatus(job.id, TransferJob.Status.SUCCESS)
                    if (job.file.exists()) job.file.delete()
                } else {
                    val errorMsg = "서버 오류: ${response.code}"
                    updateJobStatus(job.id, TransferJob.Status.FAILED, errorMsg)
                }
            }
        } catch (e: Exception) {
            Log.e("TransferManager", "Upload exception", e)
            if (e is SSLHandshakeException) {
                sslManager.clearPairing(job.targetIp)
                updateJobStatus(job.id, TransferJob.Status.FAILED, "보안 인증서가 변경되었습니다. 다시 페어링해 주세요.")
            } else {
                val mappedError = when(e) {
                    is SocketTimeoutException -> "서버 연결 시간 초과"
                    is UnknownHostException -> "서버 주소를 찾을 수 없음"
                    else -> "전송 오류: ${e.localizedMessage}"
                }
                updateJobStatus(job.id, TransferJob.Status.FAILED, mappedError)
            }
        } finally {
            processNext()
        }
    }
}
