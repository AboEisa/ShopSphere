package com.example.shopsphere.CleanArchitecture.ui.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.shopsphere.CleanArchitecture.ui.adapters.PaymentMethodsAdapter
import com.example.shopsphere.CleanArchitecture.ui.viewmodels.CheckoutSharedViewModel
import com.example.shopsphere.CleanArchitecture.utils.showSuccessDialog
import com.example.shopsphere.R
import com.example.shopsphere.databinding.FragmentPaymentMethodsBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PaymentMethodsFragment : Fragment() {

    private var _binding: FragmentPaymentMethodsBinding? = null
    private val binding get() = _binding!!

    private val sharedViewModel: CheckoutSharedViewModel by activityViewModels()
    private val paymentAdapter by lazy {
        PaymentMethodsAdapter { item -> sharedViewModel.selectPaymentMethod(item.id) }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPaymentMethodsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.recyclerCards.adapter = paymentAdapter

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        binding.btnUseCard.setOnClickListener {
            showSuccessDialog(
                title = getString(R.string.dialog_payment_selected_title),
                message = getString(R.string.dialog_payment_selected_message)
            ) {
                findNavController().navigateUp()
            }
        }
        binding.btnAddNewCard.setOnClickListener {
            findNavController().navigate(R.id.addCardFragment)
        }

        parentFragmentManager.setFragmentResultListener("card_result", viewLifecycleOwner) { _, bundle ->
            val lastFour = bundle.getString("card_last_four").orEmpty()
            val holderName = bundle.getString("card_holder_name").orEmpty()
            val brand = bundle.getString("card_brand").orEmpty()
            val wasSaved = sharedViewModel.setCardLastFour(
                lastFour = lastFour,
                holderName = holderName.ifBlank { "ShopSphere User" },
                brand = brand.ifBlank { "CARD" }
            )
            if (wasSaved) {
                showSuccessDialog(
                    title = getString(R.string.dialog_payment_success_title),
                    message = getString(R.string.dialog_payment_success_message)
                )
            } else {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.validation_payment_invalid),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        sharedViewModel.paymentMethods.observe(viewLifecycleOwner) { cards ->
            paymentAdapter.submitList(cards)
            binding.emptyCardState.visibility = if (cards.isEmpty()) View.VISIBLE else View.GONE
            binding.btnUseCard.isEnabled = cards.any { it.isSelected }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
