package com.example.shopsphere.CleanArchitecture.ui.views

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.shopsphere.CleanArchitecture.ui.viewmodels.LoginUiEvent
import com.example.shopsphere.CleanArchitecture.ui.viewmodels.LoginUiState
import com.example.shopsphere.CleanArchitecture.ui.viewmodels.LoginViewModel
import com.example.shopsphere.R
import com.example.shopsphere.databinding.FragmentLoginBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LoginViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
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
                            showToastSafely(state.message)
                            viewModel.consumeTransientState()
                        }
                        else -> setLoading(false)
                    }
                }
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        _binding?.btnLogin?.isEnabled = !isLoading
    }

    private fun navigateToHomeSafely() {
        if (!isAdded) return
        val intent = Intent(requireContext(), MainActivity::class.java)
        intent.putExtra(MainActivity.EXTRA_OPEN_HOME, true)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    private fun showToastSafely(message: String) {
        if (!isAdded) return
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
