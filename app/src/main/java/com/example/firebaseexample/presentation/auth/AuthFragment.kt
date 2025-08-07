package com.example.firebaseexample.presentation.auth

import android.annotation.SuppressLint
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
import androidx.core.content.ContextCompat

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

        // Phone Auth UI Setup
        binding.toggleAuthModeButton.setOnClickListener {
            viewModel.onEvent(AuthEvent.TogglePhoneAuthMode)
        }

        binding.verifyPhoneButton.setOnClickListener {
            val phoneNumber = binding.phoneEditText.text.toString().trim()
            if (validatePhoneNumber(phoneNumber)) {
                Timber.d("Attempting to verify phone number: $phoneNumber")
                
                // Log for debugging Firebase test numbers
                if (phoneNumber == "+905537414070") {
                    Timber.d("Using Firebase test phone number - manual OTP flow will be used")
                    Timber.d("Expected verification code: 123456")
                }
                
                viewModel.verifyPhoneNumberWithActivity(phoneNumber, requireActivity())
            }
        }

        binding.verifyCodeButton.setOnClickListener {
            val verificationCode = binding.verificationCodeEditText.text.toString().trim()
            
            // Check if code is expired
            if (viewModel.state.value.isCodeExpired) {
                showError("Verification code has expired. Please request a new code.")
                return@setOnClickListener
            }
            
            if (validateVerificationCode(verificationCode)) {
                viewModel.onEvent(AuthEvent.VerifySmsCode(verificationCode))
            }
        }

        binding.resendCodeButton.setOnClickListener {
            viewModel.resendVerificationCodeWithActivity(requireActivity())
        }
    }

    private fun launchGoogleSignIn() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setServerClientId(getString(R.string.default_web_client_id))
                    .setFilterByAuthorizedAccounts(false)
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

    private fun validatePhoneNumber(phoneNumber: String): Boolean {
        if (phoneNumber.isEmpty()) {
            showError(getString(R.string.please_enter_phone_number))
            return false
        }
        
        // Check if it starts with + and has reasonable length
        if (!phoneNumber.startsWith("+")) {
            showError(getString(R.string.invalid_phone_number))
            return false
        }
        
        // Remove + and spaces for length check
        val digitsOnly = phoneNumber.replace("+", "").replace(" ", "").replace("-", "")
        
        // Phone number should have at least 10 digits (excluding country code)
        if (digitsOnly.length < 10 || digitsOnly.length > 15) {
            showError(getString(R.string.invalid_phone_number))
            return false
        }
        
        // Check if all characters after + are digits, spaces, or hyphens
        if (!phoneNumber.substring(1).matches(Regex("[0-9\\s-]+"))) {
            showError(getString(R.string.invalid_phone_number))
            return false
        }
        
        return true
    }

    private fun validateVerificationCode(verificationCode: String): Boolean {
        if (verificationCode.isEmpty()) {
            showError(getString(R.string.please_enter_verification_code))
            return false
        }
        
        if (verificationCode.length != 6) {
            showError(getString(R.string.invalid_verification_code))
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

    @SuppressLint("DefaultLocale")
    private fun updateUI(state: AuthState) {
        binding.progressIndicator.visibility = if (state.isLoading || state.isVerifyingCode) View.VISIBLE else View.GONE
        
        // Toggle between email and phone auth modes with proper constraint management
        if (state.isPhoneAuthMode) {
            // Show phone auth, hide email auth
            binding.emailAuthSection.visibility = View.GONE
            binding.phoneAuthSection.visibility = View.VISIBLE
            binding.phoneAuthSpacer.visibility = View.VISIBLE
            
            // Update toggle button constraint to phoneAuthSpacer
            val toggleParams = binding.toggleAuthModeButton.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
            toggleParams.topToBottom = binding.phoneAuthSpacer.id
            binding.toggleAuthModeButton.layoutParams = toggleParams
            
            // Hide Google sign in and divider in phone mode
            binding.googleSignInButton.visibility = View.GONE
            binding.orDivider.visibility = View.GONE
            
            // Phone auth UI state management
            binding.verificationCodeLayout.visibility = if (state.isCodeSent) View.VISIBLE else View.GONE
            binding.verifyCodeButton.visibility = if (state.isCodeSent) View.VISIBLE else View.GONE
            binding.resendCodeButton.visibility = if (state.isCodeSent) View.VISIBLE else View.GONE
            binding.timerTextView.visibility = if (state.isCodeSent && state.isTimerActive) View.VISIBLE else View.GONE
            
            // Update timer display
            if (state.isTimerActive && state.remainingTime > 0) {
                val minutes = state.remainingTime / 60
                val seconds = state.remainingTime % 60
                binding.timerTextView.text = String.format("%02d:%02d", minutes, seconds)
                binding.timerTextView.setTextColor(
                    if (state.remainingTime <= 10) {
                        ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark)
                    } else {
                        ContextCompat.getColor(requireContext(), android.R.color.darker_gray)
                    }
                )
            }
            
            // Phone auth button states
            binding.verifyPhoneButton.isEnabled = !state.isLoading && !state.isVerifyingCode && !state.isCodeSent
            binding.verifyCodeButton.isEnabled = !state.isVerifyingCode && !state.isCodeExpired
            binding.resendCodeButton.isEnabled = !state.isLoading && (state.isCodeExpired || !state.isTimerActive)
            binding.phoneEditText.isEnabled = !state.isLoading && !state.isVerifyingCode
            binding.verificationCodeEditText.isEnabled = !state.isVerifyingCode && !state.isCodeExpired
            
        } else {
            // Show email auth, hide phone auth
            binding.emailAuthSection.visibility = View.VISIBLE
            binding.phoneAuthSection.visibility = View.GONE
            binding.phoneAuthSpacer.visibility = View.GONE
            
            // Update toggle button constraint back to emailAuthSection
            val toggleParams = binding.toggleAuthModeButton.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
            toggleParams.topToBottom = binding.emailAuthSection.id
            binding.toggleAuthModeButton.layoutParams = toggleParams
            
            // Show Google sign in and divider in email mode
            binding.googleSignInButton.visibility = View.VISIBLE
            binding.orDivider.visibility = View.VISIBLE
            
            // Email auth button states
            binding.signInButton.isEnabled = !state.isLoading
            binding.signUpButton.isEnabled = !state.isLoading
            binding.forgotPasswordButton.isEnabled = !state.isLoading
            binding.emailEditText.isEnabled = !state.isLoading
            binding.passwordEditText.isEnabled = !state.isLoading
        }
        
        // Update toggle button text
        binding.toggleAuthModeButton.text = if (state.isPhoneAuthMode) {
            getString(R.string.use_email)
        } else {
            getString(R.string.use_phone)
        }
        
        // Toggle button is always enabled unless loading
        binding.toggleAuthModeButton.isEnabled = !state.isLoading && !state.isVerifyingCode

        // Handle messages
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