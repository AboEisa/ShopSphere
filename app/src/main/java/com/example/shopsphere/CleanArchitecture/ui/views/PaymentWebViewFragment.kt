package com.example.shopsphere.CleanArchitecture.ui.views

import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
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
 * The WebView is NEVER shown to the user. A full-screen loading overlay stays
 * visible the entire time while the WebView silently loads and processes the
 * payment in the background. The overlay updates its message as the payment
 * progresses and responds to back-press / cancel at any point.
 */
@AndroidEntryPoint
class PaymentWebViewFragment : Fragment() {

    private var _binding: FragmentPaymentWebviewBinding? = null
    private val binding get() = _binding!!

    private var paymentUrl: String? = null
    private var orderId: Int? = null

    /** True once a success or fail redirect has been handled — prevents double-firing. */
    private var paymentResultHandled = false

    private val TAG = "PaymentWebView"

    @Inject lateinit var paymentCallbackUseCase: PaymentCallbackUseCase
    @Inject lateinit var markPaymentAsFailedUseCase: com.example.shopsphere.CleanArchitecture.domain.MarkPaymentAsFailedUseCase
    @Inject lateinit var payNowUseCase: PayNowUseCase
    @Inject lateinit var sharedPreference: SharedPreference

    private val sharedCartViewModel: SharedCartViewModel by activityViewModels()
    private val cartViewModel: CartViewModel by activityViewModels()
    private val sharedViewModel: CheckoutSharedViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            paymentUrl = it.getString(ARG_PAYMENT_URL)
            orderId = it.getInt(ARG_ORDER_ID, 0)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPaymentWebviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Overlay stays visible the entire time — user never sees the WebView.
        showOverlay("Preparing Secure Payment", "Connecting to payment gateway…", showCancel = true)

        setupWebView()
        setupBackPress()
        binding.btnClose.setOnClickListener { handlePaymentCancelled() }
        loadPaymentUrl()
    }

    // ─── WebView ──────────────────────────────────────────────────────────────

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
                Log.d(TAG, "onPageStarted: $url")
                if (url == null || paymentResultHandled) return

                when {
                    isPaymentSuccessUrl(url) -> {
                        paymentResultHandled = true
                        // Stop so the browser never tries to resolve the redirect domain.
                        view?.stopLoading()
                        showOverlay("Payment Successful", "Updating your order…", showCancel = false)
                        handlePaymentSuccess()
                    }
                    isPaymentFailUrl(url) -> {
                        paymentResultHandled = true
                        view?.stopLoading()
                        showOverlay("Payment Failed", "Please wait…", showCancel = false)
                        handlePaymentFailure()
                    }
                    else -> {
                        // Keep the overlay up while gateway pages are loading
                        binding.progressHeader.visibility = View.VISIBLE
                        updateOverlayText("Loading Payment Gateway", "Please wait…")
                    }
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                Log.d(TAG, "onPageFinished: $url")
                binding.progressHeader.visibility = View.GONE
                if (paymentResultHandled) return
                if (url == null || isPaymentSuccessUrl(url) || isPaymentFailUrl(url)) return

                // Payment form ready — drop the overlay so the user can interact
                hideOverlay()
            }

            override fun onReceivedError(
                view: WebView?, request: WebResourceRequest?, error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                val url = request?.url?.toString() ?: return
                Log.e(TAG, "onReceivedError url=$url code=${error?.errorCode}")

                // Fallback: if the browser couldn't resolve a redirect domain,
                // the overlay is already covering it — just handle the result.
                if (!paymentResultHandled && request?.isForMainFrame == true) {
                    when {
                        isPaymentSuccessUrl(url) -> {
                            paymentResultHandled = true
                            showOverlay("Payment Successful", "Updating your order…", showCancel = false)
                            handlePaymentSuccess()
                        }
                        isPaymentFailUrl(url) -> {
                            paymentResultHandled = true
                            showOverlay("Payment Failed", "Please wait…", showCancel = false)
                            handlePaymentFailure()
                        }
                    }
                }
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?, request: WebResourceRequest?
            ): Boolean {
                val url = request?.url?.toString() ?: return false
                Log.d(TAG, "shouldOverrideUrlLoading: $url")
                if (paymentResultHandled) return true

                return when {
                    isPaymentSuccessUrl(url) -> {
                        paymentResultHandled = true
                        showOverlay("Payment Successful", "Updating your order…", showCancel = false)
                        handlePaymentSuccess()
                        true
                    }
                    isPaymentFailUrl(url) -> {
                        paymentResultHandled = true
                        showOverlay("Payment Failed", "Please wait…", showCancel = false)
                        handlePaymentFailure()
                        true
                    }
                    else -> false
                }
            }
        }
    }

    private fun setupBackPress() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    handlePaymentCancelled()
                }
            }
        )
    }

    private fun loadPaymentUrl() {
        val url = paymentUrl
        if (url.isNullOrBlank()) {
            showErrorAndClose("No payment URL provided")
            return
        }
        binding.webviewPayment.loadUrl(url)
    }

    // ─── URL detection ────────────────────────────────────────────────────────

    private fun isPaymentSuccessUrl(url: String): Boolean {
        val lower = url.lowercase()
        return lower.startsWith("https://shopsphere.app/payment/success") ||
               lower.contains("fawaterk.com/success") ||
               lower.contains("fawaterak.com/success") ||
               lower.contains("paymob.com/success")
    }

    private fun isPaymentFailUrl(url: String): Boolean {
        val lower = url.lowercase()
        return lower.startsWith("https://shopsphere.app/payment/fail") ||
               lower.contains("fawaterk.com/fail") ||
               lower.contains("fawaterak.com/fail") ||
               lower.contains("paymob.com/fail")
    }

    // ─── Payment result handlers ──────────────────────────────────────────────

    private fun handlePaymentSuccess() {
        val currentOrderId = orderId ?: run { showSuccessDialog(); return }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val result = paymentCallbackUseCase(currentOrderId)
                if (result.isSuccess) {
                    Log.d(TAG, "✅ Payment callback successful")
                } else {
                    Log.e(TAG, "❌ Callback failed: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Callback exception", e)
            } finally {
                showSuccessDialog()
            }
        }
    }

    private fun showSuccessDialog() {
        if (!isAdded || _binding == null) return
        hideOverlay()

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
        sharedPreference.clearCartProducts()
        sharedCartViewModel.setCartItems(emptyList())
        cartViewModel.clearRemoteCart()
        sharedViewModel.fetchOrders()

        val oid = orderId?.toString() ?: run { findNavController().popBackStack(); return }
        val bundle = Bundle().apply { putString("orderId", oid) }
        findNavController().navigate(
            R.id.orderDetailsFragment, bundle,
            NavOptions.Builder().setPopUpTo(R.id.paymentWebViewFragment, true).build()
        )
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
        hideOverlay()
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Payment Failed")
            .setMessage("Your payment could not be completed. Would you like to try again?")
            .setPositiveButton("Pay Again") { _, _ -> retryPayment() }
            .setNegativeButton("Cancel") { _, _ ->
                findNavController().navigate(
                    R.id.ordersFragment, null,
                    NavOptions.Builder().setPopUpTo(R.id.homeFragment, false).build()
                )
            }
            .setCancelable(false)
            .show()
    }

    private fun retryPayment() {
        val currentOrderId = orderId ?: return
        paymentResultHandled = false
        viewLifecycleOwner.lifecycleScope.launch {
            if (!isAdded || _binding == null) return@launch
            showOverlay("Retrying Payment", "Getting a fresh payment link…", showCancel = true)

            val result = payNowUseCase(PayNowRequest(currentOrderId))
            if (!isAdded || _binding == null) return@launch

            if (result.isSuccess) {
                val url = result.getOrNull()?.url?.takeIf { it.isNotBlank() }
                    ?: result.getOrNull()?.paymentUrl?.takeIf { it.isNotBlank() }
                if (url != null) {
                    updateOverlayText("Loading Payment Gateway", "Please wait…")
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
        if (paymentResultHandled) return
        paymentResultHandled = true
        val currentOrderId = orderId
        showOverlay("Cancelling", "Please wait…", showCancel = false)
        if (currentOrderId != null && currentOrderId > 0) {
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    markPaymentAsFailedUseCase(currentOrderId)
                    sharedViewModel.fetchOrders()
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
        hideOverlay()
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Payment Not Completed")
            .setMessage("You closed the payment page. You can retry from My Orders.")
            .setPositiveButton("Go to My Orders") { _, _ ->
                findNavController().navigate(
                    R.id.ordersFragment, null,
                    NavOptions.Builder().setPopUpTo(R.id.homeFragment, false).build()
                )
            }
            .setNegativeButton("Stay Here") { dialog, _ ->
                paymentResultHandled = false
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    private fun showErrorAndClose(message: String) {
        if (!isAdded || _binding == null) return
        hideOverlay()
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Payment Error")
            .setMessage(message)
            .setPositiveButton("OK") { _, _ -> findNavController().navigateUp() }
            .setCancelable(false)
            .show()
    }

    // ─── Overlay helpers ──────────────────────────────────────────────────────

    /**
     * Shows the opaque overlay — completely hides the WebView beneath it.
     * [showCancel] controls whether the in-overlay cancel button is visible.
     */
    private fun showOverlay(title: String, subtitle: String, showCancel: Boolean) {
        if (!isAdded || _binding == null) return
        with(binding.loadingOverlay) {
            loadingOverlay.visibility = View.VISIBLE
            loadingText.text = title
            loadingSubtitle.text = subtitle
            btnCancelLoading.visibility = if (showCancel) View.VISIBLE else View.GONE
            btnCancelLoading.setOnClickListener { handlePaymentCancelled() }
        }
    }

    private fun updateOverlayText(title: String, subtitle: String) {
        if (!isAdded || _binding == null) return
        binding.loadingOverlay.loadingText.text = title
        binding.loadingOverlay.loadingSubtitle.text = subtitle
    }

    private fun hideOverlay() {
        if (!isAdded || _binding == null) return
        binding.loadingOverlay.loadingOverlay.visibility = View.GONE
        binding.progressHeader.visibility = View.GONE
    }

    // ─── Lifecycle ────────────────────────────────────────────────────────────

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
