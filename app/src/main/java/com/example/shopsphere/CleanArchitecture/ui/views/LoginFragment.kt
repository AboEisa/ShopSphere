package com.example.shopsphere.CleanArchitecture.ui.views

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.activity.result.contract.ActivityResultContracts
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
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LoginFragment : Fragment() {
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val viewModel: LoginViewModel by viewModels()

    // Facebook callback manager
    private lateinit var callbackManager: CallbackManager

    private val googleSignInLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                try {
                    val account = task.getResult(ApiException::class.java)
                    val idToken = account?.idToken
                    if (idToken != null) {
                        viewModel.loginWithGoogle(idToken)
                    }
                } catch (e: ApiException) {
                    Toast.makeText(
                        requireContext(),
                        "Google sign-in failed: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

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

        // Initialize Facebook callback manager
        callbackManager = CallbackManager.Factory.create()

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

        // Google Sign-In
        binding.btnLoginGoogle.setOnClickListener {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()

            val client = GoogleSignIn.getClient(requireActivity(), gso)
            googleSignInLauncher.launch(client.signInIntent)
        }

        // Facebook Sign-In
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
                        // Navigate to home screen
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
        // Pass the activity result back to the Facebook SDK
        callbackManager.onActivityResult(requestCode, resultCode, data)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}