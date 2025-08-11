package com.example.firebaseexample

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import com.example.firebaseexample.databinding.ActivityMainBinding
import com.example.firebaseexample.presentation.auth.AuthFragment
import com.example.firebaseexample.presentation.auth.AuthViewModel
import com.example.firebaseexample.presentation.home.HomeFragment
import com.example.firebaseexample.presentation.splash.SplashViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val authViewModel: AuthViewModel by viewModels()
    private val splashViewModel: SplashViewModel by viewModels()
    private lateinit var firebaseAnalytics: FirebaseAnalytics
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupSplashScreen(splashScreen)
        observeSplashState()
        firebaseAnalytics = FirebaseAnalytics.getInstance(this)
    }

    private fun setupSplashScreen(splashScreen: androidx.core.splashscreen.SplashScreen) {
        splashScreen.setKeepOnScreenCondition {
            val splashState = splashViewModel.state.value
            splashState.isLoading || splashState.isForceUpdateRequired || splashState.isMaintenanceMode
        }
    }

    private fun observeSplashState() {
        lifecycleScope.launch {
            splashViewModel.state.collectLatest { state ->
                when {
                    state.isLoading -> {
                        Timber.d("Splash screen loading...")
                    }
                    state.isForceUpdateRequired -> {
                        showForceUpdateDialog()
                    }
                    state.isMaintenanceMode -> {
                        showMaintenanceModeDialog()
                    }
                    state.canProceed -> {
                        initializeApp()
                    }
                    state.error != null -> {
                        Timber.e("Splash error: ${state.error}")
                        initializeApp() // Proceed on error
                    }
                }
            }
        }
    }

    private fun initializeApp() {
        observeAuthState()
    }

    private fun observeAuthState() {
        lifecycleScope.launch {
            authViewModel.state.collectLatest { state ->
                if (state.isInitialized) {
                    if (state.user != null) {
                        showHomeFragment()
                    } else {
                        showAuthFragment()
                    }
                }
            }
        }
    }

    private fun showAuthFragment() {
        supportFragmentManager.commit {
            replace(R.id.fragmentContainer, AuthFragment())
        }
    }

    private fun showHomeFragment() {
        supportFragmentManager.commit {
            replace(R.id.fragmentContainer, HomeFragment())
        }
    }

    private fun showForceUpdateDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.force_update_title))
            .setMessage(getString(R.string.force_update_message))
            .setPositiveButton(getString(R.string.ok)) { _, _ ->
                finishAffinity()
                firebaseAnalytics.logEvent("force_update_dialog_clicked", null)
            }
            .setCancelable(false)
            .show()
    }

    private fun showMaintenanceModeDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.maintenance_mode_title))
            .setMessage(getString(R.string.maintenance_mode_message))
            .setPositiveButton(getString(R.string.ok)) { _, _ ->
                finishAffinity()
                firebaseAnalytics.logEvent("maintenance_mode_dialog_clicked", Bundle().apply {
                    putString("maintenance_mode", "true")
                })
            }
            .setCancelable(false)
            .show()
    }
}