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
    private val productId = 0


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

            val items = cartAdapter.getItems()
            if (items.isEmpty()) {
                Toast.makeText(
                    requireContext(),
                    getString(com.example.shopsphere.R.string.validation_cart_empty),
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val invalidStockItem = items.firstOrNull { item ->
                val stock = item.rating.count.coerceAtLeast(0)
                val quantity = item.quantity.coerceAtLeast(1)
                quantity > stock || stock <= 0
            }
            if (invalidStockItem != null) {
                val stock = invalidStockItem.rating.count.coerceAtLeast(0)
                val message = if (stock <= 0) {
                    getString(
                        com.example.shopsphere.R.string.validation_product_out_of_stock,
                        invalidStockItem.title
                    )
                } else {
                    getString(
                        com.example.shopsphere.R.string.validation_product_stock_exceeded,
                        invalidStockItem.title,
                        stock
                    )
                }
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            sharedCartViewModel.setCartItems(items)
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
            },
            onStockLimitReached = { productTitle, stock ->
                if (!isAdded || _binding == null) return@CartAdapter
                val message = if (stock <= 0) {
                    getString(com.example.shopsphere.R.string.validation_product_out_of_stock, productTitle)
                } else {
                    getString(
                        com.example.shopsphere.R.string.validation_product_stock_exceeded,
                        productTitle,
                        stock
                    )
                }
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            }
        )
        binding.recyclerView.adapter = cartAdapter
        binding.recyclerView.itemAnimator = null
    }

    private fun observeCartItems() {
        cartViewModel.cartProducts.observe(viewLifecycleOwner) { cartProducts ->
            if (!isAdded || _binding == null) return@observe
            if (cartProducts != null) {
                val normalizedProducts = normalizeQuantitiesAgainstStock(cartProducts)
                cartAdapter.submitList(normalizedProducts)
                updateTotalPrice()
                binding.recyclerView.visibility = if (normalizedProducts.isEmpty()) View.GONE else View.VISIBLE
                updateEmptyStateVisibility(normalizedProducts.isEmpty())
            }
        }
    }

    private fun normalizeQuantitiesAgainstStock(
        products: List<PresentationProductResult>
    ): List<PresentationProductResult> {
        return products.mapNotNull { product ->
            val stock = product.rating.count.coerceAtLeast(0)
            val quantity = product.quantity.coerceAtLeast(1)
            when {
                stock <= 0 -> {
                    cartViewModel.removeFromCart(product.id)
                    null
                }

                quantity > stock -> {
                    cartViewModel.updateQuantity(product.id, stock)
                    product.copy(quantity = stock)
                }

                else -> product
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
        binding.textTotalPrice.text = "Total: EGP ${String.format("%.2f", totalPrice)}"
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
