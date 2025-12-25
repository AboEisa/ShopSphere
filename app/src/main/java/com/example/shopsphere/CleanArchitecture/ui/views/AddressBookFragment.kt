package com.example.shopsphere.CleanArchitecture.ui.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.shopsphere.CleanArchitecture.ui.adapters.AddressBookAdapter
import com.example.shopsphere.CleanArchitecture.ui.viewmodels.CheckoutSharedViewModel
import com.example.shopsphere.CleanArchitecture.utils.showSuccessDialog
import com.example.shopsphere.R
import com.example.shopsphere.databinding.FragmentAddressBookBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AddressBookFragment : Fragment() {

    private var _binding: FragmentAddressBookBinding? = null
    private val binding get() = _binding!!

    private val sharedViewModel: CheckoutSharedViewModel by activityViewModels()
    private val addressAdapter by lazy {
        AddressBookAdapter { item -> sharedViewModel.selectAddress(item.id) }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddressBookBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.recyclerAddresses.adapter = addressAdapter

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        binding.btnUseAddress.setOnClickListener {
            showSuccessDialog(
                title = getString(R.string.dialog_address_selected_title),
                message = getString(R.string.dialog_address_selected_message)
            ) {
                findNavController().navigateUp()
            }
        }
        binding.btnAddNewAddress.setOnClickListener {
            findNavController().navigate(R.id.mapsFragment)
        }

        parentFragmentManager.setFragmentResultListener("location_result", viewLifecycleOwner) { _, bundle ->
            val title = bundle.getString("nickname").orEmpty()
            val fullAddress = bundle.getString("fullname").orEmpty()
            val lat = bundle.getDouble("lat")
            val lng = bundle.getDouble("lng")
            if (title.isNotBlank() && fullAddress.isNotBlank()) {
                val wasSaved = sharedViewModel.setAddress(title, fullAddress, lat, lng)
                if (wasSaved) {
                    showSuccessDialog(
                        title = getString(R.string.dialog_address_success_title),
                        message = getString(R.string.dialog_address_success_message)
                    )
                } else {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.validation_address_invalid),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.validation_address_invalid),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        sharedViewModel.addressBook.observe(viewLifecycleOwner) { addresses ->
            addressAdapter.submitList(addresses)
            binding.emptyAddressState.visibility = if (addresses.isEmpty()) View.VISIBLE else View.GONE
            binding.btnUseAddress.isEnabled = addresses.any { it.isSelected }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
