package com.example.shopsphere.CleanArchitecture.ui.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.shopsphere.CleanArchitecture.ui.viewmodels.AuthUiState
import com.example.shopsphere.CleanArchitecture.ui.viewmodels.RegisterViewModel
import com.example.shopsphere.R
import com.example.shopsphere.databinding.FragmentRegisterBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RegisterViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
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

        binding.tvLogin.setOnClickListener {
            findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
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
            viewModel.state.collect { state ->
                when (state) {
                    is AuthUiState.Idle -> showIdleState()
                    is AuthUiState.Loading -> showLoadingState()
                    is AuthUiState.Success -> navigateToHome()
                    is AuthUiState.Error -> showErrorState(state.error)
                }
            }
        }
    }

    private fun showIdleState() {
        binding.btnCreateAccount.isEnabled = true
        binding.btnCreateAccount.text = "Create Account"
        binding.progressBar.visibility = View.GONE
    }

    private fun showLoadingState() {
        binding.btnCreateAccount.isEnabled = false
        binding.btnCreateAccount.text = "Creating Account..."
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
        else -> error
    }

    private fun navigateToHome() {
        findNavController().navigate(RegisterFragmentDirections.actionRegisterFragmentToHomeFragment())
    }

    private fun toast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
