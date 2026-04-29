package com.example.shopsphere.CleanArchitecture.ui.views

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.shopsphere.CleanArchitecture.data.local.SharedPreference
import com.example.shopsphere.CleanArchitecture.data.network.CreateInvoiceRequest
import com.example.shopsphere.CleanArchitecture.data.network.CustomerInfo
import com.example.shopsphere.CleanArchitecture.data.network.InvoiceCartItem
import com.example.shopsphere.CleanArchitecture.data.network.PayNowRequest
import com.example.shopsphere.CleanArchitecture.data.network.RedirectionUrls
import com.example.shopsphere.CleanArchitecture.domain.CreateInvoiceUseCase
import com.example.shopsphere.CleanArchitecture.domain.GetMyDetailsUseCase
import com.example.shopsphere.CleanArchitecture.domain.PayNowUseCase
import com.example.shopsphere.CleanArchitecture.domain.PaymentCallbackUseCase
import com.example.shopsphere.CleanArchitecture.domain.MarkPaymentAsFailedUseCase
import com.example.shopsphere.CleanArchitecture.domain.UpdateMyDetailsUseCase
import com.example.shopsphere.CleanArchitecture.ui.adapters.CheckoutCartItemsAdapter
import com.example.shopsphere.CleanArchitecture.ui.models.PresentationProductResult
import com.example.shopsphere.CleanArchitecture.ui.viewmodels.CartViewModel
import com.example.shopsphere.CleanArchitecture.ui.viewmodels.CheckoutSharedViewModel
import com.example.shopsphere.CleanArchitecture.ui.viewmodels.SharedCartViewModel
import com.example.shopsphere.CleanArchitecture.utils.formatEgpPrice
import com.example.shopsphere.CleanArchitecture.utils.showConfirmDialog
import com.example.shopsphere.CleanArchitecture.utils.showSuccessDialog
import com.example.shopsphere.R
import com.example.shopsphere.databinding.DialogEditAddressPhoneBinding
import com.example.shopsphere.databinding.FragmentCheckoutBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.text.DecimalFormat
import javax.inject.Inject
import kotlin.coroutines.resume

@AndroidEntryPoint
class CheckoutFragment : Fragment() {

    private var _binding: FragmentCheckoutBinding? = null
    private val binding get() = _binding!!

    private val sharedViewModel: CheckoutSharedViewModel by activityViewModels()
    private val sharedCartViewModel: SharedCartViewModel by activityViewModels()
    private val cartViewModel: CartViewModel by activityViewModels()

    private val shippingCost = 0.0  // Free for now
    private var currentCartItems: List<PresentationProductResult> = emptyList()
    private var promoApplied = false
    private var promoDiscount = 0.0

    private lateinit var cartItemsAdapter: CheckoutCartItemsAdapter
    private var pendingOrderId: Int? = null

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

    @Inject
    lateinit var markPaymentAsFailedUseCase: MarkPaymentAsFailedUseCase

    @Inject
    lateinit var getMyDetailsUseCase: GetMyDetailsUseCase

    @Inject
    lateinit var updateMyDetailsUseCase: UpdateMyDetailsUseCase

    /** True once we've handed the user off to the payment provider's URL. */
    private var awaitingPaymentReturn = false

    /** Completer to resume when user returns from payment gateway */
    private var paymentResultCompleter: kotlinx.coroutines.CompletableDeferred<Boolean>? = null

    /** Selected payment method: true = COD, false = Online Payment */
    private var isCashOnDelivery: Boolean = false

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
        setupCartItemsRecyclerView()
        setupPaymentMethodSelector()
        observeCart()
        observeViewModel()
        onClicks()
    }

    private fun setupCartItemsRecyclerView() {
        cartItemsAdapter = CheckoutCartItemsAdapter()
        binding.recyclerCartItems.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = cartItemsAdapter
        }
    }

    private fun setupPaymentMethodSelector() {
        // COD is disabled — always start with online payment selected
        isCashOnDelivery = false
        updatePaymentMethodUI()

        binding.paymentMethodGroup.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.radio_cod) {
                // COD not yet available — revert to online payment silently
                binding.paymentMethodGroup.check(R.id.radio_online_payment)
                return@setOnCheckedChangeListener
            }
            isCashOnDelivery = false
            updatePaymentMethodUI()
            Log.d(TAG, "Payment method selected: Online")
        }
    }

    private fun updatePaymentMethodUI() {
        // Update radio button visual state
        binding.radioOnlinePayment.setBackgroundResource(
            if (!isCashOnDelivery) R.drawable.bg_payment_method_selected
            else R.drawable.bg_form_field
        )
        binding.radioCod.setBackgroundResource(
            if (isCashOnDelivery) R.drawable.bg_payment_method_selected
            else R.drawable.bg_form_field
        )

        // Show/hide payment info layouts
        binding.layoutOnlinePaymentInfo.visibility =
            if (isCashOnDelivery) View.GONE else View.VISIBLE
        binding.layoutCodInfo.visibility =
            if (isCashOnDelivery) View.VISIBLE else View.GONE
    }

    private fun observeCart() {
        sharedCartViewModel.cartItems.observe(viewLifecycleOwner) { items ->
            currentCartItems = items
            if (!promoApplied) {
                promoDiscount = 0.0
                binding.textPromoStatus.visibility = View.GONE
            }
            updateSummary()
            updateCartItemsList()
        }

        sharedCartViewModel.totalPrice.observe(viewLifecycleOwner) {
            updateSummary()
        }
    }

    private fun updateCartItemsList() {
        if (currentCartItems.isEmpty()) {
            binding.recyclerCartItems.visibility = View.GONE
        } else {
            binding.recyclerCartItems.visibility = View.VISIBLE
            cartItemsAdapter.submitList(currentCartItems.toList())
        }
    }

    private fun observeViewModel() {
        sharedViewModel.selectedAddress.observe(viewLifecycleOwner) { selected ->
            binding.txtHome.text = selected?.title?.takeIf { it.isNotBlank() } ?: "N/A"
            binding.txtAddressDetail.text = selected?.address?.takeIf { it.isNotBlank() } ?: "N/A"
            binding.txtAddressPhone.text = selected?.phone?.takeIf { it.isNotBlank() } ?: "N/A"
            binding.txtAddressPhone.visibility = android.view.View.VISIBLE
        }
    }

    private fun onClicks() {
        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        binding.btnNotifications.setOnClickListener {
            findNavController().navigate(R.id.notificationsFragment)
        }
        binding.btnEditAddressPhone.setOnClickListener {
            showEditAddressPhoneDialog()
        }
        binding.btnApplyPromo.setOnClickListener { applyPromoCode() }

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
                // Show modern loading overlay
                binding.loadingOverlay.loadingOverlay.visibility = View.VISIBLE
                binding.loadingOverlay.loadingText.text = "Placing Order"
                binding.loadingOverlay.loadingSubtitle.text = "Please wait while we process your order"

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

                    val placedOrder = placedOrderResult.getOrNull()
                    if (placedOrder == null) {
                        // Hide overlay on failure
                        binding.loadingOverlay.loadingOverlay.visibility = View.GONE
                        binding.btnCheckout.isEnabled = true
                        Toast.makeText(
                            requireContext(),
                            placedOrderResult.exceptionOrNull()?.message
                                ?: getString(R.string.validation_checkout_failed),
                            Toast.LENGTH_SHORT
                        ).show()
                        return@launch
                    }

                    // Store the orderId for payment
                    pendingOrderId = placedOrder.orderId.toIntOrNull()
                    Log.d(TAG, "Order placed successfully with ID: ${placedOrder.orderId}")

                    // Order created successfully in background
                    if (isCashOnDelivery) {
                        binding.loadingOverlay.loadingOverlay.visibility = View.GONE
                        binding.btnCheckout.isEnabled = true
                        Log.d(TAG, "COD selected - completing order without online payment")
                        completeOrderSuccessfully()
                    } else {
                        // Keep overlay visible while we call /PayNow
                        binding.loadingOverlay.loadingText.text = "Preparing Payment"
                        binding.loadingOverlay.loadingSubtitle.text = "Getting your secure payment link…"

                        Log.d(TAG, "Online payment selected - launching payment gateway")
                        when (launchOnlinePayment()) {
                            null -> {
                                // WebView is opening — hide overlay now so it doesn't
                                // show through when PaymentWebViewFragment takes over
                                binding.loadingOverlay.loadingOverlay.visibility = View.GONE
                                binding.btnCheckout.isEnabled = true
                            }
                            false -> {
                                binding.loadingOverlay.loadingOverlay.visibility = View.GONE
                                binding.btnCheckout.isEnabled = true
                                handlePaymentFailure()
                            }
                            true -> {
                                binding.loadingOverlay.loadingOverlay.visibility = View.GONE
                                binding.btnCheckout.isEnabled = true
                                updatePaymentStatusAndCompleteOrder()
                            }
                        }
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
        binding.textShippingFee.text = "Free (Limited Time) 🎉"
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

        // Phone is validated as part of the delivery address (see isAddressValid).
        // Online card payment is always used - no separate validation needed.
        return null
    }

    private fun formatCurrency(value: Double): String = formatEgpPrice(value)

    /**
     * Shows a dialog to edit address and phone number inline
     */
    private fun showEditAddressPhoneDialog() {
        val dialogBinding = DialogEditAddressPhoneBinding.inflate(LayoutInflater.from(requireContext()))

        val currentAddress = sharedViewModel.selectedAddress.value
        dialogBinding.editPhone.setText(currentAddress?.phone?.takeIf { it != "N/A" } ?: "")
        dialogBinding.editAddress.setText(currentAddress?.address?.takeIf { it != "N/A" } ?: "")

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .setCancelable(true)
            .create()

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnSave.setOnClickListener {
            val phone = dialogBinding.editPhone.text?.toString().orEmpty().trim()
            val address = dialogBinding.editAddress.text?.toString().orEmpty().trim()

            if (phone.filter { it.isDigit() }.length < 8) {
                Toast.makeText(requireContext(), getString(R.string.validation_phone_required), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (address.length < 8) {
                Toast.makeText(requireContext(), getString(R.string.validation_address_invalid), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Disable button while updating
            dialogBinding.btnSave.isEnabled = false
            dialogBinding.btnSave.text = getString(R.string.payment_processing)

            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    // First, get current user details
                    val currentDetails = getMyDetailsUseCase().getOrNull()

                    if (currentDetails != null) {
                        // Update with current details but new phone and address
                        val result = updateMyDetailsUseCase(
                            firstName = currentDetails.firstName ?: "",
                            lastName = currentDetails.lastName ?: "",
                            email = currentDetails.email ?: "",
                            phone = phone,
                            address = address
                        )

                        if (result.isSuccess) {
                            // Update local cache
                            sharedViewModel.setAddress(
                                nick = currentAddress?.title ?: "Delivery Address",
                                full = address,
                                latitude = currentAddress?.latitude,
                                longitude = currentAddress?.longitude,
                                isDefault = true,
                                phone = phone
                            )

                            dialog.dismiss()
                            Toast.makeText(
                                requireContext(),
                                "Address and phone updated successfully",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(
                                requireContext(),
                                "Failed to update: ${result.exceptionOrNull()?.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Failed to fetch current user details",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating address and phone", e)
                    Toast.makeText(
                        requireContext(),
                        "Error: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                } finally {
                    dialogBinding.btnSave.isEnabled = true
                    dialogBinding.btnSave.text = getString(R.string.save)
                }
            }
        }

        dialog.show()
    }

    /**
     * Calls /PayNow and opens the payment URL in PaymentWebViewFragment.
     * Returns null when the WebView was launched (result owned by WebView fragment),
     * false when PayNow API failed before WebView could open.
     */
    private suspend fun launchOnlinePayment(): Boolean? {
        return try {
            Log.d(TAG, "Starting payment process...")

            // Get the orderId from the checkout response (stored earlier)
            val orderId = pendingOrderId ?: run {
                Log.e(TAG, "No orderId available from checkout")
                Toast.makeText(
                    requireContext(),
                    "Order ID not found. Please try again.",
                    Toast.LENGTH_LONG
                ).show()
                return false
            }

            // Call PayNow directly with orderId
            val payNowRequest = PayNowRequest(orderId = orderId)
            Log.d(TAG, "Requesting payment URL for order: $orderId")
            val payNowResult = payNowUseCase(payNowRequest)
            Log.d(TAG, "PayNow result: ${payNowResult.isSuccess}")

            if (payNowResult.isFailure) {
                val error = payNowResult.exceptionOrNull()
                val errorMessage = error?.message ?: "Unknown error"
                Log.e(TAG, "PayNow failed with error: $errorMessage", error)

                // Try to get more details from the error
                if (error is retrofit2.HttpException) {
                    val errorBody = error.response()?.errorBody()?.string()
                    Log.e(TAG, "PayNow error body: $errorBody")
                }

                // Order was already created but payment failed to start
                // Mark the order as failed in backend
                Log.w(TAG, "⚠️ Order $orderId created but payment gateway unavailable - marking as failed")

                try {
                    val failResult = markPaymentAsFailedUseCase(orderId)
                    if (failResult.isSuccess) {
                        Log.d(TAG, "✅ Order $orderId marked as failed in backend")
                    } else {
                        Log.e(TAG, "❌ Failed to mark order as failed: ${failResult.exceptionOrNull()?.message}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error marking order as failed", e)
                }
                // Refresh so the orders list shows the correct failed status (not "paid")
                sharedViewModel.fetchOrders()

                Toast.makeText(
                    requireContext(),
                    "Payment service unavailable. Your order was cancelled. Please try again.",
                    Toast.LENGTH_LONG
                ).show()
                return false
            }

            val payNow = payNowResult.getOrNull()
            Log.d(TAG, "PayNow response: $payNow")

            // Try both 'url' and 'paymentUrl' fields (backend might use either)
            val url = payNow?.url?.takeIf { it.isNotBlank() }
                ?: payNow?.paymentUrl?.takeIf { it.isNotBlank() }

            if (url == null) {
                Log.e(TAG, "PayNow returned no URL - success=${payNow?.success}, url=${payNow?.url}, paymentUrl=${payNow?.paymentUrl}, message=${payNow?.message}")
                Toast.makeText(
                    requireContext(),
                    "Payment gateway unavailable: ${payNow?.message ?: "No payment URL received"}",
                    Toast.LENGTH_LONG
                ).show()
                return false
            }

            Log.d(TAG, "Opening payment URL in WebView: $url")

            // Navigate to Payment WebView Fragment
            val action = CheckoutFragmentDirections
                .actionCheckoutFragmentToPaymentWebViewFragment(url, orderId)
            findNavController().navigate(action)

            // null = WebView launched; PaymentWebViewFragment owns success/failure.
            null
        } catch (e: Exception) {
            Log.e(TAG, "Payment launch failed with exception", e)
            Toast.makeText(
                requireContext(),
                "Payment error: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
            false
        } finally {
            paymentResultCompleter = null
        }
    }

    /**
     * Calls /Callbackt to update payment status after successful payment,
     * then completes the order and navigates to tracking.
     */
    private fun updatePaymentStatusAndCompleteOrder() {
        val currentOrderId = pendingOrderId ?: run {
            Log.e(TAG, "❌ No orderId available for callback")
            completeOrderSuccessfully()
            return
        }

        Log.d(TAG, "========================================")
        Log.d(TAG, "🔄 UPDATING PAYMENT STATUS")
        Log.d(TAG, "📦 Order ID: $currentOrderId")
        Log.d(TAG, "========================================")

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Call the callback API to update payment status
                Log.d(TAG, "📡 Calling payment callback API...")
                Log.d(TAG, "📦 Request: { \"invoice_status\": \"paid\", \"OrderId\": \"$currentOrderId\" }")

                val callbackResult = paymentCallbackUseCase(currentOrderId)

                if (callbackResult.isSuccess) {
                    val response = callbackResult.getOrNull()
                    Log.d(TAG, "✅ Payment callback successful!")
                    Log.d(TAG, "📝 Response: ${response?.message}")
                    Log.d(TAG, "💰 Payment status updated to: PAID")
                } else {
                    val error = callbackResult.exceptionOrNull()
                    Log.e(TAG, "❌ Payment callback failed!")
                    Log.e(TAG, "❗ Error: ${error?.message}")
                }

                // Clear cart and complete order
                Log.d(TAG, "🛒 Completing order and clearing cart...")
                completeOrderSuccessfully()
            } catch (e: Exception) {
                Log.e(TAG, "❌ Payment callback exception!")
                Log.e(TAG, "❗ Exception: ${e.message}")
                Log.e(TAG, "⚠️ Completing order anyway - payment gateway already charged")
                // Even if callback fails, the order might still be successful
                // Show success anyway as the payment gateway already charged
                completeOrderSuccessfully()
            }
        }
    }

    /**
     * Handles payment failure scenario
     */
    private fun handlePaymentFailure() {
        if (!isAdded || _binding == null) return

        showSuccessDialog(
            title = getString(R.string.payment_failed),
            message = getString(R.string.payment_failure_message),
            primaryText = getString(R.string.dialog_ok)
        ) {
            // Stay on checkout screen so user can retry
            // Order exists in backend with pending payment status
        }
    }

    /**
     * Completes the order successfully: clears cart and navigates to tracking
     */
    private fun completeOrderSuccessfully() {
        if (!isAdded || _binding == null) return

        sharedPreference.clearCartProducts()
        sharedCartViewModel.setCartItems(emptyList())
        cartViewModel.clearRemoteCart()

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

    override fun onResume() {
        super.onResume()
        // Refresh address/phone from SharedPreferences to pick up any edits made in MyDetails
        android.util.Log.d("CheckoutFragment", "onResume - refreshing address from preferences")
        sharedViewModel.refreshAddressFromPreferences()
        // WebView fragment handles payment result now
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