package com.example.shopsphere.CleanArchitecture.ui.views

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.shopsphere.CleanArchitecture.ui.viewmodels.AuthUiState
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
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LoginFragment : Fragment() {
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val viewModel: LoginViewModel by viewModels()

    // Facebook callback manager
    private lateinit var callbackManager: CallbackManager

    // Credential Manager for Google Sign-In
    private lateinit var credentialManager: CredentialManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize managers
        callbackManager = CallbackManager.Factory.create()
        credentialManager = CredentialManager.create(requireContext())

        setupClickListeners()
        observeAuthState()
    }

    private fun setupClickListeners() {
        // Email/Password Login
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val pass = binding.etPassword.text.toString()

            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(
                    requireContext(),
                    "Fill all fields",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            viewModel.login(email, pass)
        }

        // Google Sign-In with Credential Manager
        binding.btnLoginGoogle.setOnClickListener {
            lifecycleScope.launch {
                try {
                    val googleIdOption = GetGoogleIdOption.Builder()
                        .setFilterByAuthorizedAccounts(false)
                        .setServerClientId(getString(R.string.default_web_client_id))
                        .build()

                    val request = GetCredentialRequest.Builder()
                        .addCredentialOption(googleIdOption)
                        .build()

                    val result = credentialManager.getCredential(
                        request = request,
                        context = requireActivity()
                    )

                    val credential = result.credential
                    if (credential is GoogleIdTokenCredential) {
                        val idToken = credential.idToken
                        // ✅ Same call as before!
                        viewModel.loginWithGoogle(idToken)
                    }
                } catch (e: GetCredentialException) {
                    Toast.makeText(
                        requireContext(),
                        "Google sign-in failed: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        // Facebook Sign-In (No changes)
        binding.btnLoginFacebook.setOnClickListener {
            LoginManager.getInstance().logInWithReadPermissions(
                this,
                listOf("email", "public_profile")
            )

            LoginManager.getInstance().registerCallback(
                callbackManager,
                object : FacebookCallback<LoginResult> {
                    override fun onSuccess(result: LoginResult) {
                        val accessToken = result.accessToken.token
                        viewModel.loginWithFacebook(accessToken)
                    }

                    override fun onCancel() {
                        Toast.makeText(
                            requireContext(),
                            "Facebook login cancelled",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    override fun onError(error: FacebookException) {
                        Toast.makeText(
                            requireContext(),
                            "Facebook login failed: ${error.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            )
        }

        // Navigate to Sign Up
        binding.tvJoin.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_signupFragment)
        }
    }

    private fun observeAuthState() {
        lifecycleScope.launch {
            viewModel.state.collect { state ->
                when (state) {
                    is AuthUiState.Loading -> {
                        setLoadingState(true)
                    }
                    is AuthUiState.Success -> {
                        setLoadingState(false)
                        Toast.makeText(
                            requireContext(),
                            state.msg,
                            Toast.LENGTH_SHORT
                        ).show()
                        findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
                    }
                    is AuthUiState.Error -> {
                        setLoadingState(false)
                        Toast.makeText(
                            requireContext(),
                            state.error ?: "Authentication error",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    else -> {
                        setLoadingState(false)
                    }
                }
            }
        }
    }

    private fun setLoadingState(isLoading: Boolean) {
        binding.btnLogin.isEnabled = !isLoading
        binding.btnLoginGoogle.isEnabled = !isLoading
        binding.btnLoginFacebook.isEnabled = !isLoading
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        callbackManager.onActivityResult(requestCode, resultCode, data)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}