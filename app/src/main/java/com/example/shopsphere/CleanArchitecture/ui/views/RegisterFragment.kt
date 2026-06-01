package com.example.shopsphere.CleanArchitecture.ui.views

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.shopsphere.CleanArchitecture.ui.viewmodels.AuthUiState
import com.example.shopsphere.CleanArchitecture.ui.viewmodels.RegisterViewModel
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
        binding.etFirstName.doAfterTextChanged { text ->
            binding.ivFirstNameCheck.visibility =
                if (!text.isNullOrEmpty() && text.length >= 2) View.VISIBLE else View.GONE
        }

        binding.etLastName.doAfterTextChanged { text ->
            binding.ivLastNameCheck.visibility =
                if (!text.isNullOrEmpty() && text.length >= 2) View.VISIBLE else View.GONE
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
            val firstName = binding.etFirstName.text.toString().trim()
            val lastName = binding.etLastName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()
            val confirmPassword = binding.etConfirmPassword.text.toString()

            if (!validateInputs(firstName, lastName, email, password, confirmPassword)) return@setOnClickListener

            viewModel.register(firstName, lastName, email, password)
        }

        binding.tvLogin.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun validateInputs(
        firstName: String,
        lastName: String,
        email: String,
        password: String,
        confirmPassword: String
    ): Boolean {
        return when {
            firstName.isEmpty() -> {
                toast("Please enter your first name"); false
            }
            firstName.length < 2 -> {
                toast("First name must be at least 2 characters"); false
            }
            lastName.isEmpty() -> {
                toast("Please enter your last name"); false
            }
            lastName.length < 2 -> {
                toast("Last name must be at least 2 characters"); false
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
        binding.tvLogin.isEnabled = true
        binding.progressBar.visibility = View.GONE
    }

    private fun showLoadingState() {
        binding.btnCreateAccount.isEnabled = false
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
        else -> error
    }

    private fun navigateToHome() {
        if (!isAdded || _binding == null) return
        val intent = Intent(requireContext(), MainActivity::class.java)
        intent.putExtra(MainActivity.EXTRA_OPEN_HOME, true)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    private fun toast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
