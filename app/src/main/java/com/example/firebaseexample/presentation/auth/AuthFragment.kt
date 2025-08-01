package com.example.firebaseexample.presentation.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.firebaseexample.R
import com.example.firebaseexample.databinding.FragmentAuthBinding
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class AuthFragment : Fragment() {

    private var _binding: FragmentAuthBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAuthBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        observeState()
    }

    private fun setupUI() {
        binding.signInButton.setOnClickListener {
            val email = binding.emailEditText.text.toString()
            val password = binding.passwordEditText.text.toString()
            
            if (validateInput(email, password)) {
                viewModel.onEvent(AuthEvent.SignIn(email, password))
            }
        }

        binding.signUpButton.setOnClickListener {
            val email = binding.emailEditText.text.toString()
            val password = binding.passwordEditText.text.toString()
            
            if (validateInput(email, password)) {
                viewModel.onEvent(AuthEvent.SignUp(email, password))
            }
        }
    }

    private fun validateInput(email: String, password: String): Boolean {
        if (email.isEmpty()) {
            showError(getString(R.string.please_enter_email))
            return false
        }
        
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError(getString(R.string.invalid_email))
            return false
        }
        
        if (password.isEmpty()) {
            showError(getString(R.string.please_enter_password))
            return false
        }
        
        if (password.length < 6) {
            showError(getString(R.string.password_too_short))
            return false
        }
        
        return true
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.state.collectLatest { state ->
                updateUI(state)
            }
        }
    }

    private fun updateUI(state: AuthState) {
        binding.progressIndicator.visibility = if (state.isLoading) View.VISIBLE else View.GONE
        
        binding.signInButton.isEnabled = !state.isLoading
        binding.signUpButton.isEnabled = !state.isLoading
        binding.emailEditText.isEnabled = !state.isLoading
        binding.passwordEditText.isEnabled = !state.isLoading

        state.error?.let { error ->
            showError(error)
            viewModel.onEvent(AuthEvent.ClearError)
        }

        if (state.isAuthenticated) {
            Timber.d("User authenticated: ${state.user?.email}")
            // Navigation will be handled by the activity
        }
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 