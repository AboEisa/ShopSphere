package com.example.shopsphere.CleanArchitecture.ui.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.shopsphere.CleanArchitecture.ui.adapters.HomeProductsAdapter
import com.example.shopsphere.CleanArchitecture.ui.adapters.ShimmerHomeAdapter
import com.example.shopsphere.CleanArchitecture.ui.adapters.TypesAdapter
import com.example.shopsphere.CleanArchitecture.ui.viewmodels.SavedViewModel
import com.example.shopsphere.CleanArchitecture.utils.Constant
import com.example.shopsphere.CleanArchitecture.utils.Constant.Companion.TYPES_LIST
import com.example.shopsphere.databinding.FragmentHomeBinding
import com.example.yourpackage.viewmodels.HomeViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val productsViewModel: HomeViewModel by viewModels()
    private val favoriteViewModel: SavedViewModel by viewModels()
    private val shimmerAdapter by lazy { ShimmerHomeAdapter() }
    private val typesList = ArrayList(TYPES_LIST)
    private var selectedType: String = typesList.first()
    private val adapterTypes: TypesAdapter by lazy { TypesAdapter() }

    private val productsAdapter by lazy {
        HomeProductsAdapter(
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
            onItemClick = { productId ->
                val action = HomeFragmentDirections.actionHomeFragmentToDetailsFragment(productId)
                findNavController().navigate(action)
            }
        )
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupTypeAdapter()
        setupSearchClick()
        setupFilterClick()
        observeProducts()
        observeFilteredProducts()
        observeAllProductsAfterFilter()
        fetchProductsBasedOnType(selectedType)
    }



    private fun setupRecyclerView() {
        binding.recyclerProducts.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.recyclerProducts.adapter = productsAdapter
    }

    private fun setupTypeAdapter() {
        adapterTypes.list = typesList
        binding.recyclerTypes.adapter = adapterTypes
        adapterTypes.notifyDataSetChanged()
        adapterTypes.onTypeClick = { type ->
            selectedType = type
            fetchProductsBasedOnType(selectedType)
        }
    }

    private fun setupSearchClick() {
        binding.textSearch.setOnClickListener {
            val action = HomeFragmentDirections.actionHomeFragmentToSearchFragment()
            findNavController().navigate(action)
        }
    }

    private fun setupFilterClick() {
        binding.filter.setOnClickListener {
            val bottomSheet = FilterBottomSheetFragment()
            bottomSheet.show(childFragmentManager, "FilterBottomSheet")
        }

    }

    private fun fetchProductsBasedOnType(type: String) {
        showShimmer()
        binding.noResult.visibility = View.GONE
        lifecycleScope.launch {
            if (type.equals(TYPES_LIST.first(), ignoreCase = true)) {
                productsViewModel.fetchProducts()
            } else {
                productsViewModel.fetchProductsByCategory(type.lowercase())
            }
        }
    }

    private fun observeProducts() {
        productsViewModel.productsLiveData.observe(viewLifecycleOwner) { products ->
            viewLifecycleOwner.lifecycleScope.launch {
                delay(1000)
                if (view != null && _binding != null) { // defensive check
                    if (products.isNotEmpty()) {
                        productsAdapter.products = products.toMutableList()
                        binding.noResult.visibility = View.GONE
                    } else {
                        productsAdapter.products.clear()
                        binding.noResult.visibility = View.VISIBLE
                    }
                    productsAdapter.notifyDataSetChanged()
                    hideShimmerAndShowProducts()
                }
            }
        }
    }

    private fun observeFilteredProducts() {
        productsViewModel.filteredProductsLiveData.observe(viewLifecycleOwner) { filteredList ->
            if (filteredList != null) {
                productsAdapter.products = filteredList.toMutableList()
                productsAdapter.notifyDataSetChanged()
                binding.noResult.visibility = if (filteredList.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun observeAllProductsAfterFilter() {
        productsViewModel.productsLiveData.observe(viewLifecycleOwner) { products ->
            if (productsViewModel.filteredProductsLiveData.value.isNullOrEmpty()) {
                productsAdapter.products = products.toMutableList()
                productsAdapter.notifyDataSetChanged()
                binding.noResult.visibility = if (products.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun showShimmer() {
        binding.recyclerProducts.adapter = shimmerAdapter
    }

    private fun hideShimmerAndShowProducts() {
        binding.recyclerProducts.adapter = productsAdapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}