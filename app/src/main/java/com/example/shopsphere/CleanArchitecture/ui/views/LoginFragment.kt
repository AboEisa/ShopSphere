package com.example.shopsphere.CleanArchitecture.ui.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.shopsphere.CleanArchitecture.ui.viewmodels.LoginUiEvent
import com.example.shopsphere.CleanArchitecture.ui.viewmodels.LoginUiState
import com.example.shopsphere.CleanArchitecture.ui.viewmodels.LoginViewModel
import com.example.shopsphere.databinding.FragmentLoginBinding
import com.example.shopsphere.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private lateinit var credentialManager: CredentialManager
    private val viewModel: LoginViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        credentialManager = CredentialManager.create(requireContext())
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

        binding.tvJoin.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }

        binding.tvForgotPassword.setOnClickListener {
            Toast.makeText(requireContext(), "Forgot password clicked", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeState() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is LoginUiState.Loading -> setLoading(true)
                    is LoginUiState.Success -> {
                        setLoading(false)
                        findNavController().navigate(
                            R.id.action_loginFragment_to_homeFragment,
                            null,
                            androidx.navigation.navOptions {
                                popUpTo(R.id.loginFragment) {
                                    inclusive = true
                                }
                            }
                        )
                    }
                    is LoginUiState.Error -> {
                        setLoading(false)
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                    }
                    else -> setLoading(false)
                }
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.btnLogin.isEnabled = !isLoading
        binding.btnLoginGoogle.isEnabled = !isLoading
    }

    private fun signInWithGoogle() {
        lifecycleScope.launch {
            try {
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(getString(R.string.default_web_client_id))
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(requireActivity(), request)
                val credential = result.credential

                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdTokenCredential =
                        GoogleIdTokenCredential.createFrom(credential.data)

                    viewModel.onEvent(
                        LoginUiEvent.GoogleToken(googleIdTokenCredential.idToken)
                    )
                } else {
                    Toast.makeText(requireContext(), "Invalid Google credential", Toast.LENGTH_SHORT).show()
                }


            } catch (e: GetCredentialException) {
                Toast.makeText(requireContext(), e.message ?: "Google sign-in failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
