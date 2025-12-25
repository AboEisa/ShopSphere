package com.example.shopsphere.CleanArchitecture.ui.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.example.shopsphere.databinding.FragmentFilterBottomSheetBinding
import com.example.yourpackage.viewmodels.HomeViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FilterBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentFilterBottomSheetBinding? = null
    private val binding get() = _binding!!
    // SHARE the VM with parent fragment (HomeFragment)
    private val productsViewModel: HomeViewModel by viewModels({ requireParentFragment() })

    private var minPrice = 10f
    private var maxPrice = 50f

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentFilterBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRangeSlider()
        onCLicks()
    }

    private fun setupRangeSlider() {
        binding.priceRangeSlider.values = listOf(minPrice, maxPrice)
        updatePriceLabels(minPrice, maxPrice)

        binding.priceRangeSlider.addOnChangeListener { slider, _, _ ->
            val values = slider.values
            minPrice = values[0]
            maxPrice = values[1]
            updatePriceLabels(minPrice, maxPrice)
        }
    }

    private fun updatePriceLabels(min: Float, max: Float) {
        binding.tvMinPrice.text = "$${min.toInt()}"
        binding.tvMaxPrice.text = "$${max.toInt()}"
    }

    private fun onCLicks() {
        binding.ivClose.setOnClickListener {
            dismiss()
        }
        binding.addToCartButton.setOnClickListener {
            handleFilter()
        }
        binding.btnResetFilter.setOnClickListener {
            resetFilters()
        }
    }

    private fun handleFilter() {
        productsViewModel.filterProducts(minPrice, maxPrice)
        dismiss()
    }

    fun resetFilters() {
        productsViewModel.clearPriceFilter()
        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
