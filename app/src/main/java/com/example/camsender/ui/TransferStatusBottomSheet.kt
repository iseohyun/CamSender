package com.example.camsender.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.camsender.databinding.BottomSheetTransferStatusBinding
import com.example.camsender.network.TransferManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class TransferStatusBottomSheet(private val transferManager: TransferManager) : BottomSheetDialogFragment() {

    private var _binding: BottomSheetTransferStatusBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetTransferStatusBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = TransferJobAdapter(
            onRetry = { transferManager.retryJob(it.id) },
            onHold = { job, hold -> transferManager.holdJob(job.id, hold) },
            onRemove = { transferManager.removeJob(it.id) }
        )

        binding.rvTransfers.layoutManager = LinearLayoutManager(context)
        binding.rvTransfers.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            transferManager.jobs.collectLatest {
                adapter.submitList(it)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
