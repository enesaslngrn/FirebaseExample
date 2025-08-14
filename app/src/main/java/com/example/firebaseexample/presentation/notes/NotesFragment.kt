package com.example.firebaseexample.presentation.notes

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
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

    private var pendingAttachNote: Note? = null
    private val pickAttachmentLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        val note = pendingAttachNote ?: return@registerForActivityResult
        if (uri != null) {
            noteViewModel.onEvent(NoteEvent.UploadAttachment(note, uri))
        }
        pendingAttachNote = null
    }

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
        setupToolbars()
        observeAuthState()
        observeNoteState()
    }

    private fun setupRecyclerView() {
        notesAdapter = NotesAdapter(
            onNoteClick = { note ->
                showAddEditNoteDialog(note)
            },
            onNoteLongClick = { note ->
                if (!noteViewModel.state.value.isSelectionMode) {
                    showDeleteNoteDialog(note)
                }
            },
            onNoteSelectionChanged = { note, _ ->
                noteViewModel.onEvent(NoteEvent.ToggleNoteSelection(note))
            },
            onAttachClick = { note ->
                pendingAttachNote = note
                pickAttachmentLauncher.launch("image/*")
            },
            onRemoveAttachmentClick = { note ->
                noteViewModel.onEvent(NoteEvent.DeleteAttachment(note))
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

    private fun setupToolbars() {

        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            handleToolbarMenuClick(menuItem)
        }

        binding.toolbarSelection.setNavigationOnClickListener {
            noteViewModel.onEvent(NoteEvent.ExitSelectionMode)
        }

        binding.toolbarSelection.setOnMenuItemClickListener { menuItem ->
            handleSelectionMenuClick(menuItem)
        }
    }

    private fun handleToolbarMenuClick(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.action_select_notes -> {
                noteViewModel.onEvent(NoteEvent.EnterSelectionMode)
                true
            }
            R.id.action_delete_all -> {
                showDeleteAllNotesDialog()
                true
            }
            else -> false
        }
    }

    private fun handleSelectionMenuClick(menuItem: MenuItem): Boolean {
        val state = noteViewModel.state.value
        return when (menuItem.itemId) {
            R.id.action_delete_selected -> {
                if (state.selectedNotes.isNotEmpty()) {
                    showDeleteSelectedNotesDialog(state.selectedNotes)
                }
                true
            }
            R.id.action_select_all -> {

                state.notes.forEach { note ->
                    if (!state.selectedNotes.contains(note)) {
                        noteViewModel.onEvent(NoteEvent.ToggleNoteSelection(note))
                    }
                }
                true
            }
            else -> false
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

            toolbar.isVisible = !state.isSelectionMode
            toolbarSelection.isVisible = state.isSelectionMode
            fabAddNote.isVisible = !state.isSelectionMode

            if (state.isSelectionMode) {
                toolbarSelection.title = getString(R.string.selected_count, state.selectedNotes.size)
            }
        }

        notesAdapter.submitList(state.notes)
        notesAdapter.setSelectionMode(state.isSelectionMode)
        notesAdapter.updateSelectedNotes(state.selectedNotes)

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
            .setPositiveButton(if (isEditMode) R.string.update else R.string.attach) { _, _ ->
                val titleText: String = dialogBinding.editTextTitle.text.toString().trim()
                val contentText: String = dialogBinding.editTextContent.text.toString().trim()

                if (titleText.isBlank()) {
                    showSnackbar(getString(R.string.title_cannot_be_empty))
                    return@setPositiveButton
                }

                if (isEditMode && note != null) {
                    noteViewModel.onEvent(NoteEvent.UpdateNote(note.copy(title = titleText, content = contentText)))
                } else {
                    noteViewModel.onEvent(NoteEvent.AddNote(titleText, contentText))
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showDeleteNoteDialog(note: Note) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_note)
            .setMessage(getString(R.string.delete_note_confirmation, note.title))
            .setPositiveButton(R.string.delete) { _, _ ->
                noteViewModel.onEvent(NoteEvent.DeleteNote(note.id))
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showDeleteAllNotesDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_all_notes)
            .setMessage(R.string.delete_all_confirmation)
            .setPositiveButton(R.string.delete) { _, _ ->
                noteViewModel.onEvent(NoteEvent.DeleteAllNotes)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showDeleteSelectedNotesDialog(selectedNotes: List<Note>) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_selected)
            .setMessage(getString(R.string.delete_selected_confirmation, selectedNotes.size))
            .setPositiveButton(R.string.delete) { _, _ ->
                noteViewModel.onEvent(NoteEvent.DeleteSelectedNotes(selectedNotes.map { it.id }))
            }
            .setNegativeButton(R.string.cancel, null)
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