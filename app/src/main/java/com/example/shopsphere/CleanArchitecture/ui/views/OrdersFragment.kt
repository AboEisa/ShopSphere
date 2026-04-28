package com.example.shopsphere.CleanArchitecture.ui.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.shopsphere.R
import com.example.shopsphere.CleanArchitecture.ui.adapters.OrdersAdapter
import com.example.shopsphere.CleanArchitecture.ui.models.OrderHistoryItem
import com.example.shopsphere.CleanArchitecture.utils.showSuccessDialog
import com.example.shopsphere.CleanArchitecture.ui.viewmodels.CheckoutSharedViewModel
import com.example.shopsphere.databinding.DialogOrderReviewBinding
import com.example.shopsphere.databinding.FragmentOrdersBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale

@AndroidEntryPoint
class OrdersFragment : Fragment() {

    private var _binding: FragmentOrdersBinding? = null
    private val binding get() = _binding!!

    private val sharedViewModel: CheckoutSharedViewModel by activityViewModels()
    private val ordersAdapter by lazy {
        OrdersAdapter(
            onTrackClicked = { order ->
                val action = OrdersFragmentDirections.actionOrdersFragmentToTrackOrderFragment(order.orderId)
                findNavController().navigate(action)
            },
            onReviewClicked = { order ->
                showReviewSheet(order)
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
        binding.recyclerOrders.adapter = ordersAdapter
        binding.btnBack.setOnClickListener {
            // Orders is now a root bottom-nav tab; when reached from the nav there
            // is nothing to pop. Delegate to the activity back handler so we route
            // to Home cleanly instead of a no-op.
            requireActivity().onBackPressedDispatcher.onBackPressed()
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}