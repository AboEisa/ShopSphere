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
    private var showingCompleted = false

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
        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        binding.btnNotifications.setOnClickListener {
            findNavController().navigate(R.id.notificationsFragment)
        }
        binding.tabOngoing.setOnClickListener {
            showingCompleted = false
            renderOrders()
        }
        binding.tabCompleted.setOnClickListener {
            showingCompleted = true
            renderOrders()
        }

        sharedViewModel.orderHistory.observe(viewLifecycleOwner) { orders ->
            allOrders = orders
            renderOrders()
        }
    }

    private fun renderOrders() {
        val filtered = if (showingCompleted) {
            allOrders.filter(::isCompletedOrder)
        } else {
            allOrders.filterNot(::isCompletedOrder)
        }

        updateTabs()
        ordersAdapter.submitList(filtered, showingCompleted)

        val isEmpty = filtered.isEmpty()
        binding.recyclerOrders.visibility = if (isEmpty) View.GONE else View.VISIBLE
        binding.emptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.textEmptyTitle.text = getString(
            if (showingCompleted) R.string.orders_no_completed_title else R.string.orders_no_ongoing_title
        )
        binding.textEmptySubtitle.text = getString(
            if (showingCompleted) R.string.orders_no_completed_message else R.string.orders_no_ongoing_message
        )
    }

    private fun updateTabs() {
        if (showingCompleted) {
            binding.tabCompleted.setBackgroundResource(R.drawable.bg_segmented_selected)
            binding.tabCompleted.setTextColor(requireContext().getColor(R.color._1a1a1a))
            binding.tabOngoing.background = null
            binding.tabOngoing.setTextColor(requireContext().getColor(R.color._999999))
        } else {
            binding.tabOngoing.setBackgroundResource(R.drawable.bg_segmented_selected)
            binding.tabOngoing.setTextColor(requireContext().getColor(R.color._1a1a1a))
            binding.tabCompleted.background = null
            binding.tabCompleted.setTextColor(requireContext().getColor(R.color._999999))
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
                    showingCompleted = true
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
