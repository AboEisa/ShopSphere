package com.example.shopsphere.CleanArchitecture.ui.views

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import com.example.shopsphere.CleanArchitecture.utils.showSuccessDialog
import com.example.shopsphere.databinding.FragmentAddCardBinding
import dagger.hilt.android.AndroidEntryPoint
import com.google.firebase.auth.FirebaseAuth
import javax.inject.Inject

@AndroidEntryPoint
class AddCardFragment : Fragment() {

    private var _binding: FragmentAddCardBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var firebaseAuth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddCardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupLivePreview()

        binding.btnAddCart.setOnClickListener {
            val holderName = binding.editCardHolder.text.toString().trim()
            val cardDigits = binding.editCardNumber.text.toString().filter { it.isDigit() }
            val expiry = binding.editExpiry.text.toString().trim()
            val cvv = binding.editCvv.text.toString().trim()

            if (holderName.isBlank()) {
                binding.editCardHolder.error = getString(com.example.shopsphere.R.string.add_card_invalid_holder)
                return@setOnClickListener
            }
            if (cardDigits.length !in 13..19) {
                binding.editCardNumber.error = getString(com.example.shopsphere.R.string.add_card_invalid_number)
                return@setOnClickListener
            }
            if (!isValidExpiry(expiry)) {
                binding.editExpiry.error = getString(com.example.shopsphere.R.string.add_card_invalid_expiry)
                return@setOnClickListener
            }
            if (cvv.length !in 3..4 || cvv.any { !it.isDigit() }) {
                binding.editCvv.error = getString(com.example.shopsphere.R.string.add_card_invalid_cvv)
                return@setOnClickListener
            }

            val brand = detectBrand(cardDigits)
            val lastFour = cardDigits.takeLast(4)

            val result = Bundle().apply {
                putString("card_brand", brand)
                putString("card_last_four", lastFour)
                putString("card_holder_name", holderName)
            }
            setFragmentResult("card_result", result)
            showSuccessDialog(
                title = getString(com.example.shopsphere.R.string.add_card_saved_title),
                message = getString(com.example.shopsphere.R.string.add_card_saved_message)
            ) {
                findNavController().navigateUp()
            }
        }

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupLivePreview() {
        val defaultHolder = firebaseAuth.currentUser?.displayName?.takeIf { it.isNotBlank() } ?: "ShopSphere User"
        binding.textCardHolderPreview.text = defaultHolder

        binding.editCardHolder.addTextChangedListener(simpleWatcher {
            val holder = binding.editCardHolder.text.toString().trim()
            binding.textCardHolderPreview.text = if (holder.isNotBlank()) holder.uppercase() else defaultHolder.uppercase()
        })
        binding.editCardNumber.addTextChangedListener(simpleWatcher {
            val digits = binding.editCardNumber.text.toString().filter { ch -> ch.isDigit() }
            val formatted = formatCardNumber(digits)
            if (binding.editCardNumber.text.toString() != formatted) {
                binding.editCardNumber.setText(formatted)
                binding.editCardNumber.setSelection(formatted.length)
            }
            binding.textCardNumberPreview.text =
                if (digits.length >= 4) formatCardNumber(digits).padEnd(19, '*')
                else "**** **** **** 1234"
        })
        binding.editExpiry.addTextChangedListener(simpleWatcher {
            val formatted = formatExpiry(binding.editExpiry.text.toString())
            if (binding.editExpiry.text.toString() != formatted) {
                binding.editExpiry.setText(formatted)
                binding.editExpiry.setSelection(formatted.length)
            }
            binding.textExpiryPreview.text = if (formatted.isBlank()) "MM/YY" else formatted
        })
    }

    private fun simpleWatcher(onAfter: () -> Unit): TextWatcher {
        return object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) = onAfter.invoke()
        }
    }

    private fun formatCardNumber(digits: String): String {
        return digits.chunked(4).joinToString(" ").take(19)
    }

    private fun formatExpiry(raw: String): String {
        val digits = raw.filter { it.isDigit() }.take(4)
        return when {
            digits.length >= 3 -> digits.substring(0, 2) + "/" + digits.substring(2)
            else -> digits
        }
    }

    private fun detectBrand(digits: String): String {
        return when {
            digits.startsWith("4") -> "VISA"
            digits.startsWith("34") || digits.startsWith("37") -> "AMEX"
            digits.startsWith("5") || digits.startsWith("2") -> "MASTERCARD"
            else -> "CARD"
        }
    }

    private fun isValidExpiry(expiry: String): Boolean {
        val parts = expiry.split("/")
        if (parts.size != 2) return false
        val month = parts[0].toIntOrNull() ?: return false
        val year = parts[1].toIntOrNull() ?: return false
        if (parts[0].length != 2 || parts[1].length != 2) return false
        return month in 1..12 && year >= 0
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
