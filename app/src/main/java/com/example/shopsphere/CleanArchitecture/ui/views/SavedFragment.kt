package com.example.shopsphere.CleanArchitecture.ui.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.shopsphere.CleanArchitecture.ui.adapters.SavedAdapter
import com.example.shopsphere.CleanArchitecture.ui.viewmodels.SavedViewModel
import com.example.shopsphere.R
import com.example.shopsphere.databinding.FragmentHomeBinding
import com.example.shopsphere.databinding.FragmentSavedBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking

@AndroidEntryPoint
class SavedFragment : Fragment() {

    private var _binding: FragmentSavedBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: SavedAdapter
    val viewModel: SavedViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSavedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeViewModel()
        onClicks()
    }

    fun onClicks(){
       binding.btnBack.setOnClickListener {
           findNavController().navigateUp()
       }
    }

    fun setupRecyclerView(){

        adapter = SavedAdapter(
            onItemClick = { productId ->
                val action = SavedFragmentDirections.actionSavedFragmentToDetailsFragment2(
                    productId
                )
                findNavController().navigate(action)

            }
            ,
            onFavoriteClick = { product ->
                viewModel.toggleFavorite(product.id)
            },
            isFavorite = { productId ->
                runBlocking { viewModel.isFavorite(productId) }
            }
        )
        binding.recyclerView.layoutManager = GridLayoutManager(requireContext(),2)
        binding.recyclerView.adapter = adapter
        viewModel.loadFavoriteProducts()
    }



    private fun observeViewModel() {
        viewModel.favoriteProducts.observe(viewLifecycleOwner) { products ->
            adapter.submitList(products)
            if (products.isEmpty()) {
                showEmptyState()
            } else {
                hideEmptyState()
            }
        }
        viewModel.loading.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading) {
                binding.progressBar.visibility = View.VISIBLE
            } else {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun showEmptyState() {

        binding.imageView.visibility = View.VISIBLE
        binding.textView2.visibility = View.VISIBLE
        binding.textView3.visibility = View.VISIBLE
        binding.textView4.visibility = View.VISIBLE
    }

    private fun hideEmptyState() {

        binding.imageView.visibility = View.GONE
        binding.textView2.visibility = View.GONE
        binding.textView3.visibility = View.GONE
        binding.textView4.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
