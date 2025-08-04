package com.example.firebaseexample.presentation.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.firebaseexample.R
import com.example.firebaseexample.databinding.FragmentHomeBinding
import com.example.firebaseexample.presentation.auth.AuthEvent
import com.example.firebaseexample.presentation.auth.AuthState
import com.example.firebaseexample.presentation.auth.AuthViewModel
import com.example.firebaseexample.presentation.notes.NotesFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        observeAuthState()
    }

    private fun setupUI() {
        binding.signOutButton.setOnClickListener {
            authViewModel.onEvent(AuthEvent.SignOut)
        }
        
        binding.deleteAccountButton.setOnClickListener {
            showDeleteAccountConfirmationDialog()
        }
        
        binding.sendVerificationButton.setOnClickListener {
            authViewModel.onEvent(AuthEvent.SendEmailVerification)
        }
        
        binding.buttonMyNotes.setOnClickListener {
            checkEmailVerificationAndNavigate()
        }
    }

    private fun showDeleteAccountConfirmationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.delete_account_title))
            .setMessage(getString(R.string.delete_account_message))
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                authViewModel.onEvent(AuthEvent.DeleteAccount)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun checkEmailVerificationAndNavigate() {
        viewLifecycleOwner.lifecycleScope.launch {
            authViewModel.reloadCurrentUser {
                val user = authViewModel.state.value.user
                if (user != null && user.isEmailVerified == true) {
                    navigateToNotes()
                } else {
                    Snackbar.make(binding.root, getString(R.string.email_verification_required), Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun navigateToNotes() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, NotesFragment())
            .addToBackStack(null)
            .commit()
    }

    private fun observeAuthState() {
        viewLifecycleOwner.lifecycleScope.launch {
            authViewModel.state.collectLatest { state ->
                updateUI(state)
                handleAccountDeletion(state)
            }
        }
    }

    private fun updateUI(state: AuthState) {
        binding.progressIndicator.visibility = if (state.isLoading) View.VISIBLE else View.GONE
        
        state.user?.let { user ->
            binding.userEmailTextView.text = user.email
            binding.userIdTextView.text = getString(R.string.user_id, user.id)
            user.isEmailVerified?.let {
                binding.verifiedTextView.isVisible = it
                binding.sendVerificationButton.isVisible = !it
            }
            Timber.d("User info displayed: ${user.email}")
        }

        state.verificationMessage?.let {
            Snackbar.make(binding.root, getString(R.string.verification_email_sent), Snackbar.LENGTH_LONG).show()
            authViewModel.onEvent(AuthEvent.ClearSuccessMessage)
        }

        state.error?.let { error ->
            Snackbar.make(binding.root, error, Snackbar.LENGTH_LONG).show()
            authViewModel.onEvent(AuthEvent.ClearError)
        }
    }

    private fun handleAccountDeletion(state: AuthState) {
        if (state.accountDeleted && state.successMessage != null) {
            Snackbar.make(binding.root, state.successMessage, Snackbar.LENGTH_LONG).show()
            authViewModel.onEvent(AuthEvent.ClearSuccessMessage)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 