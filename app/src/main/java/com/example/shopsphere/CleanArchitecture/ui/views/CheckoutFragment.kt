package com.example.shopsphere.CleanArchitecture.ui.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.example.shopsphere.CleanArchitecture.data.local.SharedPreference
import com.example.shopsphere.R
import com.example.shopsphere.CleanArchitecture.ui.models.PresentationProductResult
import com.example.shopsphere.CleanArchitecture.ui.viewmodels.CheckoutSharedViewModel
import com.example.shopsphere.CleanArchitecture.ui.viewmodels.SharedCartViewModel
import com.example.shopsphere.CleanArchitecture.utils.showConfirmDialog
import com.example.shopsphere.CleanArchitecture.utils.showSuccessDialog
import com.example.shopsphere.databinding.FragmentCheckoutBinding
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CheckoutFragment : Fragment() {

    private var _binding: FragmentCheckoutBinding? = null
    private val binding get() = _binding!!

    private val sharedViewModel: CheckoutSharedViewModel by activityViewModels()
    private val sharedCartViewModel: SharedCartViewModel by activityViewModels()
    private val shippingCost = 80.0
    private var currentCartItems: List<PresentationProductResult> = emptyList()

    @Inject
    lateinit var sharedPreference: SharedPreference

    @Inject
    lateinit var firebaseAuth: FirebaseAuth

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
        observeViewModel()
    }

    private fun observeCart() {
        sharedCartViewModel.cartItems.observe(viewLifecycleOwner) { items ->
            currentCartItems = items
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
            findNavController().navigate(R.id.addressBookFragment)
        }
    }

    private fun observeViewModel() {
        sharedViewModel.selectedAddress.observe(viewLifecycleOwner) { selected ->
            if (selected != null) {
                binding.txtHome.text = selected.title
                binding.txtAddressDetail.text = selected.address
            }
        }
        sharedViewModel.selectedPaymentMethod.observe(viewLifecycleOwner) { selected ->
            if (selected != null) {
                binding.txtCardNumber.text =
                    getString(R.string.account_card_ending_in, selected.lastFour)
            }
        }
    }

    fun onClicks() {
        binding.btnEditCard.setOnClickListener {
            findNavController().navigate(R.id.paymentMethodsFragment)
        }
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnCheckout.setOnClickListener {
            val validationError = validateCheckout()
            if (validationError != null) {
                Toast.makeText(requireContext(), validationError, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            showConfirmDialog(
                title = getString(R.string.dialog_order_confirm_title),
                message = getString(R.string.dialog_order_confirm_message),
                positiveText = getString(R.string.place_order)
            ) {
                val user = firebaseAuth.currentUser
                val customerName = sharedPreference.getProfileName().ifBlank {
                    user?.displayName?.takeIf { it.isNotBlank() } ?: getString(R.string.account_guest_user)
                }
                val phone = sharedPreference.getProfilePhone().ifBlank { "01000000000" }
                val total = binding.txtOrderTotal.text.toString()

                val placedOrderResult = sharedViewModel.placeOrder(
                    total = total,
                    customerName = customerName,
                    phone = phone,
                    cartItems = currentCartItems
                )
                val placedOrder = placedOrderResult.getOrNull()
                if (placedOrder == null) {
                    Toast.makeText(
                        requireContext(),
                        placedOrderResult.exceptionOrNull()?.message
                            ?: getString(R.string.validation_checkout_failed),
                        Toast.LENGTH_SHORT
                    ).show()
                    return@showConfirmDialog
                }

                sharedPreference.saveCartProducts(emptyMap())
                sharedCartViewModel.setCartItems(emptyList())

                showSuccessDialog(
                    title = getString(R.string.dialog_order_success_title),
                    message = getString(R.string.dialog_order_success_message),
                    primaryText = getString(R.string.track_your_order),
                    secondaryText = getString(R.string.continue_shopping),
                    onSecondary = {
                        val navController = findNavController()
                        navController.navigate(
                            R.id.homeFragment,
                            null,
                            navOptions {
                                popUpTo(R.id.homeFragment) {
                                    inclusive = false
                                }
                                launchSingleTop = true
                            }
                        )
                    }
                ) {
                    val action = CheckoutFragmentDirections
                        .actionCheckoutFragmentToTrackOrderFragment(placedOrder.orderId)
                    findNavController().navigate(action)
                }
            }
        }

    }

    private fun validateCheckout(): String? {
        if (currentCartItems.isEmpty()) {
            return getString(R.string.validation_cart_empty)
        }

        val invalidStockItem = currentCartItems.firstOrNull { item ->
            val stock = item.rating.count.coerceAtLeast(0)
            val quantity = item.quantity.coerceAtLeast(1)
            quantity > stock || stock <= 0
        }
        if (invalidStockItem != null) {
            val stock = invalidStockItem.rating.count.coerceAtLeast(0)
            return if (stock <= 0) {
                getString(R.string.validation_product_out_of_stock, invalidStockItem.title)
            } else {
                getString(R.string.validation_product_stock_exceeded, invalidStockItem.title, stock)
            }
        }

        if (!sharedViewModel.isAddressValid(sharedViewModel.selectedAddress.value)) {
            return getString(R.string.validation_address_invalid)
        }

        if (!sharedViewModel.isPaymentMethodValid(sharedViewModel.selectedPaymentMethod.value)) {
            return getString(R.string.validation_payment_invalid)
        }

        return null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
