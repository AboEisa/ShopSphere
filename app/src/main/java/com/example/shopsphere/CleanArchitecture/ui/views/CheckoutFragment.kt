package com.example.shopsphere.CleanArchitecture.ui.views

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.browser.customtabs.CustomTabsIntent
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.shopsphere.CleanArchitecture.data.local.SharedPreference
import com.example.shopsphere.CleanArchitecture.domain.CreateInvoiceUseCase
import com.example.shopsphere.CleanArchitecture.domain.PayNowUseCase
import com.example.shopsphere.CleanArchitecture.domain.PaymentCallbackUseCase
import com.example.shopsphere.CleanArchitecture.ui.models.PresentationProductResult
import com.example.shopsphere.CleanArchitecture.ui.viewmodels.CartViewModel
import com.example.shopsphere.CleanArchitecture.ui.viewmodels.CheckoutSharedViewModel
import com.example.shopsphere.CleanArchitecture.ui.viewmodels.SharedCartViewModel
import com.example.shopsphere.CleanArchitecture.utils.formatEgpPrice
import com.example.shopsphere.CleanArchitecture.utils.showConfirmDialog
import com.example.shopsphere.CleanArchitecture.utils.showSuccessDialog
import com.example.shopsphere.R
import com.example.shopsphere.databinding.FragmentCheckoutBinding
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import java.text.DecimalFormat
import javax.inject.Inject

@AndroidEntryPoint
class CheckoutFragment : Fragment() {

    private var _binding: FragmentCheckoutBinding? = null
    private val binding get() = _binding!!

    private val sharedViewModel: CheckoutSharedViewModel by activityViewModels()
    private val sharedCartViewModel: SharedCartViewModel by activityViewModels()
    private val cartViewModel: CartViewModel by activityViewModels()

    private val shippingCost = 80.0
    private var currentCartItems: List<PresentationProductResult> = emptyList()
    private var promoApplied = false
    private var promoDiscount = 0.0

    @Inject
    lateinit var sharedPreference: SharedPreference

    @Inject
    lateinit var firebaseAuth: FirebaseAuth

    @Inject
    lateinit var createInvoiceUseCase: CreateInvoiceUseCase

    @Inject
    lateinit var payNowUseCase: PayNowUseCase

    @Inject
    lateinit var paymentCallbackUseCase: PaymentCallbackUseCase

    /** True once we've handed the user off to the payment provider's URL. */
    private var awaitingPaymentReturn = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCheckoutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeCart()
        observeViewModel()
        onClicks()
    }

    private fun observeCart() {
        sharedCartViewModel.cartItems.observe(viewLifecycleOwner) { items ->
            currentCartItems = items
            if (!promoApplied) {
                promoDiscount = 0.0
                binding.textPromoStatus.visibility = View.GONE
            }
            updateSummary()
        }

        sharedCartViewModel.totalPrice.observe(viewLifecycleOwner) {
            updateSummary()
        }
    }

    private fun observeViewModel() {
        sharedViewModel.selectedAddress.observe(viewLifecycleOwner) { selected ->
            binding.txtHome.text = selected?.title?.takeIf { it.isNotBlank() } ?: "N/A"
            binding.txtAddressDetail.text = selected?.address?.takeIf { it.isNotBlank() } ?: "N/A"
            binding.txtAddressPhone.text = selected?.phone?.takeIf { it.isNotBlank() } ?: "N/A"
            binding.txtAddressPhone.visibility = android.view.View.VISIBLE
        }

        sharedViewModel.selectedPaymentMethod.observe(viewLifecycleOwner) { selected ->
            if (selected != null) {
                binding.txtCardNumber.text =
                    getString(R.string.account_card_ending_in, selected.lastFour)
                binding.textPaymentBrand.text = selected.brand
            }
        }
    }

    private fun onClicks() {
        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        binding.btnNotifications.setOnClickListener {
            findNavController().navigate(R.id.notificationsFragment)
        }
        binding.txtChangeAddress.setOnClickListener {
            findNavController().navigate(R.id.addressBookFragment)
        }
        binding.btnEditCard.setOnClickListener {
            findNavController().navigate(R.id.paymentMethodsFragment)
        }
        binding.btnApplyPromo.setOnClickListener { applyPromoCode() }
        binding.paymentTabCard.setOnClickListener {
            findNavController().navigate(R.id.paymentMethodsFragment)
        }
        binding.paymentTabCash.setOnClickListener {
            Toast.makeText(
                requireContext(),
                getString(R.string.payment_method_coming_soon, getString(R.string.payment_cash)),
                Toast.LENGTH_SHORT
            ).show()
        }
        binding.paymentTabApple.setOnClickListener {
            Toast.makeText(
                requireContext(),
                getString(R.string.payment_method_coming_soon, getString(R.string.payment_apple_pay)),
                Toast.LENGTH_SHORT
            ).show()
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
                // Disable the button while the checkout API is in flight, so the
                // user can't trigger it twice and we don't clear the cart until
                // the backend confirms the order.
                binding.btnCheckout.isEnabled = false
                viewLifecycleOwner.lifecycleScope.launch {
                    val placedOrderResult = sharedViewModel.placeOrder(
                        total = binding.txtOrderTotal.text.toString(),
                        customerName = customerName,
                        phone = "",
                        cartItems = currentCartItems
                    )
                    if (_binding == null || !isAdded) return@launch
                    binding.btnCheckout.isEnabled = true

                    val placedOrder = placedOrderResult.getOrNull()
                    if (placedOrder == null) {
                        Toast.makeText(
                            requireContext(),
                            placedOrderResult.exceptionOrNull()?.message
                                ?: getString(R.string.validation_checkout_failed),
                            Toast.LENGTH_SHORT
                        ).show()
                        return@launch
                    }

                    sharedPreference.clearCartProducts()
                    sharedCartViewModel.setCartItems(emptyList())
                    cartViewModel.clearRemoteCart()

                    // Card path → run the online payment handshake.
                    // /CreateInvoice prepares the invoice on the server, /PayNow
                    // returns the provider URL we hand off to the user.
                    // Cash / Apple Pay / "no card selected" all skip this and
                    // settle out-of-band.
                    if (sharedViewModel.selectedPaymentMethod.value != null) {
                        launchOnlinePayment()
                    }

                    showSuccessDialog(
                        title = getString(R.string.dialog_congratulations_title),
                        message = getString(R.string.dialog_order_success_message),
                        primaryText = getString(R.string.track_your_order)
                    ) {
                        // Wait for fetchOrders() to return the real order from the backend,
                        // then navigate with the real orderId (not "PENDING")
                        val observer = object : Observer<List<com.example.shopsphere.CleanArchitecture.ui.models.OrderHistoryItem>> {
                            override fun onChanged(orders: List<com.example.shopsphere.CleanArchitecture.ui.models.OrderHistoryItem>) {
                                val realOrder = orders.firstOrNull { it.orderId != "PENDING" }
                                if (realOrder != null) {
                                    sharedViewModel.orderHistory.removeObserver(this)
                                    if (findNavController().currentDestination?.id == R.id.checkoutFragment) {
                                        val action = CheckoutFragmentDirections
                                            .actionCheckoutFragmentToTrackOrderFragment(realOrder.orderId)
                                        findNavController().navigate(action)
                                    }
                                }
                            }
                        }
                        sharedViewModel.orderHistory.observe(viewLifecycleOwner, observer)
                    }
                }
            }
        }
    }

    private fun applyPromoCode() {
        val code = binding.editPromoCode.text?.toString().orEmpty().trim().uppercase()
        val subtotal = currentCartItems.sumOf { it.price * it.quantity.coerceAtLeast(1) }

        when {
            code.isBlank() -> {
                Toast.makeText(requireContext(), getString(R.string.promo_code_empty), Toast.LENGTH_SHORT).show()
            }

            promoApplied -> {
                Toast.makeText(requireContext(), getString(R.string.promo_code_already_applied), Toast.LENGTH_SHORT).show()
            }

            code == PROMO_CODE && subtotal > 0.0 -> {
                promoApplied = true
                promoDiscount = subtotal * 0.10
                binding.textPromoStatus.visibility = View.VISIBLE
                binding.textPromoStatus.text = getString(R.string.promo_code_applied)
                updateSummary()
            }

            else -> {
                Toast.makeText(requireContext(), getString(R.string.promo_code_invalid), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateSummary() {
        val subtotal = currentCartItems.sumOf { it.price * it.quantity.coerceAtLeast(1) }
        if (promoApplied) {
            promoDiscount = (subtotal * 0.10).coerceAtLeast(0.0)
        }
        val adjustedSubtotal = (subtotal - promoDiscount).coerceAtLeast(0.0)
        val totalWithShipping = adjustedSubtotal + shippingCost

        binding.textSubTotal.text = formatCurrency(subtotal)
        binding.textShippingFee.text = formatCurrency(shippingCost)
        binding.txtOrderTotal.text = formatCurrency(totalWithShipping)
        binding.textVat.text = formatCurrency(0.0)
        binding.textDiscountValue.text = if (promoApplied) {
            "-${formatCurrency(promoDiscount)}"
        } else {
            formatCurrency(0.0)
        }
    }

    private fun validateCheckout(): String? {
        if (currentCartItems.isEmpty()) {
            return getString(R.string.validation_cart_empty)
        }

        val invalidStockItem = currentCartItems.firstOrNull { item ->
            val stock = item.stock.coerceAtLeast(0)
            val quantity = item.quantity.coerceAtLeast(1)
            quantity > stock || stock <= 0
        }
        if (invalidStockItem != null) {
            val stock = invalidStockItem.stock.coerceAtLeast(0)
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

        // Phone is validated as part of the delivery address (see isAddressValid).
        // No separate profile-phone check needed here.
        return null
    }

    private fun formatCurrency(value: Double): String = formatEgpPrice(value)

    /**
     * Calls /CreateInvoice → /PayNow and opens the provider payment URL in a
     * Chrome Custom Tab. Falls back to a plain browser intent if Chrome isn't
     * installed. Errors are swallowed silently — the order is already placed
     * and we don't want to confuse the user with provider-internal failures.
     */
    private fun launchOnlinePayment() {
        viewLifecycleOwner.lifecycleScope.launch {
            createInvoiceUseCase()  // server-side preparation; we don't need the body
            val payNow = payNowUseCase().getOrNull()
            val url = payNow?.paymentUrl?.takeIf { it.isNotBlank() } ?: run {
                Log.w(TAG, "PayNow returned no URL — order will be settled out-of-band")
                return@launch
            }

            val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return@launch
            try {
                CustomTabsIntent.Builder().build().launchUrl(requireContext(), uri)
            } catch (_: Throwable) {
                // Fall back to default browser if no CCT-capable browser is present.
                runCatching { startActivity(Intent(Intent.ACTION_VIEW, uri)) }
            }
            awaitingPaymentReturn = true
        }
    }

    override fun onResume() {
        super.onResume()
        // When the user returns from the payment provider, ask the server to
        // reconcile via /Callbackt. The provider also calls this server-side,
        // but a foreground ping shortens the "stuck on Pending" window.
        if (awaitingPaymentReturn) {
            awaitingPaymentReturn = false
            viewLifecycleOwner.lifecycleScope.launch {
                paymentCallbackUseCase()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val PROMO_CODE = "SAVE10"
        private const val TAG = "CheckoutFragment"
    }
}