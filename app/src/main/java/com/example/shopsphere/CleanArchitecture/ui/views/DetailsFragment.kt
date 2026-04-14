package com.example.shopsphere.CleanArchitecture.ui.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.shopsphere.R
import com.example.shopsphere.CleanArchitecture.ui.adapters.DetailsAdapter
import com.example.shopsphere.CleanArchitecture.ui.viewmodels.CartViewModel
import com.example.shopsphere.CleanArchitecture.ui.viewmodels.DetailsViewModel
import com.example.shopsphere.CleanArchitecture.ui.viewmodels.SavedViewModel
import com.example.shopsphere.databinding.FragmentDetailsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DetailsFragment : Fragment() {

    private var _binding: FragmentDetailsBinding? = null
    private val binding get() = _binding!!

    private val detailsViewModel: DetailsViewModel by viewModels()
    private val favoriteViewModel: SavedViewModel by viewModels()
    private val args: DetailsFragmentArgs by navArgs()

    // FIX: Single activity-scoped instance — shares state with HomeFragment badge
    // and CartFragment list. The old local `by viewModels()` was a separate instance
    // with empty cartProducts, causing addProductToCart to always use stale state.
    private val cartViewModel: CartViewModel by activityViewModels()

    private val detailsAdapter by lazy {
        DetailsAdapter(
            onFavoriteClick = { productId ->
                if (!isAdded || _binding == null) return@DetailsAdapter
                favoriteViewModel.toggleFavorite(productId)
            },
            isFavorite = { productId ->
                if (!isAdded || _binding == null) return@DetailsAdapter false
                favoriteViewModel.isFavoriteSync(productId)
            },
            onAddToCartClick = { productId, size ->
                if (!isAdded || _binding == null) return@DetailsAdapter
                try {
                    val product = detailsViewModel.productLiveData.value
                    if (product == null) {
                        context?.let { ctx ->
                            Toast.makeText(ctx, "Product not loaded yet, please try again", Toast.LENGTH_SHORT).show()
                        }
                        return@DetailsAdapter
                    }
                    val stock = product.stock.coerceAtLeast(0)
                    lifecycleScope.launch {
                        val added = cartViewModel.addProductToCart(productId, size, stock)
                        cartViewModel.refreshCartCount()
                        if (isAdded && _binding != null) {
                            context?.let { ctx ->
                                if (added) {
                                    Toast.makeText(ctx, "Product added to cart", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(ctx, "Could not add to cart, please try again", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    context?.let { ctx ->
                        Toast.makeText(ctx, "Something went wrong", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            isInCart = { productId, size ->
                if (!isAdded || _binding == null) return@DetailsAdapter false
                try {
                    cartViewModel.isInCart(productId, size)
                } catch (e: Exception) {
                    false
                }
            },
            removeFromCart = { productId, size ->
                if (!isAdded || _binding == null) return@DetailsAdapter
                try {
                    cartViewModel.removeFromCart(productId, size)
                    cartViewModel.refreshCartCount()
                    if (isAdded && _binding != null) {
                        context?.let { ctx ->
                            Toast.makeText(ctx, "Product removed from cart", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    context?.let { ctx ->
                        Toast.makeText(ctx, "Something went wrong", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onViewReviews = { productId ->
                if (!isAdded || _binding == null) return@DetailsAdapter
                val action = DetailsFragmentDirections.actionDetailsFragmentToReviewsFragment(productId)
                findNavController().navigate(action)
            },
            onOutOfStockClick = { title ->
                if (!isAdded || _binding == null) return@DetailsAdapter
                context?.let { ctx ->
                    Toast.makeText(
                        ctx,
                        ctx.getString(R.string.validation_product_out_of_stock, title),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (_binding == null) return
        setupRecyclerView()
        detailsViewModel.fetchProductById(args.productId)
        observeProduct()
        onClicks()
    }

    private fun setupRecyclerView() {
        if (_binding == null) return
        binding.recyclerDetails.adapter = detailsAdapter
    }

    private fun observeProduct() {
        detailsViewModel.productLiveData.observe(viewLifecycleOwner) { product ->
            if (!isAdded || _binding == null) return@observe
            product?.let {
                detailsAdapter.products = mutableListOf(it)
                detailsAdapter.notifyDataSetChanged()
            }
        }
    }

    private fun onClicks() {
        if (_binding == null) return
        binding.btnBack.setOnClickListener {
            if (!isAdded || _binding == null) return@setOnClickListener
            findNavController().navigateUp()
        }
        binding.btnNotifications.setOnClickListener {
            if (!isAdded || _binding == null) return@setOnClickListener
            findNavController().navigate(R.id.notificationsFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}