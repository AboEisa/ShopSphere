package com.example.shopsphere.CleanArchitecture.ui.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.shopsphere.CleanArchitecture.ui.adapters.CartAdapter
import com.example.shopsphere.CleanArchitecture.ui.models.PresentationProductResult
import com.example.shopsphere.CleanArchitecture.ui.viewmodels.CartViewModel
import com.example.shopsphere.CleanArchitecture.ui.viewmodels.SharedCartViewModel
import com.example.shopsphere.databinding.FragmentCartBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CartFragment : Fragment() {

    private var _binding: FragmentCartBinding? = null
    private val binding get() = _binding!!
    private val cartViewModel: CartViewModel by viewModels()
    private val sharedCartViewModel: SharedCartViewModel by activityViewModels()
    private lateinit var cartAdapter: CartAdapter
    val productId = 0


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCartBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (_binding == null) return
        setupRecyclerView()
        observeCartItems()
        observeLoading()
        observeEmptyState()
        updateTotalPrice()
        onClicks()
    }

    override fun onResume() {
        super.onResume()
        cartViewModel.loadCartProducts()
        cartViewModel.refreshCartCount()
    }

    fun onClicks() {
        if (_binding == null) return
        binding.btnBack.setOnClickListener {
            if (!isAdded || _binding == null) return@setOnClickListener
            findNavController().navigateUp()
        }
        binding.btnCheckout.setOnClickListener {
            if (!isAdded || _binding == null) return@setOnClickListener

            sharedCartViewModel.setCartItems(cartAdapter.getItems())
            findNavController().navigate(
               CartFragmentDirections.actionCartFragmentToCheckoutFragment(
                   productId
               )
            )
        }
    }

    private fun setupRecyclerView() {
        if (_binding == null) return
        cartAdapter = CartAdapter(
            onItemClick = { productId ->
                if (!isAdded || _binding == null) return@CartAdapter
                val action = CartFragmentDirections.actionCartFragmentToDetailsFragment(productId)
                findNavController().navigate(action)
            },
            onRemoveClick = { productId ->
                if (!isAdded || _binding == null) return@CartAdapter
                cartViewModel.removeFromCart(productId)
                updateTotalPrice()
                if (isAdded && _binding != null) {
                    Toast.makeText(requireContext(), "Product removed from cart", Toast.LENGTH_SHORT).show()
                }
            },
            onQuantityChanged = { productId, newQuantity ->
                if (!isAdded || _binding == null) return@CartAdapter
                cartViewModel.updateQuantity(productId, newQuantity)
                updateTotalPrice()
            }
        )
        binding.recyclerView.adapter = cartAdapter
        binding.recyclerView.itemAnimator = null
    }

    private fun observeCartItems() {
        cartViewModel.cartProducts.observe(viewLifecycleOwner) { cartProducts ->
            if (!isAdded || _binding == null) return@observe
            if (cartProducts != null) {
                cartAdapter.submitList(cartProducts)
                updateTotalPrice()
                binding.recyclerView.visibility = if (cartProducts.isEmpty()) View.GONE else View.VISIBLE
                updateEmptyStateVisibility(cartProducts.isEmpty())
            }
        }
    }

    private fun observeLoading() {
        cartViewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            if (!isAdded || _binding == null) return@observe
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }

    private fun observeEmptyState() {
        cartViewModel.emptyState.observe(viewLifecycleOwner) { isEmpty ->
            if (!isAdded || _binding == null) return@observe
            updateEmptyStateVisibility(isEmpty)
        }
    }

    private fun updateEmptyStateVisibility(isEmpty: Boolean) {
        if (_binding == null) return
        if (isEmpty) {
            showEmptyState()
        } else {
            hideEmptyState()
        }
    }

    private fun updateTotalPrice() {
        if (_binding == null) return
        val totalPrice = cartAdapter.getTotalPrice()
        binding.textTotalPrice.text = "Total: EGP${String.format("%.2f", totalPrice)}"
    }

    private fun showEmptyState() {
        if (_binding == null) return
        binding.recyclerView.visibility = GONE
        binding.imageView.visibility = VISIBLE
        binding.textView2.visibility = VISIBLE
        binding.textView3.visibility = VISIBLE
        binding.textView4.visibility = VISIBLE
        binding.totalContainer.visibility = GONE
    }

    private fun hideEmptyState() {
        if (_binding == null) return
        binding.recyclerView.visibility = VISIBLE
        binding.imageView.visibility = GONE
        binding.textView2.visibility = GONE
        binding.textView3.visibility = GONE
        binding.textView4.visibility = GONE
        binding.totalContainer.visibility = VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}