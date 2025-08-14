package com.example.firebaseexample.presentation.notes

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.firebaseexample.databinding.ItemNoteBinding
import com.example.firebaseexample.domain.models.Note
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotesAdapter(
    private val onNoteClick: (Note) -> Unit,
    private val onNoteLongClick: (Note) -> Unit,
    private val onNoteSelectionChanged: (Note, Boolean) -> Unit
) : ListAdapter<Note, NotesAdapter.NoteViewHolder>(NoteDiffCallback()) {

    private var isSelectionMode = false
    private var selectedNotes = setOf<String>()

    fun setSelectionMode(enabled: Boolean) {
        isSelectionMode = enabled
        notifyDataSetChanged()
    }

    fun updateSelectedNotes(selectedNotes: List<Note>) {
        this.selectedNotes = selectedNotes.map { it.id }.toSet()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val binding: ItemNoteBinding = ItemNoteBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NoteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class NoteViewHolder(
        private val binding: ItemNoteBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(note: Note) {
            binding.apply {
                textViewTitle.text = note.title
                textViewContent.text = note.content
                textViewTimestamp.text = formatTimestamp(note.timestamp)

                checkBoxSelection.isVisible = isSelectionMode
                checkBoxSelection.isChecked = selectedNotes.contains(note.id)

                if (isSelectionMode) {
                    root.setOnClickListener {
                        val isCurrentlySelected = selectedNotes.contains(note.id)
                        onNoteSelectionChanged(note, !isCurrentlySelected)
                    }
                    
                    checkBoxSelection.setOnClickListener {
                        onNoteSelectionChanged(note, checkBoxSelection.isChecked)
                    }
                } else {
                    root.setOnClickListener {
                        onNoteClick(note)
                    }
                }

                root.setOnLongClickListener {
                    if (!isSelectionMode) {
                        onNoteLongClick(note)
                    }
                    true
                }

                val alpha = if (isSelectionMode && selectedNotes.contains(note.id)) 0.7f else 1.0f
                root.alpha = alpha
            }
        }

        private fun formatTimestamp(timestamp: Long): String {
            val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
            return dateFormat.format(Date(timestamp))
        }
    }

    private class NoteDiffCallback : DiffUtil.ItemCallback<Note>() {

        override fun areContentsTheSame(oldItem: Note, newItem: Note): Boolean {
            return oldItem == newItem
        }

        override fun areItemsTheSame(oldItem: Note, newItem: Note): Boolean {
            return oldItem.id == newItem.id
        }
    }
} 