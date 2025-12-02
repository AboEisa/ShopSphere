package com.example.shopsphere.CleanArchitecture.ui.views

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.shopsphere.CleanArchitecture.ui.viewmodels.RegisterViewModel
import com.example.shopsphere.CleanArchitecture.ui.viewmodels.AuthUiState
import com.example.shopsphere.R
import com.example.shopsphere.databinding.FragmentSignupBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RegisterFragment : Fragment() {

    private var _binding: FragmentSignupBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RegisterViewModel by viewModels()

    companion object {
        private const val TAG = "SignupFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d(TAG, "onCreateView called")
        _binding = FragmentSignupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated called")

        setupValidationChecks()
        setupClickListeners()
        observeState()
    }

    private fun setupValidationChecks() {
        Log.d(TAG, "Setting up validation checks")

        // Full Name Validation
        binding.etFullName.doAfterTextChanged { text ->
            if (!text.isNullOrEmpty() && text.length >= 3) {
                binding.ivFullNameCheck.visibility = View.VISIBLE
            } else {
                binding.ivFullNameCheck.visibility = View.GONE
            }
        }

        // Email Validation
        binding.etEmail.doAfterTextChanged { text ->
            if (!text.isNullOrEmpty() && android.util.Patterns.EMAIL_ADDRESS.matcher(text).matches()) {
                binding.ivEmailCheck.visibility = View.VISIBLE
            } else {
                binding.ivEmailCheck.visibility = View.GONE
            }
        }

        // Password Validation
        binding.etPassword.doAfterTextChanged { text ->
            if (!text.isNullOrEmpty() && text.length >= 6) {
                binding.ivPasswordCheck.visibility = View.VISIBLE
            } else {
                binding.ivPasswordCheck.visibility = View.GONE
            }
        }

        // Confirm Password Validation
        binding.etConfirmPassword.doAfterTextChanged { text ->
            val password = binding.etPassword.text.toString()
            if (!text.isNullOrEmpty() && text.toString() == password) {
                binding.ivConfirmPasswordCheck.visibility = View.VISIBLE
            } else {
                binding.ivConfirmPasswordCheck.visibility = View.GONE
            }
        }
    }

    private fun setupClickListeners() {
        Log.d(TAG, "Setting up click listeners")

        // Create Account Button
        binding.btnCreateAccount.setOnClickListener {
            Log.d(TAG, "Create Account button clicked!")

            val name = binding.etFullName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()
            val confirmPassword = binding.etConfirmPassword.text.toString()

            Log.d(TAG, "Name: $name, Email: $email, Password length: ${password.length}")

            // Validation
            when {
                name.isEmpty() -> {
                    Log.d(TAG, "Validation failed: Name is empty")
                    Toast.makeText(requireContext(), "Please enter your full name", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                name.length < 3 -> {
                    Log.d(TAG, "Validation failed: Name too short")
                    Toast.makeText(requireContext(), "Name must be at least 3 characters", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                email.isEmpty() -> {
                    Log.d(TAG, "Validation failed: Email is empty")
                    Toast.makeText(requireContext(), "Please enter your email", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                    Log.d(TAG, "Validation failed: Invalid email")
                    Toast.makeText(requireContext(), "Please enter a valid email", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                password.isEmpty() -> {
                    Log.d(TAG, "Validation failed: Password is empty")
                    Toast.makeText(requireContext(), "Please enter a password", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                password.length < 6 -> {
                    Log.d(TAG, "Validation failed: Password too short")
                    Toast.makeText(requireContext(), "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                confirmPassword.isEmpty() -> {
                    Log.d(TAG, "Validation failed: Confirm password is empty")
                    Toast.makeText(requireContext(), "Please confirm your password", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                password != confirmPassword -> {
                    Log.d(TAG, "Validation failed: Passwords don't match")
                    Toast.makeText(requireContext(), "Passwords don't match", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            // All validations passed - register
            Log.d(TAG, "All validations passed. Calling viewModel.register()")
            viewModel.register(name, email, password)
        }

        // Navigate to Login
        binding.tvLogin.setOnClickListener {
            Log.d(TAG, "Login text clicked")
            try {
                findNavController().navigateUp()
            } catch (e: Exception) {
                Log.e(TAG, "Navigation error: ${e.message}")
                Toast.makeText(requireContext(), "Navigation error", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun observeState() {
        Log.d(TAG, "Setting up state observer")

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.state.collect { state ->
                Log.d(TAG, "State changed: $state")

                when (state) {
                    is AuthUiState.Idle -> {
                        Log.d(TAG, "State: Idle")
                        binding.btnCreateAccount.isEnabled = true
                        binding.btnCreateAccount.text = "Create Account"
                    }

                    is AuthUiState.Loading -> {
                        Log.d(TAG, "State: Loading")
                        binding.btnCreateAccount.isEnabled = false
                        binding.btnCreateAccount.text = "Creating Account..."
                    }

                    is AuthUiState.Success -> {
                        Log.d(TAG, "State: Success - ${state.msg}")
                        binding.btnCreateAccount.isEnabled = true
                        binding.btnCreateAccount.text = "Create Account"

                        Toast.makeText(
                            requireContext(),
                            "Account created successfully!",
                            Toast.LENGTH_SHORT
                        ).show()

                        // Try to navigate
                        try {
                            Log.d(TAG, "Attempting navigation to HomeFragment")

                            // Option 1: Try with NavDirections (if generated)
                            try {
                                findNavController().navigate(
                                    SignupFragmentDirections.actionSignupFragmentToHomeFragment()
                                )
                                Log.d(TAG, "Navigation with NavDirections successful")
                            } catch (e: Exception) {
                                Log.e(TAG, "NavDirections failed: ${e.message}")

                                // Option 2: Try with action ID
                                try {
                                    findNavController().navigate(R.id.action_signupFragment_to_homeFragment)
                                    Log.d(TAG, "Navigation with action ID successful")
                                } catch (e2: Exception) {
                                    Log.e(TAG, "Action ID failed: ${e2.message}")

                                    // Option 3: Navigate directly to destination
                                    try {
                                        findNavController().navigate(R.id.homeFragment)
                                        Log.d(TAG, "Direct navigation to homeFragment successful")
                                    } catch (e3: Exception) {
                                        Log.e(TAG, "Direct navigation failed: ${e3.message}")
                                        Toast.makeText(requireContext(),
                                            "Registration successful! Please restart the app.",
                                            Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "All navigation attempts failed: ${e.message}")
                            Toast.makeText(requireContext(),
                                "Registration successful! Please restart the app.",
                                Toast.LENGTH_LONG).show()
                        }
                    }

                    is AuthUiState.Error -> {
                        Log.e(TAG, "State: Error - ${state.error}")
                        binding.btnCreateAccount.isEnabled = true
                        binding.btnCreateAccount.text = "Create Account"

                        val errorMessage = when {
                            state.error?.contains("already in use") == true ->
                                "This email is already registered"
                            state.error?.contains("invalid-email") == true ->
                                "Invalid email address"
                            state.error?.contains("weak-password") == true ->
                                "Password is too weak"
                            state.error?.contains("network") == true ->
                                "Network error. Please check your connection"
                            else -> state.error ?: "Registration failed"
                        }

                        Toast.makeText(
                            requireContext(),
                            errorMessage,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "onDestroyView called")
        _binding = null
    }
}