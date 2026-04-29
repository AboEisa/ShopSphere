package com.example.shopsphere.CleanArchitecture.ui.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.shopsphere.CleanArchitecture.ui.viewmodels.AccountUiEvent
import com.example.shopsphere.CleanArchitecture.ui.viewmodels.AccountViewModel
import com.example.shopsphere.CleanArchitecture.utils.showSuccessDialog
import com.example.shopsphere.R
import com.example.shopsphere.databinding.FragmentAddressPhoneBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * Address & Phone screen.
 *
 * Displays data from GET /MyDetails (address, phone).
 * Starts read-only; tapping the edit icon enables the two fields.
 * Save submits to PUT /UpdateMyAddress&Phone { Address, Phone }.
 */
@AndroidEntryPoint
class AddressPhoneFragment : Fragment() {

    private var _binding: FragmentAddressPhoneBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AccountViewModel by viewModels()

    private var isEditMode = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddressPhoneBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setEditMode(false)
        setupClickListeners()
        observeViewModel()
        viewModel.refreshFromServer()
    }

    // ─────────────────────────────────────────────────────────────────────────

    private fun observeViewModel() {
        viewModel.profileState.observe(viewLifecycleOwner) { state ->
            if (!isEditMode) {
                binding.editAddress.setText(state.address)
                binding.editPhone.setText(state.phone)
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.buttonSubmit.isEnabled = !loading
            binding.buttonSubmit.alpha     = if (loading) 0.6f else 1.0f
        }

        viewModel.uiEvent.observe(viewLifecycleOwner) { event ->
            event ?: return@observe
            when (event) {
                is AccountUiEvent.SaveSuccess -> {
                    setEditMode(false)
                    showSuccessDialog(
                        title   = getString(R.string.account_changes_saved),
                        message = getString(R.string.account_changes_saved)
                    ) { findNavController().navigateUp() }
                }
                is AccountUiEvent.PartialSaveError -> {
                    setEditMode(false)
                    showSuccessDialog(
                        title   = getString(R.string.account_changes_saved),
                        message = getString(R.string.my_details_partial_save_warning, event.message)
                    ) { findNavController().navigateUp() }
                }
                is AccountUiEvent.ValidationError -> {
                    when (event.field) {
                        "address" -> binding.editAddress.error = event.message
                        "phone"   -> binding.editPhone.error   = event.message
                    }
                }
                is AccountUiEvent.Error -> {
                    showSuccessDialog(
                        title   = getString(R.string.dialog_error_title),
                        message = event.message
                    )
                }
                else -> { /* not relevant here */ }
            }
            viewModel.clearEvent()
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        binding.btnNotifications.setOnClickListener {
            findNavController().navigate(R.id.notificationsFragment)
        }

        binding.btnEdit.setOnClickListener {
            if (isEditMode) {
                // Cancel — restore last known values
                setEditMode(false)
                viewModel.profileState.value?.let { state ->
                    binding.editAddress.setText(state.address)
                    binding.editPhone.setText(state.phone)
                }
            } else {
                setEditMode(true)
            }
        }

        binding.buttonSubmit.setOnClickListener {
            // Pass current firstName/lastName/email from state unchanged; only update phone/address
            val state = viewModel.profileState.value
            viewModel.saveMyDetails(
                firstName = state?.firstName.orEmpty(),
                lastName  = state?.lastName.orEmpty(),
                email     = state?.email.orEmpty(),
                phone     = binding.editPhone.text.toString(),
                address   = binding.editAddress.text.toString()
            )
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    private fun setEditMode(edit: Boolean) {
        isEditMode = edit

        listOf(binding.editAddress, binding.editPhone).forEach { field ->
            field.isEnabled = edit
            field.isFocusable = edit
            field.isFocusableInTouchMode = edit
        }

        binding.buttonSubmit.visibility = if (edit) View.VISIBLE else View.GONE
        binding.btnEdit.setImageResource(
            if (edit) R.drawable.ic_remove else R.drawable.ic_edit
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
