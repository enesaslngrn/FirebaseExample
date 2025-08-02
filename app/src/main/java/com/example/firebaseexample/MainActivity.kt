package com.example.firebaseexample

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.firebaseexample.presentation.auth.AuthFragment
import com.example.firebaseexample.presentation.auth.AuthState
import com.example.firebaseexample.presentation.auth.AuthViewModel
import com.example.firebaseexample.presentation.home.HomeFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    
    private val authViewModel: AuthViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        observeAuthState()
    }
    
    private fun observeAuthState() {
        lifecycleScope.launch {
            authViewModel.state.collectLatest { state ->
                handleAuthState(state)
            }
        }
    }
    
    private fun handleAuthState(state: AuthState) {
        if (!state.isInitialized) {
            return
        }
        state.user?.let {
            Timber.d("User is authenticated, showing home fragment")
            showHomeFragment()
        } ?: run {
            Timber.d("User is not authenticated, showing auth fragment")
            showAuthFragment()
        }
    }
    
    private fun showHomeFragment() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, HomeFragment())
            .commit()
    }
    
    private fun showAuthFragment() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, AuthFragment())
            .commit()
    }
}