package com.example.camsender.ui

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.camsender.databinding.ItemTransferJobBinding
import com.example.camsender.model.TransferJob
import java.text.SimpleDateFormat
import java.util.*

class TransferJobAdapter(
    private val onRetry: (TransferJob) -> Unit,
    private val onHold: (TransferJob, Boolean) -> Unit,
    private val onRemove: (TransferJob) -> Unit
) : ListAdapter<TransferJob, TransferJobAdapter.ViewHolder>(DiffCallback) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    class ViewHolder(val binding: ItemTransferJobBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTransferJobBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val job = getItem(position)
        with(holder.binding) {
            tvFileName.text = job.file.name
            tvTimestamp.text = dateFormat.format(Date(job.timestamp))
            tvStatus.text = when (job.status) {
                TransferJob.Status.PENDING -> "대기 중"
                TransferJob.Status.SENDING -> "전송 중..."
                TransferJob.Status.SUCCESS -> "성공"
                TransferJob.Status.FAILED -> "실패: ${job.errorMessage}"
                TransferJob.Status.HOLD -> "보류됨"
            }

            // Thumbnail loading
            if (job.file.exists()) {
                val options = BitmapFactory.Options().apply {
                    inSampleSize = 8
                }
                val bitmap = BitmapFactory.decodeFile(job.file.absolutePath, options)
                ivThumbnail.setImageBitmap(bitmap)
            } else {
                ivThumbnail.setImageDrawable(null)
            }

            btnRetry.visibility = if (job.status == TransferJob.Status.FAILED) View.VISIBLE else View.GONE
            btnHold.setImageResource(
                if (job.status == TransferJob.Status.HOLD) android.R.drawable.ic_media_play 
                else android.R.drawable.ic_media_pause
            )

            btnRetry.setOnClickListener { onRetry(job) }
            btnHold.setOnClickListener { onHold(job, job.status != TransferJob.Status.HOLD) }
            btnRemove.setOnClickListener { onRemove(job) }
        }
    }

    object DiffCallback : DiffUtil.ItemCallback<TransferJob>() {
        override fun areItemsTheSame(oldItem: TransferJob, newItem: TransferJob) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: TransferJob, newItem: TransferJob) = oldItem == newItem
    }
}
