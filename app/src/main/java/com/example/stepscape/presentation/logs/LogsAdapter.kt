package com.example.stepscape.presentation.logs

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.stepscape.databinding.ItemLogBinding
import com.example.stepscape.domain.model.StepSession
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone


class LogsAdapter : ListAdapter<StepSession, LogsAdapter.LogViewHolder>(LogDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val binding = ItemLogBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return LogViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class LogViewHolder(
        private val binding: ItemLogBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val isoDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        fun bind(session: StepSession) {
            val timestamp = isoDateFormat.format(Date(session.startTime))
            val userName = session.userId.ifEmpty { "Aurora" }
            val syncStatus = if (session.syncedToFirebase) "Data synced to Firebase." else "Not synced."
            
            val logEntry = "StepLog: $timestamp - User '$userName' took ${session.steps} steps. $syncStatus"
            binding.tvLogEntry.text = logEntry
        }
    }

    class LogDiffCallback : DiffUtil.ItemCallback<StepSession>() {
        override fun areItemsTheSame(oldItem: StepSession, newItem: StepSession): Boolean {
            return oldItem.startTime == newItem.startTime && oldItem.userId == newItem.userId
        }

        override fun areContentsTheSame(oldItem: StepSession, newItem: StepSession): Boolean {
            return oldItem == newItem
        }
    }
}
