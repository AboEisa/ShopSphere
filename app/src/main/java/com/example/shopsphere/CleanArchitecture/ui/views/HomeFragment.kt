package com.example.shopsphere.CleanArchitecture.ui.views

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.shopsphere.CleanArchitecture.ui.adapters.HomeProductsAdapter
import com.example.shopsphere.CleanArchitecture.ui.adapters.ShimmerHomeAdapter
import com.example.shopsphere.CleanArchitecture.ui.adapters.TypesAdapter
import com.example.shopsphere.CleanArchitecture.ui.viewmodels.CartViewModel
import com.example.shopsphere.CleanArchitecture.ui.viewmodels.SavedViewModel
import com.example.shopsphere.databinding.FragmentHomeBinding
import com.example.yourpackage.viewmodels.HomeViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.math.abs

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val productsViewModel: HomeViewModel by viewModels()
    private val favoriteViewModel: SavedViewModel by viewModels()
    private val shimmerAdapter by lazy { ShimmerHomeAdapter() }
    private val typesList = arrayListOf(HomeViewModel.ALL_CATEGORY)
    private var selectedType: String = HomeViewModel.ALL_CATEGORY
    private val adapterTypes: TypesAdapter by lazy { TypesAdapter() }

    private val cartViewModel: CartViewModel by activityViewModels()

    // Variables for draggable FAB
    private var dX = 0f
    private var dY = 0f
    private var isDragging = false
    private var startX = 0f
    private var startY = 0f
    private val clickThreshold = 10f

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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupDraggableFab()
        setupFloatingCartButton()
        observeCartItems()
        setupRecyclerView()
        setupTypeAdapter()
        setupSearchClick()
        setupFilterClick()
        observeLoadingState()
        observeCategories()
        observeProducts()
        fetchProductsBasedOnType(selectedType)

        //  Initialize badge position
        updateBadgePosition()
    }

    private fun setupDraggableFab() {
        binding.fabCart.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    dX = view.x - event.rawX
                    dY = view.y - event.rawY
                    startX = event.rawX
                    startY = event.rawY
                    isDragging = false
                    return@setOnTouchListener true
                }

                MotionEvent.ACTION_MOVE -> {
                    val deltaX = abs(event.rawX - startX)
                    val deltaY = abs(event.rawY - startY)

                    if (!isDragging && (deltaX > clickThreshold || deltaY > clickThreshold)) {
                        isDragging = true
                        view.animate().scaleX(1.15f).scaleY(1.15f).setDuration(200).start()
                    }

                    if (isDragging) {
                        var newX = event.rawX + dX
                        var newY = event.rawY + dY

                        // Keep within screen bounds
                        val minX = 0f
                        val maxX = binding.root.width.toFloat() - view.width
                        val minY = 0f
                        val maxY = binding.root.height.toFloat() - view.height

                        newX = newX.coerceIn(minX, maxX)
                        newY = newY.coerceIn(minY, maxY)

                        view.x = newX
                        view.y = newY

                        //  Update badge position while dragging
                        updateBadgePosition()

                        return@setOnTouchListener true
                    }
                }

                MotionEvent.ACTION_UP -> {
                    if (isDragging) {
                        isDragging = false
                        view.animate().scaleX(1f).scaleY(1f).setDuration(200).start()
                        snapToEdge(view)
                        return@setOnTouchListener true
                    } else {
                        val deltaX = abs(event.rawX - startX)
                        val deltaY = abs(event.rawY - startY)

                        if (deltaX < clickThreshold && deltaY < clickThreshold) {
                            binding.fabCart.performClick()
                        }
                        return@setOnTouchListener true
                    }
                }
            }
            false
        }
    }


    private fun updateBadgePosition() {
        if (_binding == null) return
        val fabX = binding.fabCart.x
        val fabY = binding.fabCart.y
        val fabWidth = binding.fabCart.width
        val badgeWidth = binding.tvCartBadge.width

        // Position badge at top-right corner of FAB
        binding.tvCartBadge.x = fabX + fabWidth - badgeWidth + 4 // +4 for slight overlap
        binding.tvCartBadge.y = fabY - 4 // -4 to position slightly above
    }

    private fun snapToEdge(view: View) {
        val screenWidth = binding.root.width
        val fabWidth = view.width
        val currentX = view.x

        val toLeftEdge = currentX
        val toRightEdge = screenWidth - (currentX + fabWidth)

        val targetX = if (toLeftEdge < toRightEdge) {
            16f
        } else {
            (screenWidth - fabWidth - 16).toFloat()
        }

        view.animate()
            .x(targetX)
            .setDuration(300)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                // Update badge position after snap animation
                updateBadgePosition()
            }
            .start()
    }

    private fun setupRecyclerView() {
        binding.recyclerProducts.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.recyclerProducts.adapter = productsAdapter
    }

    private fun setupTypeAdapter() {
        adapterTypes.submitTypes(typesList, selectedType)
        binding.recyclerTypes.adapter = adapterTypes
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
        binding.noResult.visibility = View.GONE
        if (type.equals(HomeViewModel.ALL_CATEGORY, ignoreCase = true)) {
            productsViewModel.fetchProducts()
        } else {
            productsViewModel.fetchProductsByCategory(type)
        }
    }

    private fun observeLoadingState() {
        productsViewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            if (!isAdded || _binding == null) return@observe
            if (loading == true) {
                showShimmer()
                binding.noResult.visibility = View.GONE
            } else {
                hideShimmerAndShowProducts()
                val hasLoaded = productsViewModel.hasLoadedOnce.value == true
                val visibleProducts = productsViewModel.productsLiveData.value.orEmpty()
                binding.noResult.visibility =
                    if (hasLoaded && visibleProducts.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun observeCategories() {
        productsViewModel.categoriesLiveData.observe(viewLifecycleOwner) { categories ->
            if (!isAdded || _binding == null) return@observe
            val incoming = categories.orEmpty()
            if (incoming.isEmpty()) return@observe

            val normalizedCurrent = selectedType.trim()
            if (incoming.none { it.equals(normalizedCurrent, ignoreCase = true) }) {
                selectedType = incoming.first()
            }

            typesList.clear()
            typesList.addAll(incoming)
            adapterTypes.submitTypes(typesList, selectedType)
        }
    }

    private fun observeProducts() {
        productsViewModel.productsLiveData.observe(viewLifecycleOwner) { products ->
            if (!isAdded || _binding == null) return@observe
            val safeProducts = products.orEmpty()
            productsAdapter.products = safeProducts.toMutableList()
            productsAdapter.notifyDataSetChanged()

            val loading = productsViewModel.isLoading.value == true
            val hasLoaded = productsViewModel.hasLoadedOnce.value == true
            binding.noResult.visibility =
                if (!loading && hasLoaded && safeProducts.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun showShimmer() {
        binding.recyclerProducts.adapter = shimmerAdapter
    }

    private fun hideShimmerAndShowProducts() {
        binding.recyclerProducts.adapter = productsAdapter
    }

    private fun setupFloatingCartButton() {
        binding.fabCart.setOnClickListener {
            val action = HomeFragmentDirections.actionHomeFragmentToCartFragment()
            findNavController().navigate(action)
        }
    }

    private fun observeCartItems() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                cartViewModel.cartItemCount.collect { count ->
                    updateCartBadge(count)
                }
            }
        }
    }

    private fun updateCartBadge(count: Int) {
        if (_binding == null) return
        if (count > 0) {
            // Show FAB when cart has items
            binding.fabCart.show()
            binding.tvCartBadge.visibility = View.VISIBLE
            binding.tvCartBadge.text = when {
                count > 99 -> "99+"
                else -> count.toString()
            }
            binding.tvCartBadge.post {
                updateBadgePosition()
            }
        } else {
            binding.fabCart.hide()
            binding.tvCartBadge.visibility = View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        cartViewModel.refreshCartCount()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
