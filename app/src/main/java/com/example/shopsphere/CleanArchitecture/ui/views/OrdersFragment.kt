package com.example.shopsphere.CleanArchitecture.ui.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.shopsphere.R
import com.example.shopsphere.CleanArchitecture.data.network.PayNowRequest
import com.example.shopsphere.CleanArchitecture.domain.PayNowUseCase
import com.example.shopsphere.CleanArchitecture.ui.adapters.OrdersAdapter
import com.example.shopsphere.CleanArchitecture.ui.models.OrderHistoryItem
import com.example.shopsphere.CleanArchitecture.utils.showSuccessDialog
import com.example.shopsphere.CleanArchitecture.ui.viewmodels.CheckoutSharedViewModel
import com.example.shopsphere.databinding.DialogOrderReviewBinding
import com.example.shopsphere.databinding.FragmentOrdersBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import java.util.Locale
import kotlinx.coroutines.launch

@AndroidEntryPoint
class OrdersFragment : Fragment() {

    private var _binding: FragmentOrdersBinding? = null
    private val binding get() = _binding!!

    private val sharedViewModel: CheckoutSharedViewModel by activityViewModels()

    @Inject
    lateinit var payNowUseCase: PayNowUseCase
    private val ordersAdapter by lazy {
        OrdersAdapter(
            onTrackClicked = { order ->
                val action = OrdersFragmentDirections.actionOrdersFragmentToTrackOrderFragment(order.orderId)
                findNavController().navigate(action)
            },
            onReviewClicked = { order ->
                showReviewSheet(order)
            },
            onOrderClicked = { order ->
                // Navigate to order details with just orderId
                val action = OrdersFragmentDirections.actionOrdersFragmentToOrderDetailsFragment(
                    orderId = order.orderId
                )
                findNavController().navigate(action)
            },
            onPayAgainClicked = { order ->
                handlePayAgain(order)
            }
        )
    }
    private var allOrders: List<OrderHistoryItem> = emptyList()
    private var activeTab: Tab = Tab.ALL
    private var searchQuery: String = ""

    private enum class Tab { ALL, ONGOING, COMPLETED }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOrdersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        android.util.Log.d("OrdersFragment", "onViewCreated - initializing orders screen")

        binding.recyclerOrders.adapter = ordersAdapter

        // Fetch orders immediately on first visit
        android.util.Log.d("OrdersFragment", "Calling fetchOrders() for the first time")
        sharedViewModel.fetchOrders()

        binding.btnBack.setOnClickListener {
            // Navigate back using NavController to respect the back stack
            // This will properly return to AccountFragment when navigated from there
            val navController = findNavController()
            if (!navController.navigateUp()) {
                // If navigateUp fails, fall back to activity back handler
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }
        binding.btnNotifications.setOnClickListener {
            // Top-right icon now reads as a shopping bag in the redesign — open
            // Cart as a tab so the bottom-nav state stays in sync.
            val nav = findNavController()
            val options = androidx.navigation.NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setPopUpTo(R.id.homeFragment, false)
                .build()
            runCatching { nav.navigate(R.id.cartFragment, null, options) }
        }
        binding.tabAll.setOnClickListener {
            activeTab = Tab.ALL
            renderOrders()
        }
        binding.tabOngoing.setOnClickListener {
            activeTab = Tab.ONGOING
            renderOrders()
        }
        binding.tabCompleted.setOnClickListener {
            activeTab = Tab.COMPLETED
            renderOrders()
        }

        binding.searchInput.addTextChangedListener(
            object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun afterTextChanged(s: android.text.Editable?) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    searchQuery = s?.toString().orEmpty().trim()
                    renderOrders()
                }
            }
        )

        sharedViewModel.orderHistory.observe(viewLifecycleOwner) { orders ->
            android.util.Log.d("OrdersFragment", "📦 Orders observed: ${orders.size} orders")
            allOrders = orders
            renderOrders()
        }

        // Show/hide shimmer skeletons while orders load for the FIRST time. If we
        // already have a cached list, we keep it on screen and silently refetch in
        // the background — no flashing skeletons between visits.
        sharedViewModel.isLoadingOrders.observe(viewLifecycleOwner) {
            binding.shimmerOrders.visibility = View.GONE
            binding.shimmerOrders.stopShimmer()
            binding.recyclerOrders.visibility = View.VISIBLE
            binding.emptyState.visibility = if (allOrders.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        // Always refetch so orders appear after signup or placing a new order.
        // The shimmer only flashes when the list is empty; otherwise the old
        // list stays on screen while the silent background fetch completes.
        sharedViewModel.fetchOrders()
    }

    private fun renderOrders() {
        val byTab = when (activeTab) {
            Tab.ALL -> allOrders
            Tab.ONGOING -> allOrders.filterNot(::isCompletedOrder)
            Tab.COMPLETED -> allOrders.filter(::isCompletedOrder)
        }

        val filtered = if (searchQuery.isBlank()) byTab else {
            val q = searchQuery.lowercase(Locale.ENGLISH)
            byTab.filter { order ->
                order.orderId.lowercase(Locale.ENGLISH).contains(q) ||
                        order.status.lowercase(Locale.ENGLISH).contains(q) ||
                        order.itemTitle.lowercase(Locale.ENGLISH).contains(q) ||
                        order.driverName?.lowercase(Locale.ENGLISH)?.contains(q) == true
            }
        }

        updateTabs()
        ordersAdapter.submitList(filtered, activeTab == Tab.COMPLETED)

        val isEmpty = filtered.isEmpty()
        binding.recyclerOrders.visibility = if (isEmpty) View.GONE else View.VISIBLE
        binding.emptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.textEmptyTitle.text = getString(
            if (activeTab == Tab.COMPLETED) R.string.orders_no_completed_title
            else R.string.orders_no_ongoing_title
        )
        binding.textEmptySubtitle.text = getString(
            if (activeTab == Tab.COMPLETED) R.string.orders_no_completed_message
            else R.string.orders_no_ongoing_message
        )
    }

    private fun updateTabs() {
        val context = requireContext()
        val activeBg = R.drawable.bg_segmented_selected
        val activeColor = context.getColor(R.color.bright_green)
        val inactiveColor = 0xFF6B7280.toInt()

        binding.tabAll.background = null
        binding.tabOngoing.background = null
        binding.tabCompleted.background = null
        binding.tabAll.setTextColor(inactiveColor)
        binding.tabOngoing.setTextColor(inactiveColor)
        binding.tabCompleted.setTextColor(inactiveColor)

        when (activeTab) {
            Tab.ALL -> {
                binding.tabAll.setBackgroundResource(activeBg)
                binding.tabAll.setTextColor(activeColor)
            }
            Tab.ONGOING -> {
                binding.tabOngoing.setBackgroundResource(activeBg)
                binding.tabOngoing.setTextColor(activeColor)
            }
            Tab.COMPLETED -> {
                binding.tabCompleted.setBackgroundResource(activeBg)
                binding.tabCompleted.setTextColor(activeColor)
            }
        }
    }

    private fun isCompletedOrder(order: OrderHistoryItem): Boolean {
        return resolveOrderStatusStep(order) >= 3
    }

    private fun resolveOrderStatusStep(order: OrderHistoryItem): Int {
        val normalizedStatus = order.status
            .trim()
            .lowercase(Locale.ENGLISH)
            .replace("_", " ")
            .replace("-", " ")

        val derivedStep = when {
            normalizedStatus.contains("deliver") || normalizedStatus.contains("complete") -> 3
            normalizedStatus.contains("transit") ||
                    normalizedStatus.contains("shipping") ||
                    normalizedStatus.contains("shipped") ||
                    normalizedStatus.contains("out for delivery") -> 2
            normalizedStatus.contains("pick") || normalizedStatus.contains("dispatch") -> 1
            else -> 0
        }

        return maxOf(order.statusStep.coerceIn(0, 3), derivedStep)
    }

    private fun showReviewSheet(order: OrderHistoryItem) {
        val context = requireContext()
        val dialog = BottomSheetDialog(context)
        val dialogBinding = DialogOrderReviewBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        var selectedRating = order.reviewRating.toInt().coerceIn(0, 5)
        val stars = listOf(
            dialogBinding.star1,
            dialogBinding.star2,
            dialogBinding.star3,
            dialogBinding.star4,
            dialogBinding.star5
        )

        fun updateStars() {
            stars.forEachIndexed { index, imageView ->
                imageView.setColorFilter(
                    if (index < selectedRating) context.getColor(R.color.review_star_active)
                    else 0xFFFFD28A.toInt()
                )
            }
        }

        stars.forEachIndexed { index, imageView ->
            imageView.setOnClickListener {
                selectedRating = index + 1
                updateStars()
            }
        }

        dialogBinding.editReviewComment.setText(order.reviewComment)
        dialogBinding.buttonCloseReview.setOnClickListener { dialog.dismiss() }
        dialogBinding.buttonSubmitReview.background = context.getDrawable(R.drawable.bg_black_action)
        dialogBinding.buttonSubmitReview.setOnClickListener {
            val review = dialogBinding.editReviewComment.text.toString().trim()
            when {
                selectedRating == 0 -> {
                    Toast.makeText(context, getString(R.string.review_sheet_rating_required), Toast.LENGTH_SHORT).show()
                }

                review.isBlank() -> {
                    Toast.makeText(context, getString(R.string.review_sheet_text_required), Toast.LENGTH_SHORT).show()
                }

                else -> {
                    sharedViewModel.submitOrderReview(order.orderId, selectedRating, review)
                    dialog.dismiss()
                    activeTab = Tab.COMPLETED
                    renderOrders()
                    showSuccessDialog(
                        title = getString(R.string.review_sheet_success_title),
                        message = getString(R.string.review_sheet_success_message),
                        primaryText = getString(R.string.dialog_done)
                    )
                }
            }
        }

        updateStars()
        dialog.show()
    }

    /**
     * Called when the user taps "Pay Again" on an order with pending/failed payment.
     * Calls PayNow with the orderId, then navigates to the WebView for payment.
     */
    private fun handlePayAgain(order: OrderHistoryItem) {
        val orderId = order.orderId.toIntOrNull() ?: run {
            Toast.makeText(requireContext(), "Invalid order ID", Toast.LENGTH_SHORT).show()
            return
        }

        // Show loading feedback
        Toast.makeText(requireContext(), "Loading payment...", Toast.LENGTH_SHORT).show()

        viewLifecycleOwner.lifecycleScope.launch {
            val result = payNowUseCase(PayNowRequest(orderId = orderId))
            if (result.isFailure) {
                Toast.makeText(
                    requireContext(),
                    "Payment service unavailable: ${result.exceptionOrNull()?.message}",
                    Toast.LENGTH_LONG
                ).show()
                return@launch
            }

            val payNow = result.getOrNull()
            val url = payNow?.url?.takeIf { it.isNotBlank() }
                ?: payNow?.paymentUrl?.takeIf { it.isNotBlank() }

            if (url == null) {
                Toast.makeText(
                    requireContext(),
                    "No payment URL received: ${payNow?.message ?: "Unknown error"}",
                    Toast.LENGTH_LONG
                ).show()
                return@launch
            }

            // Navigate to WebView payment screen
            val action = OrdersFragmentDirections
                .actionOrdersFragmentToPaymentWebViewFragment(url, orderId)
            findNavController().navigate(action)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}