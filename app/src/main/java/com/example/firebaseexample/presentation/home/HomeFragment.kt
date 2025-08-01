package com.example.firebaseexample.presentation.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.firebaseexample.R
import com.example.firebaseexample.databinding.FragmentHomeBinding
import com.example.firebaseexample.presentation.auth.AuthEvent
import com.example.firebaseexample.presentation.auth.AuthViewModel
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
    }

    private fun observeAuthState() {
        viewLifecycleOwner.lifecycleScope.launch {
            authViewModel.state.collectLatest { state ->
                updateUI(state)
            }
        }
    }

    private fun updateUI(state: com.example.firebaseexample.presentation.auth.AuthState) {
        state.user?.let { user ->
            binding.userEmailTextView.text = user.email
            binding.userIdTextView.text = getString(R.string.user_id, user.id)
            
            Timber.d("User info displayed: ${user.email}")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 