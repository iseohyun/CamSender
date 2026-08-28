package com.example.camsender.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.camsender.databinding.ItemTransferJobBinding
import com.example.camsender.model.TransferJob

class TransferJobAdapter(
    private val onRetry: (TransferJob) -> Unit,
    private val onHold: (TransferJob, Boolean) -> Unit,
    private val onRemove: (TransferJob) -> Unit
) : ListAdapter<TransferJob, TransferJobAdapter.ViewHolder>(DiffCallback) {

    class ViewHolder(val binding: ItemTransferJobBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTransferJobBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val job = getItem(position)
        with(holder.binding) {
            tvFileName.text = job.file.name
            tvStatus.text = when (job.status) {
                TransferJob.Status.PENDING -> "Pending"
                TransferJob.Status.SENDING -> "Sending..."
                TransferJob.Status.SUCCESS -> "Success"
                TransferJob.Status.FAILED -> "Failed: ${job.errorMessage}"
                TransferJob.Status.HOLD -> "Paused"
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
