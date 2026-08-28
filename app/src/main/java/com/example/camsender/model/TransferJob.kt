package com.example.camsender.model

import java.io.File
import java.util.UUID

data class TransferJob(
    val id: String = UUID.randomUUID().toString(),
    val file: File,
    val targetIp: String,
    val targetPort: Int,
    var status: Status = Status.PENDING,
    var errorMessage: String? = null
) {
    enum class Status {
        PENDING,
        SENDING,
        SUCCESS,
        FAILED,
        HOLD
    }
}
