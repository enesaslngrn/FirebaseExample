package com.example.firebaseexample.presentation.notes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.firebaseexample.R
import com.example.firebaseexample.databinding.DialogAddEditNoteBinding
import com.example.firebaseexample.databinding.FragmentNotesBinding
import com.example.firebaseexample.domain.models.Note
import com.example.firebaseexample.presentation.auth.AuthViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class NotesFragment : Fragment() {

    private var _binding: FragmentNotesBinding? = null
    private val binding: FragmentNotesBinding get() = _binding!!

    private val noteViewModel: NoteViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()

    private lateinit var notesAdapter: NotesAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupClickListeners()
        observeAuthState()
        observeNoteState()
    }

    private fun setupRecyclerView() {
        notesAdapter = NotesAdapter(
            onNoteClick = { note ->
                showAddEditNoteDialog(note)
            },
            onNoteLongClick = { note ->
                showDeleteNoteDialog(note)
            }
        )

        binding.recyclerViewNotes.apply {
            adapter = notesAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupClickListeners() {
        binding.fabAddNote.setOnClickListener {
            showAddEditNoteDialog()
        }
    }

    private fun observeAuthState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                authViewModel.state.collect { authState ->
                    authState.user?.let { user ->
                        noteViewModel.setUserId(user.id)
                    }
                }
            }
        }
    }

    private fun observeNoteState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                noteViewModel.state.collect { state ->
                    updateUI(state)
                }
            }
        }
    }

    private fun updateUI(state: NoteState) {
        binding.apply {
            progressIndicator.isVisible = state.isLoading
            textViewEmptyState.isVisible = state.notes.isEmpty() && state.isInitialized && !state.isLoading
            recyclerViewNotes.isVisible = state.notes.isNotEmpty()
        }

        notesAdapter.submitList(state.notes)

        state.error?.let { error ->
            showSnackbar(error)
            noteViewModel.onEvent(NoteEvent.ClearError)
        }

        state.successMessage?.let { message ->
            showSnackbar(message)
            noteViewModel.onEvent(NoteEvent.ClearSuccessMessage)
        }
    }

    private fun showAddEditNoteDialog(note: Note? = null) {
        val dialogBinding: DialogAddEditNoteBinding = DialogAddEditNoteBinding.inflate(layoutInflater)
        
        val isEditMode: Boolean = note != null
        val title: String = if (isEditMode) getString(R.string.edit_note) else getString(R.string.add_note)

        if (isEditMode && note != null) {
            dialogBinding.editTextTitle.setText(note.title)
            dialogBinding.editTextContent.setText(note.content)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setView(dialogBinding.root)
            .setPositiveButton(if (isEditMode) getString(R.string.update) else getString(R.string.add)) { _, _ ->
                val noteTitle: String = dialogBinding.editTextTitle.text.toString().trim()
                val noteContent: String = dialogBinding.editTextContent.text.toString().trim()

                if (noteTitle.isBlank()) {
                    showSnackbar(getString(R.string.title_cannot_be_empty))
                    return@setPositiveButton
                }

                if (isEditMode && note != null) {
                    val updatedNote: Note = note.copy(
                        title = noteTitle,
                        content = noteContent
                    )
                    noteViewModel.onEvent(NoteEvent.UpdateNote(updatedNote))
                } else {
                    noteViewModel.onEvent(NoteEvent.AddNote(noteTitle, noteContent))
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun showDeleteNoteDialog(note: Note) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.delete_note))
            .setMessage(getString(R.string.delete_note_confirmation, note.title))
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                noteViewModel.onEvent(NoteEvent.DeleteNote(note.id))
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 