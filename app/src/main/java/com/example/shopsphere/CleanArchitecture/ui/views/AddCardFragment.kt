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
import com.example.shopsphere.CleanArchitecture.data.local.SharedPreference
import com.example.shopsphere.CleanArchitecture.utils.showSuccessDialog
import com.example.shopsphere.R
import com.example.shopsphere.databinding.FragmentAddCardBinding
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AddCardFragment : Fragment() {

    private var _binding: FragmentAddCardBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var firebaseAuth: FirebaseAuth

    @Inject
    lateinit var sharedPreference: SharedPreference

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddCardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val watcher = simpleWatcher { updateAddButtonState() }
        binding.editCardNumber.addTextChangedListener(simpleWatcher {
            val digits = binding.editCardNumber.text?.toString().orEmpty().filter(Char::isDigit)
            val formatted = formatCardNumber(digits)
            if (binding.editCardNumber.text?.toString() != formatted) {
                binding.editCardNumber.setText(formatted)
                binding.editCardNumber.setSelection(formatted.length)
            }
            updateAddButtonState()
        })
        binding.editExpiry.addTextChangedListener(simpleWatcher {
            val formatted = formatExpiry(binding.editExpiry.text?.toString().orEmpty())
            if (binding.editExpiry.text?.toString() != formatted) {
                binding.editExpiry.setText(formatted)
                binding.editExpiry.setSelection(formatted.length)
            }
            updateAddButtonState()
        })
        binding.editCvv.addTextChangedListener(watcher)

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        binding.btnNotifications.setOnClickListener {
            findNavController().navigate(R.id.notificationsFragment)
        }
        binding.btnAddCart.setOnClickListener { saveCard() }

        updateAddButtonState()
    }

    private fun saveCard() {
        val cardDigits = binding.editCardNumber.text?.toString().orEmpty().filter(Char::isDigit)
        val expiry = binding.editExpiry.text?.toString().orEmpty().trim()
        val cvv = binding.editCvv.text?.toString().orEmpty().trim()

        if (cardDigits.length !in 13..19) {
            binding.editCardNumber.error = getString(R.string.add_card_invalid_number)
            return
        }
        if (!isValidExpiry(expiry)) {
            binding.editExpiry.error = getString(R.string.add_card_invalid_expiry)
            return
        }
        if (cvv.length !in 3..4 || cvv.any { !it.isDigit() }) {
            binding.editCvv.error = getString(R.string.add_card_invalid_cvv)
            return
        }

        val holderName = sharedPreference.getProfileName().ifBlank {
            firebaseAuth.currentUser?.displayName?.takeIf { it.isNotBlank() }
                ?: getString(R.string.account_guest_user)
        }
        val result = Bundle().apply {
            putString("card_brand", detectBrand(cardDigits))
            putString("card_last_four", cardDigits.takeLast(4))
            putString("card_holder_name", holderName)
        }

        showSuccessDialog(
            title = getString(R.string.dialog_congratulations_title),
            message = getString(R.string.dialog_payment_success_message),
            primaryText = getString(R.string.dialog_thanks)
        ) {
            setFragmentResult("card_result", result)
            findNavController().navigateUp()
        }
    }

    private fun updateAddButtonState() {
        val digits = binding.editCardNumber.text?.toString().orEmpty().filter(Char::isDigit)
        val expiry = binding.editExpiry.text?.toString().orEmpty().trim()
        val cvv = binding.editCvv.text?.toString().orEmpty().trim()
        val enabled = digits.length in 13..19 && isValidExpiry(expiry) && cvv.length in 3..4
        binding.btnAddCart.isEnabled = enabled
        binding.btnAddCart.alpha = if (enabled) 1f else 0.45f
    }

    private fun simpleWatcher(onAfter: () -> Unit): TextWatcher {
        return object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) = onAfter.invoke()
        }
    }

    private fun formatCardNumber(digits: String): String {
        return digits.take(16).chunked(4).joinToString(" ")
    }

    private fun formatExpiry(raw: String): String {
        val digits = raw.filter(Char::isDigit).take(4)
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
        return parts[0].length == 2 && parts[1].length == 2 && month in 1..12 && year >= 0
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
