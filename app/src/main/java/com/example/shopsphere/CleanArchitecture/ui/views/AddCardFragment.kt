package com.example.shopsphere.CleanArchitecture.ui.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import com.example.shopsphere.databinding.FragmentAddCardBinding

class AddCardFragment : Fragment() {

    private var _binding: FragmentAddCardBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddCardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onClicks()
    }

    private fun onClicks() {
        binding.btnAddCart.setOnClickListener {
            val cardNumber = binding.editCardNumber.text.toString().replace(" ", "")
            val expiry = binding.editExpiryDate.text.toString()
            val cvv = binding.editCvv.text.toString()
            var hasError = false
            if (cardNumber.length < 16) {
                binding.editCardNumber.error = "Card number must be 16 digits"
                hasError = true
            }

            if (!expiry.matches(Regex("\\d{2}/\\d{2}"))) {
                binding.editExpiryDate.error = "Expiry must be MM/YY"
                hasError = true
            }
            if (cvv.length < 3) {
                binding.editCvv.error = "CVV must be at least 3 digits"
                hasError = true
            }
            if (hasError) return@setOnClickListener

            // Get last 4 digits
            val lastFour = cardNumber.takeLast(4)


            val result = Bundle().apply {
                putString("card_last_four", lastFour)
            }
            setFragmentResult("card_result", result)
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}