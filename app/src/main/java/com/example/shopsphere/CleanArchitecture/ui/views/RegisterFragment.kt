package com.example.shopsphere.CleanArchitecture.ui.views

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.credentials.CustomCredential
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.example.shopsphere.CleanArchitecture.ui.viewmodels.AuthUiState
import com.example.shopsphere.CleanArchitecture.ui.viewmodels.RegisterViewModel
import com.example.shopsphere.R
import com.example.shopsphere.databinding.FragmentRegisterBinding
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RegisterFragment : Fragment() {

    companion object {
        private const val TAG = "RegisterFragment"
    }

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private lateinit var credentialManager: CredentialManager
    private lateinit var callbackManager: CallbackManager
    private val viewModel: RegisterViewModel by viewModels()
    private var googleSignInJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        credentialManager = CredentialManager.create(requireContext())
        callbackManager = CallbackManager.Factory.create()
        registerFacebookCallback()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupValidation()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupValidation() {
        binding.etFullName.doAfterTextChanged { text ->
            binding.ivFullNameCheck.visibility =
                if (!text.isNullOrEmpty() && text.length >= 3) View.VISIBLE else View.GONE
        }

        binding.etEmail.doAfterTextChanged { text ->
            binding.ivEmailCheck.visibility =
                if (!text.isNullOrEmpty() && android.util.Patterns.EMAIL_ADDRESS.matcher(text).matches())
                    View.VISIBLE else View.GONE
        }

        binding.etPassword.doAfterTextChanged { text ->
            binding.ivPasswordCheck.visibility =
                if (!text.isNullOrEmpty() && text.length >= 6) View.VISIBLE else View.GONE
        }

        binding.etConfirmPassword.doAfterTextChanged { text ->
            val password = binding.etPassword.text.toString()
            binding.ivConfirmPasswordCheck.visibility =
                if (!text.isNullOrEmpty() && text.toString() == password) View.VISIBLE else View.GONE
        }
    }

    private fun setupClickListeners() {
        binding.btnCreateAccount.setOnClickListener {
            val name = binding.etFullName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()
            val confirmPassword = binding.etConfirmPassword.text.toString()

            if (!validateInputs(name, email, password, confirmPassword)) return@setOnClickListener

            viewModel.register(name, email, password)
        }

        binding.btnRegisterGoogle.setOnClickListener {
            signInWithGoogle()
        }

        binding.btnRegisterFacebook.setOnClickListener {
            signInWithFacebook()
        }

        binding.tvLogin.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun validateInputs(
        name: String,
        email: String,
        password: String,
        confirmPassword: String
    ): Boolean {
        return when {
            name.isEmpty() -> {
                toast("Please enter your full name"); false
            }
            name.length < 3 -> {
                toast("Name must be at least 3 characters"); false
            }
            email.isEmpty() -> {
                toast("Please enter your email"); false
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                toast("Please enter a valid email"); false
            }
            password.isEmpty() -> {
                toast("Please enter a password"); false
            }
            password.length < 6 -> {
                toast("Password must be at least 6 characters"); false
            }
            confirmPassword.isEmpty() -> {
                toast("Please confirm your password"); false
            }
            password != confirmPassword -> {
                toast("Passwords do not match"); false
            }
            else -> true
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    when (state) {
                        is AuthUiState.Idle -> showIdleState()
                        is AuthUiState.Loading -> showLoadingState()
                        is AuthUiState.Success -> {
                            showIdleState()
                            navigateToHome()
                            viewModel.consumeTransientState()
                        }
                        is AuthUiState.Error -> {
                            showErrorState(state.error)
                            viewModel.consumeTransientState()
                        }
                    }
                }
            }
        }
    }

    private fun showIdleState() {
        binding.btnCreateAccount.isEnabled = true
        binding.btnCreateAccount.text = "Create Account"
        binding.btnRegisterGoogle.isEnabled = true
        binding.btnRegisterFacebook.isEnabled = true
        binding.tvLogin.isEnabled = true
        binding.progressBar.visibility = View.GONE
    }

    private fun showLoadingState() {
        binding.btnCreateAccount.isEnabled = false
        binding.btnRegisterGoogle.isEnabled = false
        binding.btnRegisterFacebook.isEnabled = false
        binding.tvLogin.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE
    }

    private fun showErrorState(error: String?) {
        showIdleState()
        toast(mapErrorMessage(error))
    }

    private fun mapErrorMessage(error: String?): String = when {
        error.isNullOrEmpty() -> "Registration failed"
        error.contains("already in use", ignoreCase = true) -> "This email is already registered"
        error.contains("invalid-email", ignoreCase = true) -> "Invalid email address"
        error.contains("weak-password", ignoreCase = true) -> "Password is too weak"
        error.contains("network", ignoreCase = true) -> "Network error. Please check your connection"
        error.contains("facebook", ignoreCase = true) -> getString(R.string.facebook_sign_in_failed)
        error.contains("google", ignoreCase = true) -> getString(R.string.google_sign_in_failed)
        else -> error
    }

    private fun signInWithGoogle() {
        if (googleSignInJob?.isActive == true) return
        googleSignInJob = viewLifecycleOwner.lifecycleScope.launch {
            showLoadingState()
            try {
                val webClientId = resolveGoogleWebClientId()
                if (webClientId.isBlank()) {
                    toast(getString(R.string.google_sign_in_failed))
                    return@launch
                }

                val credential = tryGetGoogleCredential(webClientId)
                val googleIdTokenCredential = extractGoogleIdTokenCredential(credential)
                if (googleIdTokenCredential == null) {
                    toast(getString(R.string.invalid_google_credential))
                    return@launch
                }

                viewModel.continueWithGoogle(googleIdTokenCredential.idToken)
            } catch (e: GetCredentialException) {
                Log.e(TAG, "Google sign-in failed", e)
                showIdleState()
                toast(mapGoogleSignInError(e))
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected Google sign-in error", e)
                showIdleState()
                toast(getString(R.string.google_sign_in_failed))
            }
        }
    }

    private fun signInWithFacebook() {
        if (!isAdded) return
        showLoadingState()
        LoginManager.getInstance().logInWithReadPermissions(
            this,
            listOf("email", "public_profile")
        )
    }

    private fun registerFacebookCallback() {
        LoginManager.getInstance().registerCallback(
            callbackManager,
            object : FacebookCallback<LoginResult> {
                override fun onCancel() {
                    showIdleState()
                    toast(getString(R.string.facebook_sign_in_cancelled))
                }

                override fun onError(error: FacebookException) {
                    Log.e(TAG, "Facebook sign-in failed", error)
                    showIdleState()
                    toast(getString(R.string.facebook_sign_in_failed))
                }

                override fun onSuccess(result: LoginResult) {
                    viewModel.continueWithFacebook(result.accessToken.token)
                }
            }
        )
    }

    private suspend fun tryGetGoogleCredential(webClientId: String): androidx.credentials.Credential {
        val authorizedRequest = GetCredentialRequest.Builder()
            .addCredentialOption(
                GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(true)
                    .setServerClientId(webClientId)
                    .build()
            )
            .build()

        return try {
            credentialManager.getCredential(requireActivity(), authorizedRequest).credential
        } catch (noCredential: NoCredentialException) {
            val allAccountsRequest = GetCredentialRequest.Builder()
                .addCredentialOption(
                    GetGoogleIdOption.Builder()
                        .setFilterByAuthorizedAccounts(false)
                        .setServerClientId(webClientId)
                        .build()
                )
                .build()
            credentialManager.getCredential(requireActivity(), allAccountsRequest).credential
        }
    }

    private fun extractGoogleIdTokenCredential(
        credential: androidx.credentials.Credential
    ): GoogleIdTokenCredential? {
        return when (credential) {
            is CustomCredential -> {
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    try {
                        GoogleIdTokenCredential.createFrom(credential.data)
                    } catch (e: GoogleIdTokenParsingException) {
                        Log.e(TAG, "Failed to parse Google ID token credential", e)
                        null
                    }
                } else {
                    null
                }
            }
            else -> null
        }
    }

    private fun mapGoogleSignInError(exception: GetCredentialException): String {
        val message = exception.message?.lowercase().orEmpty()
        return when {
            message.contains("developer console") ||
                message.contains("10:") ||
                message.contains("12500") -> getString(R.string.google_sign_in_console_not_configured)
            message.contains("canceled") ||
                message.contains("cancelled") -> getString(R.string.google_sign_in_cancelled)
            else -> exception.message ?: getString(R.string.google_sign_in_failed)
        }
    }

    private fun resolveGoogleWebClientId(): String {
        val resourceId = resources.getIdentifier(
            "default_web_client_id",
            "string",
            requireContext().packageName
        )
        return if (resourceId == 0) "" else getString(resourceId).trim()
    }

    private fun navigateToHome() {
        val navController = findNavController()
        if (navController.currentDestination?.id != R.id.registerFragment) return
        navController.navigate(
            R.id.homeFragment,
            null,
            navOptions {
                popUpTo(R.id.loginFragment) {
                    inclusive = true
                }
                launchSingleTop = true
            }
        )
    }

    private fun toast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        callbackManager.onActivityResult(requestCode, resultCode, data)
    }

    override fun onDestroyView() {
        googleSignInJob?.cancel()
        googleSignInJob = null
        LoginManager.getInstance().unregisterCallback(callbackManager)
        super.onDestroyView()
        _binding = null
    }
}
