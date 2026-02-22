package com.example.shopsphere.CleanArchitecture.ui.views

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.credentials.CustomCredential
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.NoCredentialException
import androidx.credentials.exceptions.GetCredentialException
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.example.shopsphere.CleanArchitecture.ui.viewmodels.LoginUiEvent
import com.example.shopsphere.CleanArchitecture.ui.viewmodels.LoginUiState
import com.example.shopsphere.CleanArchitecture.ui.viewmodels.LoginViewModel
import com.example.shopsphere.databinding.FragmentLoginBinding
import com.example.shopsphere.R
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
class LoginFragment : Fragment() {

    companion object {
        private const val TAG = "LoginFragment"
    }

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private lateinit var credentialManager: CredentialManager
    private lateinit var callbackManager: CallbackManager
    private val viewModel: LoginViewModel by viewModels()
    private var googleSignInJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        credentialManager = CredentialManager.create(requireContext())
        callbackManager = CallbackManager.Factory.create()
        registerFacebookCallback()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupListeners()
        observeState()
    }

    private fun setupListeners() {
        binding.etEmail.addTextChangedListener {
            viewModel.onEvent(LoginUiEvent.EmailChanged(it.toString()))
        }

        binding.etPassword.addTextChangedListener {
            viewModel.onEvent(LoginUiEvent.PasswordChanged(it.toString()))
        }

        binding.btnLogin.setOnClickListener {
            viewModel.onEvent(LoginUiEvent.LoginClicked)
        }

        binding.btnLoginGoogle.setOnClickListener {
            signInWithGoogle()
        }

        binding.btnLoginFacebook.setOnClickListener {
            signInWithFacebook()
        }

        binding.tvJoin.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }

        binding.tvForgotPassword.setOnClickListener {
            Toast.makeText(requireContext(), "Forgot password clicked", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is LoginUiState.Loading -> setLoading(true)
                        is LoginUiState.Success -> {
                            setLoading(false)
                            navigateToHomeSafely()
                            viewModel.consumeTransientState()
                        }
                        is LoginUiState.Error -> {
                            setLoading(false)
                            showToastSafely(mapGoogleAuthResultError(state.message))
                            viewModel.consumeTransientState()
                        }
                        else -> setLoading(false)
                    }
                }
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        _binding?.let { safeBinding ->
            safeBinding.btnLogin.isEnabled = !isLoading
            safeBinding.btnLoginGoogle.isEnabled = !isLoading
            safeBinding.btnLoginFacebook.isEnabled = !isLoading
        }
    }

    private fun signInWithGoogle() {
        if (googleSignInJob?.isActive == true) return

        googleSignInJob = viewLifecycleOwner.lifecycleScope.launch {
            setLoading(true)
            try {
                val webClientId = resolveGoogleWebClientId()
                if (webClientId.isBlank()) {
                    showToastSafely(getString(R.string.google_sign_in_failed))
                    return@launch
                }

                val credential = tryGetGoogleCredential(webClientId)
                val googleIdTokenCredential = extractGoogleIdTokenCredential(credential)

                if (googleIdTokenCredential == null) {
                    showToastSafely(getString(R.string.invalid_google_credential))
                    return@launch
                }

                viewModel.onEvent(LoginUiEvent.GoogleToken(googleIdTokenCredential.idToken))
            } catch (e: GetCredentialException) {
                Log.e(TAG, "Google sign-in failed", e)
                showToastSafely(mapGoogleSignInError(e))
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected sign-in error", e)
                showToastSafely(getString(R.string.google_sign_in_failed))
            } finally {
                if (!isDetached) {
                    setLoading(false)
                }
            }
        }
    }

    private fun signInWithFacebook() {
        if (!isAdded) return
        setLoading(true)
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
                    setLoading(false)
                    showToastSafely(getString(R.string.facebook_sign_in_cancelled))
                }

                override fun onError(error: FacebookException) {
                    Log.e(TAG, "Facebook sign-in failed", error)
                    setLoading(false)
                    showToastSafely(getString(R.string.facebook_sign_in_failed))
                }

                override fun onSuccess(result: LoginResult) {
                    viewModel.onEvent(LoginUiEvent.FacebookToken(result.accessToken.token))
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

    private fun mapGoogleAuthResultError(message: String): String {
        val lower = message.lowercase()
        return when {
            lower.contains("error_api_not_available") ||
                lower.contains("api key not valid") ||
                lower.contains("api key") && lower.contains("restricted") ||
                lower.contains("identity toolkit") ||
                lower.contains("configuration_not_found") ||
                lower.contains("operation_not_allowed") ->
                getString(R.string.google_sign_in_api_key_restricted)

            lower.contains("error_internal_error") ||
                lower.contains("an internal error has occurred") ->
                getString(R.string.google_sign_in_console_not_configured)

            lower.contains("facebook") && lower.contains("cancel") ->
                getString(R.string.facebook_sign_in_cancelled)

            lower.contains("facebook") ->
                getString(R.string.facebook_sign_in_failed)

            else -> message
        }
    }

    private fun navigateToHomeSafely() {
        if (!isAdded) return
        val navController = findNavController()
        if (navController.currentDestination?.id != R.id.loginFragment) return

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

    private fun showToastSafely(message: String) {
        if (!isAdded) return
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
