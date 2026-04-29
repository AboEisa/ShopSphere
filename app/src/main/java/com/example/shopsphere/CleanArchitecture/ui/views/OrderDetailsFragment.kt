package com.example.shopsphere.CleanArchitecture.ui.views

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.shopsphere.R
import com.example.shopsphere.CleanArchitecture.ui.adapters.OrderProductsAdapter
import com.example.shopsphere.CleanArchitecture.ui.models.OrderHistoryItem
import com.example.shopsphere.CleanArchitecture.ui.viewmodels.CheckoutSharedViewModel
import com.example.shopsphere.databinding.FragmentOrderDetailsBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class OrderDetailsFragment : Fragment() {

    private var _binding: FragmentOrderDetailsBinding? = null
    private val binding get() = _binding!!

    private val args: OrderDetailsFragmentArgs by navArgs()
    private val sharedViewModel: CheckoutSharedViewModel by activityViewModels()
    
    private val TAG = "OrderDetailsFragment"
    
    private val productsAdapter = OrderProductsAdapter()
    
    private var currentOrder: OrderHistoryItem? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOrderDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val orderId = args.orderId
        Log.d(TAG, "========================================")
        Log.d(TAG, "📦 DISPLAYING ORDER DETAILS")
        Log.d(TAG, "🆔 Order ID: $orderId")
        Log.d(TAG, "========================================")
        
        // Setup back button
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
        
        // Setup products recycler view
        binding.recyclerProducts.adapter = productsAdapter
        
        // Observe orders from shared ViewModel
        sharedViewModel.orderHistory.observe(viewLifecycleOwner) { orders ->
            val order = orders.find { it.orderId == orderId }
            if (order != null) {
                currentOrder = order
                displayOrderDetails(order)
            } else {
                Log.e(TAG, "Order not found: $orderId")
            }
        }
        
        // Fetch orders if not already loaded
        if (sharedViewModel.orderHistory.value.isNullOrEmpty()) {
            sharedViewModel.fetchOrders()
        }
    }
    
    private fun displayOrderDetails(order: OrderHistoryItem) {
        Log.d(TAG, "📅 Date: ${order.date}")
        Log.d(TAG, "📊 Status: ${order.status}")
        Log.d(TAG, "💰 Payment: ${order.paymentStatus}")
        Log.d(TAG, "💵 Total: ${order.total}")
        Log.d(TAG, "📍 Address: ${order.shippingAddress}")
        Log.d(TAG, "🛍️ Products: ${order.products.size}")
        
        // Populate order information
        binding.textOrderId.text = "#${order.orderId}"
        binding.textOrderDate.text = order.date.takeIf { it.isNotBlank() } ?: "N/A"
        binding.textOrderStatus.text = order.status.takeIf { it.isNotBlank() } ?: "Processing"
        binding.textPaymentStatus.text = order.paymentStatus.orEmpty().takeIf { it.isNotBlank() } ?: "Pending"
        
        // Sanitize and display shipping address
        val cleanAddress = order.shippingAddress
            .takeIf { it.isNotBlank() && it != "," && !it.matches(Regex("^,+$")) }
            ?: "Address not provided"
        binding.textShippingAddress.text = cleanAddress
        
        // Format total amount
        binding.textTotalAmount.text = order.total.orEmpty().ifBlank { "0.00 EGP" }
        
        // Color code payment status
        when (order.paymentStatus.orEmpty().lowercase()) {
            "paid" -> {
                binding.textPaymentStatus.setTextColor(requireContext().getColor(R.color.bright_green))
                Log.d(TAG, "✅ Payment status: PAID")
            }
            "pending" -> {
                binding.textPaymentStatus.setTextColor(requireContext().getColor(android.R.color.holo_orange_dark))
                Log.d(TAG, "⏳ Payment status: PENDING")
            }
            "failed" -> {
                binding.textPaymentStatus.setTextColor(requireContext().getColor(android.R.color.holo_red_dark))
                Log.d(TAG, "❌ Payment status: FAILED")
            }
            else -> binding.textPaymentStatus.setTextColor(requireContext().getColor(R.color.bright_green))
        }
        
        // Display products
        if (order.products.isEmpty()) {
            binding.recyclerProducts.visibility = View.GONE
            binding.textNoProducts.visibility = View.VISIBLE
            Log.d(TAG, "⚠️ No products in this order")
        } else {
            binding.recyclerProducts.visibility = View.VISIBLE
            binding.textNoProducts.visibility = View.GONE
            productsAdapter.submitList(order.products)
            Log.d(TAG, "✅ Displaying ${order.products.size} product(s)")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
