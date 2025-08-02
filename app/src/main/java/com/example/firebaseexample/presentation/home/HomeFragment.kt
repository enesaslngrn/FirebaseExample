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
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import com.google.android.material.snackbar.Snackbar
import androidx.fragment.app.commit
import com.example.firebaseexample.presentation.notes.NotesFragment

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
        authViewModel.checkEmailVerified()
    }

    private fun setupUI() {
        binding.signOutButton.setOnClickListener {
            authViewModel.onEvent(AuthEvent.SignOut)
        }
        binding.sendVerificationButton.setOnClickListener {
            authViewModel.onEvent(AuthEvent.SendEmailVerification)
        }
        binding.buttonMyNotes.setOnClickListener {
            navigateToNotes()
        }
    }

    private fun observeAuthState() {
        viewLifecycleOwner.lifecycleScope.launch {
            authViewModel.state.collectLatest { state ->
                updateUI(state)
            }
        }
    }

    private fun updateUI(state: AuthState) {
        binding.progressIndicator.visibility = if (state.isLoading) View.VISIBLE else View.GONE
        state.user?.let { user ->
            binding.userEmailTextView.text = user.email
            binding.userIdTextView.text = getString(R.string.user_id, user.id)
            Timber.d("User info displayed: ${user.email}")
        }
        if (state.user != null) {
            state.isEmailVerified?.let { isEmailVerified ->
                binding.sendVerificationButton.isVisible = !isEmailVerified
                binding.verifiedTextView.isVisible = isEmailVerified
            }
        }
        state.verificationMessage?.let {
            Snackbar.make(binding.root, getString(R.string.verification_email_sent), Snackbar.LENGTH_LONG).show()
            authViewModel.onEvent(AuthEvent.ClearSuccessMessage)
        }
    }

    private fun navigateToNotes() {
        parentFragmentManager.commit {
            replace(R.id.fragmentContainer, NotesFragment())
            addToBackStack(null)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 