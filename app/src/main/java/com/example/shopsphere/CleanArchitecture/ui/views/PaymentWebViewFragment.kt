package com.example.shopsphere.CleanArchitecture.ui.views

import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.shopsphere.CleanArchitecture.data.local.SharedPreference
import com.example.shopsphere.CleanArchitecture.data.network.PayNowRequest
import com.example.shopsphere.CleanArchitecture.domain.PaymentCallbackUseCase
import com.example.shopsphere.CleanArchitecture.domain.PayNowUseCase
import com.example.shopsphere.CleanArchitecture.ui.viewmodels.CartViewModel
import com.example.shopsphere.CleanArchitecture.ui.viewmodels.CheckoutSharedViewModel
import com.example.shopsphere.CleanArchitecture.ui.viewmodels.SharedCartViewModel
import com.example.shopsphere.R
import com.example.shopsphere.databinding.FragmentPaymentWebviewBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Fragment that displays the payment gateway in a WebView.
 * Monitors URL changes to detect payment success/failure.
 */
@AndroidEntryPoint
class PaymentWebViewFragment : Fragment() {

    private var _binding: FragmentPaymentWebviewBinding? = null
    private val binding get() = _binding!!

    private var paymentUrl: String? = null
    private var orderId: Int? = null

    private val TAG = "PaymentWebView"

    @Inject
    lateinit var paymentCallbackUseCase: PaymentCallbackUseCase

    @Inject
    lateinit var markPaymentAsFailedUseCase: com.example.shopsphere.CleanArchitecture.domain.MarkPaymentAsFailedUseCase

    @Inject
    lateinit var payNowUseCase: PayNowUseCase

    @Inject
    lateinit var sharedPreference: SharedPreference

    private val sharedCartViewModel: SharedCartViewModel by activityViewModels()
    private val cartViewModel: CartViewModel by activityViewModels()
    private val sharedViewModel: CheckoutSharedViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Get arguments
        arguments?.let {
            paymentUrl = it.getString(ARG_PAYMENT_URL)
            orderId = it.getInt(ARG_ORDER_ID, 0)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPaymentWebviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupWebView()
        setupClickListeners()
        loadPaymentUrl()
    }

    private fun setupWebView() {
        binding.webviewPayment.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            builtInZoomControls = false
            setSupportZoom(false)
        }

        binding.webviewPayment.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                Log.d(TAG, "Page started loading: $url")
                showLoading(true)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                Log.d(TAG, "Page finished loading: $url")
                showLoading(false)
                // checkPaymentResult uses exact URL matching so it's safe to call always
                checkPaymentResult(url)
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString() ?: return false
                Log.d(TAG, "URL loading: $url")

                if (isPaymentSuccessUrl(url)) {
                    Log.d(TAG, "Payment success redirect detected")
                    handlePaymentSuccess()
                    return true
                } else if (isPaymentFailUrl(url)) {
                    Log.d(TAG, "Payment fail redirect detected")
                    handlePaymentFailure()
                    return true
                }

                return false
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnClose.setOnClickListener {
            handlePaymentCancelled()
        }
    }

    private fun loadPaymentUrl() {
        val url = paymentUrl
        if (url.isNullOrBlank()) {
            showErrorAndClose("No payment URL provided")
            return
        }

        binding.webviewPayment.loadUrl(url)
    }

    private fun checkPaymentResult(url: String?) {
        if (url == null) return

        if (isPaymentSuccessUrl(url)) {
            handlePaymentSuccess()
        } else if (isPaymentFailUrl(url)) {
            handlePaymentFailure()
        }
    }

    private fun isPaymentSuccessUrl(url: String): Boolean {
        // Only match the exact redirect URL configured in RedirectionUrls.
        // Do NOT use generic keyword matching — the payment gateway passes
        // URLs containing "fail", "cancel", "success", etc. as intermediate
        // redirects while loading the payment form. Keyword matching causes
        // false positives before the user even sees the payment page.
        return url.startsWith("https://shopsphere.app/payment/success", ignoreCase = true)
    }

    private fun isPaymentFailUrl(url: String): Boolean {
        return url.startsWith("https://shopsphere.app/payment/fail", ignoreCase = true)
    }

    private fun handlePaymentSuccess() {
        Log.d(TAG, "========================================")
        Log.d(TAG, "PAYMENT SUCCESS DETECTED")
        Log.d(TAG, "Order ID: $orderId")
        Log.d(TAG, "========================================")

        val currentOrderId = orderId ?: run {
            Log.e(TAG, "❌ No orderId available for callback")
            showSuccessDialog()
            return
        }

        // Call the callback API to update payment status
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                Log.d(TAG, "📡 Calling payment callback API...")
                Log.d(TAG, "📦 Request body: { \"invoice_status\": \"paid\", \"OrderId\": \"$currentOrderId\" }")

                val callbackResult = paymentCallbackUseCase(currentOrderId)

                if (callbackResult.isSuccess) {
                    val response = callbackResult.getOrNull()
                    Log.d(TAG, "✅ Payment callback successful!")
                    Log.d(TAG, "📝 Response message: ${response?.message}")
                    Log.d(TAG, "💰 Payment status should now be: PAID")
                } else {
                    val error = callbackResult.exceptionOrNull()
                    Log.e(TAG, "❌ Payment callback failed!")
                    Log.e(TAG, "❗ Error: ${error?.message}")
                    Log.e(TAG, "⚠️ Continuing anyway - payment gateway already charged")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Payment callback exception!")
                Log.e(TAG, "❗ Exception: ${e.message}")
                Log.e(TAG, "📋 Stack trace:", e)
                Log.e(TAG, "⚠️ Continuing anyway - payment gateway already charged")
            } finally {
                Log.d(TAG, "🎉 Showing success dialog to user")
                // Show success dialog after callback
                showSuccessDialog()
            }
        }
    }

    private fun showSuccessDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_payment_success, null)

        dialogView.findViewById<TextView>(R.id.text_order_id).text = "Order #$orderId"

        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_continue)
            .setOnClickListener {
                dialog.dismiss()
                navigateToOrderDetails()
            }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    private fun navigateToOrderDetails() {
        // Clear cart — order is now placed and paid
        sharedPreference.clearCartProducts()
        sharedCartViewModel.setCartItems(emptyList())
        cartViewModel.clearRemoteCart()
        // Refresh orders so order details reflects paid status
        sharedViewModel.fetchOrders()

        val oid = orderId?.toString() ?: run {
            findNavController().popBackStack()
            return
        }
        val bundle = Bundle().apply { putString("orderId", oid) }
        val navOptions = NavOptions.Builder()
            .setPopUpTo(R.id.paymentWebViewFragment, true)
            .build()
        findNavController().navigate(R.id.orderDetailsFragment, bundle, navOptions)
    }

    private fun handlePaymentFailure() {
        val currentOrderId = orderId
        if (currentOrderId != null && currentOrderId > 0) {
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    markPaymentAsFailedUseCase(currentOrderId)
                    sharedViewModel.fetchOrders()
                } catch (e: Exception) {
                    Log.e(TAG, "Error marking order as failed", e)
                }
                showPayAgainDialog()
            }
        } else {
            showPayAgainDialog()
        }
    }

    private fun showPayAgainDialog() {
        if (!isAdded || _binding == null) return
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Payment Failed")
            .setMessage("Your payment could not be completed. Would you like to try again?")
            .setPositiveButton("Pay Again") { _, _ -> retryPayment() }
            .setNegativeButton("Cancel") { _, _ ->
                findNavController().navigate(
                    R.id.ordersFragment,
                    null,
                    NavOptions.Builder().setPopUpTo(R.id.homeFragment, false).build()
                )
            }
            .setCancelable(false)
            .show()
    }

    private fun retryPayment() {
        val currentOrderId = orderId ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            if (!isAdded || _binding == null) return@launch
            binding.loadingOverlay.loadingOverlay.visibility = View.VISIBLE
            binding.loadingOverlay.loadingText.text = "Retrying Payment"
            binding.loadingOverlay.loadingSubtitle.text = "Please wait..."

            val result = payNowUseCase(PayNowRequest(currentOrderId))

            if (!isAdded || _binding == null) return@launch
            binding.loadingOverlay.loadingOverlay.visibility = View.GONE

            if (result.isSuccess) {
                val payNow = result.getOrNull()
                val url = payNow?.url?.takeIf { it.isNotBlank() }
                    ?: payNow?.paymentUrl?.takeIf { it.isNotBlank() }
                if (url != null) {
                    binding.webviewPayment.loadUrl(url)
                } else {
                    showPayAgainDialog()
                }
            } else {
                showPayAgainDialog()
            }
        }
    }

    private fun handlePaymentCancelled() {
        val currentOrderId = orderId

        if (currentOrderId != null && currentOrderId > 0) {
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    markPaymentAsFailedUseCase(currentOrderId)
                    sharedViewModel.fetchOrders()
                    Log.d(TAG, "Order $currentOrderId marked as failed (user cancelled WebView)")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to mark order as failed on cancel", e)
                }
                showCancelledDialog()
            }
        } else {
            showCancelledDialog()
        }
    }

    private fun showCancelledDialog() {
        if (!isAdded || _binding == null) return
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Payment Not Completed")
            .setMessage("You closed the payment page without completing payment. You can retry from My Orders.")
            .setPositiveButton("Go to My Orders") { _, _ ->
                findNavController().navigate(
                    com.example.shopsphere.R.id.ordersFragment,
                    null,
                    androidx.navigation.NavOptions.Builder()
                        .setPopUpTo(com.example.shopsphere.R.id.homeFragment, false)
                        .build()
                )
            }
            .setNegativeButton("Stay Here") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    private fun showErrorAndClose(message: String) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Payment Error")
            .setMessage(message)
            .setPositiveButton("OK") { _, _ ->
                findNavController().navigateUp()
            }
            .setCancelable(false)
            .show()
    }

    private fun showLoading(show: Boolean) {
        binding.loadingOverlay.loadingOverlay.visibility = if (show) View.VISIBLE else View.GONE
        binding.progressHeader.visibility = if (show) View.VISIBLE else View.GONE

        if (show) {
            binding.loadingOverlay.loadingText.text = "Processing Payment"
            binding.loadingOverlay.loadingSubtitle.text = "Please complete your payment"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.webviewPayment.destroy()
        _binding = null
    }

    companion object {
        const val ARG_PAYMENT_URL = "payment_url"
        const val ARG_ORDER_ID = "order_id"
    }
}