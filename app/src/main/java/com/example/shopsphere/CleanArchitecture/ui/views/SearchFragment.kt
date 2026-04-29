package com.example.shopsphere.CleanArchitecture.ui.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.navigation.fragment.findNavController
import com.example.shopsphere.CleanArchitecture.ui.adapters.SearchAdapter
import com.example.shopsphere.CleanArchitecture.ui.viewmodels.SearchViewModel
import com.example.shopsphere.databinding.FragmentSearchBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!
    private val searchViewModel: SearchViewModel by viewModels()

    private val searchAdapter by lazy {
        SearchAdapter(
            onItemClick = { productId ->
                findNavController().navigate(SearchFragmentDirections.actionSearchFragmentToDetailsFragment(productId))
            }
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeSearchResults()
        observeLoading()
        onClicks()
        setupSearchInput()
    }

    private fun setupSearchInput() {
        binding.textSearch.doAfterTextChanged { text ->
            val query = text?.toString().orEmpty()
            if (query.isNotEmpty()) {
                searchViewModel.searchProducts(query)
                showSearchResults()
            } else {
                searchViewModel.clearResults()
                binding.recyclerSearchResults.visibility = View.GONE
                hideEmptyState()
            }
        }
    }

    private fun setupRecyclerView() {
        binding.recyclerSearchResults.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerSearchResults.adapter = searchAdapter
    }

    private fun observeSearchResults() {
        searchViewModel.searchResults.observe(viewLifecycleOwner) { results ->
            searchAdapter.submitList(results)
            if (results.isNullOrEmpty() && binding.textSearch.text?.isNotEmpty() == true) {
                showEmptyState()
            } else {
                hideEmptyState()
            }
        }
    }

    private fun observeLoading() {
        searchViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (_binding == null) return@observe
            binding.progressBarSearch.visibility = if (isLoading) View.VISIBLE else View.GONE
            // While loading, hide the recycler so the spinner sits centered
            if (isLoading) binding.recyclerSearchResults.visibility = View.GONE
        }
    }

    private fun showSearchResults() {
        binding.recyclerSearchResults.visibility = View.VISIBLE
        hideEmptyState()
    }

    private fun onClicks() {
        binding.btnBack.setOnClickListener {
            if (!findNavController().navigateUp()) {
                findNavController().popBackStack()
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