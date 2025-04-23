package com.example.shopsphere.CleanArchitecture.ui.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.shopsphere.CleanArchitecture.ui.adapters.DetailsAdapter
import com.example.shopsphere.CleanArchitecture.ui.viewmodels.CartViewModel
import com.example.shopsphere.CleanArchitecture.ui.viewmodels.SavedViewModel
import com.example.shopsphere.databinding.FragmentDetailsBinding
import com.example.yourpackage.viewmodels.HomeViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@AndroidEntryPoint
class DetailsFragment : Fragment() {

    private var _binding: FragmentDetailsBinding? = null
    private val binding get() = _binding!!

    private val productsViewModel: HomeViewModel by viewModels()
    private val favoriteViewModel: SavedViewModel by viewModels()
    private val cartViewModel: CartViewModel by viewModels()
    private val args: DetailsFragmentArgs by navArgs()

    private val detailsAdapter by lazy {
        DetailsAdapter(
            onFavoriteClick = { productId ->
                lifecycleScope.launch {
                    if (favoriteViewModel.isFavorite(productId)) {
                        favoriteViewModel.removeFavoriteProduct(productId)
                    } else {
                        favoriteViewModel.addFavoriteProduct(productId)
                    }
                }
            },
            isFavorite = { productId ->
                runBlocking { favoriteViewModel.isFavorite(productId) }
            },
            onAddToCartClick = { productId ->
                val product = productsViewModel.productsLiveData.value?.find { it.id == productId }
                product?.let {
                    cartViewModel.addProductToCart(productId)
                    Toast.makeText(requireContext(), "Product added to cart", Toast.LENGTH_SHORT).show()
                }
            }
            ,
            isInCart = { productId ->
                cartViewModel.isInCart(productId)
            },
            removeFromCart = { productId ->
                cartViewModel.removeFromCart(productId)
                Toast.makeText(requireContext(), "Product removed from cart", Toast.LENGTH_SHORT).show()
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

        setupRecyclerView()
        observeProduct(args.productId)
        onClicks()
    }

    private fun setupRecyclerView() {
        binding.recyclerDetails.adapter = detailsAdapter
    }

    private fun observeProduct(productId: Int) {
        productsViewModel.productsLiveData.observe(viewLifecycleOwner) { products ->
            val selectedProduct = products.find { it.id == productId }
            selectedProduct?.let { product ->
                detailsAdapter.products = mutableListOf(product)
                detailsAdapter.notifyDataSetChanged()
            }
        }
    }


    private fun onClicks() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}