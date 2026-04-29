package com.example.shopsphere.CleanArchitecture.ui.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.shopsphere.CleanArchitecture.data.local.RecentlyViewedStore
import com.example.shopsphere.CleanArchitecture.ui.adapters.HomeProductsAdapter
import com.example.shopsphere.CleanArchitecture.ui.adapters.ShimmerHomeAdapter
import com.example.shopsphere.CleanArchitecture.ui.adapters.TypesAdapter
import com.example.shopsphere.CleanArchitecture.ui.viewmodels.CartViewModel
import com.example.shopsphere.CleanArchitecture.ui.viewmodels.SavedViewModel
import com.example.shopsphere.databinding.FragmentHomeBinding
import com.example.yourpackage.viewmodels.HomeViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.abs

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val productsViewModel: HomeViewModel by viewModels()
    private val favoriteViewModel: SavedViewModel by activityViewModels()
    private val shimmerAdapter by lazy { ShimmerHomeAdapter() }
    private val typesList = arrayListOf(HomeViewModel.ALL_CATEGORY)
    private var selectedType: String = HomeViewModel.ALL_CATEGORY
    private val adapterTypes: TypesAdapter by lazy { TypesAdapter() }

    private val cartViewModel: CartViewModel by activityViewModels()

    @Inject
    lateinit var recentlyViewedStore: RecentlyViewedStore

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
                favoriteViewModel.toggleFavorite(productId)
            },
            isFavorite = { productId ->
                favoriteViewModel.isFavoriteSync(productId)
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
        // Subscribe to favorites BEFORE products so by the time the product list
        // commit fires onCurrentListChanged → PAYLOAD_FAV rebind, the adapter's
        // favoriteIds set is already the canonical value from SavedViewModel.
        observeInterestSignals()
        observeProducts()
        // Avoid a blank-white flash on every Home revisit: if the ViewModel
        // already has cached products (survives config change / back-nav), we
        // skip the explicit refetch. HomeViewModel.init() does the first load.
        if (productsViewModel.productsLiveData.value.isNullOrEmpty()) {
            fetchProductsBasedOnType(selectedType)
        }

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

                        // Keep within screen bounds. Reserve the bottom 104dp for
                        // the floating nav (72dp height + 16dp bottom margin +
                        // 16dp gap) so the FAB can't be dragged behind it.
                        val density = resources.displayMetrics.density
                        val bottomReserve = (104f * density)
                        val minX = 0f
                        val maxX = binding.root.width.toFloat() - view.width
                        val minY = 0f
                        val maxY = binding.root.height.toFloat() - view.height - bottomReserve

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

    private fun observeInterestSignals() {
        favoriteViewModel.favoriteIds.observe(viewLifecycleOwner) { ids ->
            if (!isAdded || _binding == null) return@observe
            // Keep the adapter's heart state in sync with the canonical favorite
            // set so every visible row reflects the latest toggle — including
            // after scroll/refresh.
            productsAdapter.updateFavoriteIds(ids.orEmpty())
            pushInterestsToViewModel()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                recentlyViewedStore.ids.collect {
                    if (!isAdded || _binding == null) return@collect
                    pushInterestsToViewModel()
                }
            }
        }
    }

    private fun pushInterestsToViewModel() {
        val favIds = favoriteViewModel.favoriteIds.value.orEmpty()
        val recentIds = recentlyViewedStore.ids.value
        productsViewModel.setInterestSignals(favIds, recentIds)
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
            
            // Show modern loading overlay on first load
            if (loading == true) {
                val hasLoaded = productsViewModel.hasLoadedOnce.value == true
                if (!hasLoaded) {
                    // First time loading - show modern overlay
                    binding.loadingOverlay.loadingOverlay.visibility = View.VISIBLE
                    binding.loadingOverlay.loadingText.text = "Loading Products"
                    binding.loadingOverlay.loadingSubtitle.text = "Fetching the latest items for you"
                }
                binding.noResult.visibility = View.GONE
            } else {
                // Hide loading overlay
                binding.loadingOverlay.loadingOverlay.visibility = View.GONE
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
            productsAdapter.submitList(safeProducts)

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
            // Open Cart as a tab destination (not pushed on top of Home), so the
            // bottom nav's selected state stays in sync and tapping Home later pops
            // correctly.
            val navController = findNavController()
            val options = androidx.navigation.NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setRestoreState(true)
                .setPopUpTo(
                    navController.graph.startDestinationId,
                    inclusive = false,
                    saveState = true
                )
                .build()
            navController.navigate(com.example.shopsphere.R.id.cartFragment, null, options)
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
