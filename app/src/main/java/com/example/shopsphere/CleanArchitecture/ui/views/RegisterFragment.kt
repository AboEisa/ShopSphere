package com.example.shopsphere.CleanArchitecture.ui.views
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.shopsphere.CleanArchitecture.ui.viewmodels.RegisterViewModel
import com.example.shopsphere.CleanArchitecture.ui.viewmodels.AuthUiState
import com.example.shopsphere.databinding.FragmentSignupBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RegisterFragment : Fragment() {
    private var _binding: FragmentSignupBinding? = null
    private val binding get() = _binding!!
    private val viewModel: RegisterViewModel by viewModels()
    override fun onCreateView(inflater: LayoutInflater, container:
    ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentSignupBinding.inflate(inflater, container,
            false)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnCreateAccount.setOnClickListener {
            val name = binding.etFullName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val pass = binding.etPassword.text.toString()
            val confirm = binding.etConfirmPassword.text.toString()
            if (name.isEmpty() || email.isEmpty() || pass.isEmpty() ||
                confirm.isEmpty()) {
                Toast.makeText(requireContext(), "Fill all fields",
                    Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (pass != confirm) {
                Toast.makeText(requireContext(), "Passwords don't match",Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.register(name, email, pass)
        }
        lifecycleScope.launchWhenStarted {
            viewModel.state.collect { state ->
                when (state) {
                    is AuthUiState.Loading ->
                        binding.btnCreateAccount.isEnabled = false
                    is AuthUiState.Success -> {
                        binding.btnCreateAccount.isEnabled = true
                        Toast.makeText(requireContext(), state.msg,
                            Toast.LENGTH_SHORT).show()
// navigate to main/home
                    }
                    is AuthUiState.Error -> {
                        binding.btnCreateAccount.isEnabled = true
                        Toast.makeText(requireContext(), state.error ?:
                        "Error", Toast.LENGTH_SHORT).show()
                    }
                    else -> {}
                }
            }
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}