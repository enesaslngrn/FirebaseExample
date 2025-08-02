package com.example.firebaseexample.presentation.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.credentials.Credential
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
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.CustomCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GetGoogleIdOption

@AndroidEntryPoint
class AuthFragment : Fragment() {

    private var _binding: FragmentAuthBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels()
    private lateinit var credentialManager: CredentialManager

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
        setupCredentialManager()
        setupUI()
        observeState()
    }

    private fun setupCredentialManager() {
        credentialManager = CredentialManager.create(requireContext())
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

        binding.forgotPasswordButton.setOnClickListener {
            val email = binding.emailEditText.text.toString()
            
            if (validateEmail(email)) {
                viewModel.onEvent(AuthEvent.SendPasswordReset(email))
            }
        }

        binding.googleSignInButton.setOnClickListener {
            launchGoogleSignIn()
        }
    }

    private fun launchGoogleSignIn() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setServerClientId(getString(R.string.default_web_client_id))
                    .setFilterByAuthorizedAccounts(false) // “Sadece daha önce bu uygulamaya giriş yapmış kullanıcıların Google hesaplarını göster.”
                    .setAutoSelectEnabled(true)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val credentialResponse = credentialManager.getCredential(
                    context = requireContext(),
                    request = request
                )
                handleSignIn(credentialResponse.credential)

            } catch (e: GetCredentialException) {
                Timber.e(e, "GetCredentialException")
                showError(getString(R.string.google_sign_in_failed))
            } catch (e: Exception) {
                Timber.e(e, "Failed to launch Google sign in")
                showError(getString(R.string.google_sign_in_failed))
            }
        }
    }

    private fun handleSignIn(credential: Credential) {
        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            viewModel.onEvent(AuthEvent.SignInWithGoogle(googleIdTokenCredential.idToken))
        } else {
            Timber.w("Credential is not of type Google ID!")
            showError(getString(R.string.google_sign_in_failed))
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

    private fun validateEmail(email: String): Boolean {
        if (email.isEmpty()) {
            showError(getString(R.string.please_enter_email))
            return false
        }
        
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError(getString(R.string.invalid_email))
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
        binding.forgotPasswordButton.isEnabled = !state.isLoading
        binding.googleSignInButton.isEnabled = !state.isLoading
        binding.emailEditText.isEnabled = !state.isLoading
        binding.passwordEditText.isEnabled = !state.isLoading

        state.error?.let { error ->
            showError(error)
            viewModel.onEvent(AuthEvent.ClearError)
        }

        state.successMessage?.let { message ->
            showSuccess(message)
            viewModel.onEvent(AuthEvent.ClearSuccessMessage)
        }
    }

    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    private fun showSuccess(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 