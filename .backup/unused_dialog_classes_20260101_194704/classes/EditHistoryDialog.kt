package com.synapse.social.studioasinc.chat

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.SupabaseClient
import com.synapse.social.studioasinc.databinding.DialogEditHistoryBinding
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import java.text.SimpleDateFormat
import java.util.*

/**
 * Dialog for viewing message edit history
 */
class EditHistoryDialog : DialogFragment() {

    private var _binding: DialogEditHistoryBinding? = null
    private val binding get() = _binding!!

    private var messageId: String? = null
    private val editHistory = mutableListOf<EditHistoryItem>()

    data class EditHistoryItem(
        val timestamp: Long,
        val previousContent: String,
        val editedBy: String
    )

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogEditHistoryBinding.inflate(layoutInflater)

        messageId = arguments?.getString(ARG_MESSAGE_ID)

        setupViews()
        loadEditHistory()

        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .create()
    }

    override fun onStart() {
        super.onStart()
        // Make dialog full width
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun setupViews() {
        binding.rvEditHistory.layoutManager = LinearLayoutManager(requireContext())
        binding.rvEditHistory.adapter = EditHistoryAdapter(editHistory)

        binding.btnClose.setOnClickListener {
            dismiss()
        }
    }

    private fun loadEditHistory() {
        lifecycleScope.launch {
            try {
                val history = fetchEditHistory(messageId ?: return@launch)
                editHistory.clear()
                editHistory.addAll(history)
                
                withContext(Dispatchers.Main) {
                    if (editHistory.isEmpty()) {
                        binding.rvEditHistory.visibility = View.GONE
                        binding.tvEmptyState.visibility = View.VISIBLE
                    } else {
                        binding.rvEditHistory.visibility = View.VISIBLE
                        binding.tvEmptyState.visibility = View.GONE
                        binding.rvEditHistory.adapter?.notifyDataSetChanged()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.rvEditHistory.visibility = View.GONE
                    binding.tvEmptyState.visibility = View.VISIBLE
                    binding.tvEmptyState.text = "Failed to load edit history"
                }
            }
        }
    }

    private suspend fun fetchEditHistory(messageId: String): List<EditHistoryItem> {
        return withContext(Dispatchers.IO) {
            try {
                val result = SupabaseClient.client.from("message_edit_history")
                    .select(columns = Columns.raw("*")) {
                        filter {
                            eq("message_id", messageId)
                        }
                    }
                    .decodeList<JsonObject>()

                result.sortedByDescending { json ->
                    json["edited_at"]?.toString()?.removeSurrounding("\"")?.toLongOrNull() ?: 0L
                }.map { json ->
                    EditHistoryItem(
                        timestamp = json["edited_at"]?.toString()?.removeSurrounding("\"")?.toLongOrNull() ?: 0L,
                        previousContent = json["previous_content"]?.toString()?.removeSurrounding("\"") ?: "",
                        editedBy = json["edited_by"]?.toString()?.removeSurrounding("\"") ?: ""
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("EditHistoryDialog", "Error fetching edit history", e)
                emptyList()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_MESSAGE_ID = "message_id"

        fun newInstance(messageId: String): EditHistoryDialog {
            return EditHistoryDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_MESSAGE_ID, messageId)
                }
            }
        }
    }

    /**
     * RecyclerView adapter for edit history items
     */
    private class EditHistoryAdapter(
        private val items: List<EditHistoryItem>
    ) : RecyclerView.Adapter<EditHistoryAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_edit_history, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount(): Int = items.size

        class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val tvTimestamp: TextView = itemView.findViewById(R.id.tv_timestamp)
            private val tvPreviousContent: TextView = itemView.findViewById(R.id.tv_previous_content)
            private val divider: View = itemView.findViewById(R.id.divider)

            fun bind(item: EditHistoryItem) {
                tvTimestamp.text = formatTimestamp(item.timestamp)
                tvPreviousContent.text = item.previousContent
                
                // Hide divider for last item
                val position = bindingAdapterPosition
                val totalItems = itemView.parent?.let { parent ->
                    if (parent is RecyclerView) parent.adapter?.itemCount else null
                } ?: 0
                divider.visibility = if (position == totalItems - 1) {
                    View.GONE
                } else {
                    View.VISIBLE
                }
            }

            private fun formatTimestamp(timestamp: Long): String {
                val now = System.currentTimeMillis()
                val diff = now - timestamp

                return when {
                    diff < 60_000 -> "Just now"
                    diff < 3600_000 -> "${diff / 60_000} minutes ago"
                    diff < 86400_000 -> "${diff / 3600_000} hours ago"
                    diff < 604800_000 -> "${diff / 86400_000} days ago"
                    else -> SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(timestamp))
                }
            }
        }
    }
}
