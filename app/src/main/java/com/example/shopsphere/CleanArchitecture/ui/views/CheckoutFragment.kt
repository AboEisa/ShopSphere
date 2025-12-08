package com.example.shopsphere.CleanArchitecture.ui.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.NavHostFragment.Companion.findNavController
import com.example.shopsphere.CleanArchitecture.ui.viewmodels.CheckoutSharedViewModel
import com.example.shopsphere.CleanArchitecture.ui.viewmodels.SharedCartViewModel
import com.example.shopsphere.databinding.FragmentCheckoutBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CheckoutFragment : Fragment() {

    private var _binding: FragmentCheckoutBinding? = null
    private val binding get() = _binding!!

    private val sharedViewModel: CheckoutSharedViewModel by activityViewModels()
    private val sharedCartViewModel: SharedCartViewModel by activityViewModels()
   private val shippingCost = 80.0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentCheckoutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (_binding == null) return
        observeCart()
        onMapClick()
        onClicks()
        listenForLocationResult()
        listenForCardResult()
        observeViewModel()
    }

    private fun observeCart() {
        sharedCartViewModel.cartItems.observe(viewLifecycleOwner) { items ->
            val subtotal = items.sumOf { it.price * (it.quantity ?: 1) }
            val totalWithShipping = subtotal + shippingCost
            binding.txtOrderTotal.text = "EGP${String.format("%.2f", totalWithShipping)}"
            binding.textSubTotal.text = "EGP${String.format("%.2f", subtotal)}"
            binding.textShippingFee.text = "EGP${String.format("%.2f", shippingCost)}"

        }

        sharedCartViewModel.totalPrice.observe(viewLifecycleOwner) { total ->
            val totalWithShipping = total + shippingCost
            binding.txtOrderTotal.text = "EGP${String.format("%.2f", totalWithShipping)}"
            binding.textSubTotal.text = "EGP${String.format("%.2f", total)}"
            binding.textShippingFee.text = "EGP${String.format("%.2f", shippingCost)}"
        }
    }

    fun onMapClick() {
        binding.txtChangeAddress.setOnClickListener {
            findNavController(this).navigate(
                CheckoutFragmentDirections.actionCheckoutFragmentToMapsFragment()
            )
        }
    }

    private fun listenForLocationResult() {
        parentFragmentManager.setFragmentResultListener("location_result", viewLifecycleOwner) { _, bundle ->
            val nickName = bundle.getString("nickname") ?: ""
            val fullName = bundle.getString("fullname") ?: ""
            sharedViewModel.setAddress(nickName, fullName)
        }
    }

    private fun listenForCardResult() {
        parentFragmentManager.setFragmentResultListener("card_result", viewLifecycleOwner) { _, bundle ->
            val lastFour = bundle.getString("card_last_four") ?: ""
            sharedViewModel.setCardLastFour(lastFour)
        }
    }

    private fun observeViewModel() {
        sharedViewModel.address.observe(viewLifecycleOwner) { pair ->
            if (pair != null) {
                binding.txtHome.text = pair.first
                binding.txtAddressDetail.text = pair.second
            }
        }
        sharedViewModel.cardLastFour.observe(viewLifecycleOwner) { lastFour ->
            if (!lastFour.isNullOrEmpty()) {
                binding.txtCardNumber.text = "**** **** **** $lastFour"
            }
        }
    }

    fun onClicks() {
        binding.btnEditCard.setOnClickListener {
            findNavController(this).navigate(
                CheckoutFragmentDirections.actionCheckoutFragmentToAddCardFragment()
            )
        }
        binding.btnBack.setOnClickListener {
            findNavController(this).navigateUp()
        }

        binding.btnCheckout.setOnClickListener {
            findNavController(this).navigate(CheckoutFragmentDirections.actionCheckoutFragmentToHomeFragment())
            Toast.makeText(requireContext(), "Order Placed", Toast.LENGTH_SHORT).show()
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}