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
import com.example.shopsphere.databinding.FragmentMyDetailsBinding
import dagger.hilt.android.AndroidEntryPoint

/**
 * My Details screen.
 *
 * Displays all 5 fields from GET /MyDetails: firstName, lastName, email, phone, address.
 * One Edit button → makes all fields editable.
 * One Save button → submits all 5 fields to PUT /UpdateMyDetails.
 */
@AndroidEntryPoint
class MyDetailsFragment : Fragment() {

    private var _binding: FragmentMyDetailsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AccountViewModel by viewModels()

    private var isEditMode = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyDetailsBinding.inflate(inflater, container, false)
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
    // Observers
    // ─────────────────────────────────────────────────────────────────────────

    private fun observeViewModel() {
        viewModel.profileState.observe(viewLifecycleOwner) { state ->
            if (!isEditMode) {
                binding.editFirstName.setText(state.firstName)
                binding.editLastName.setText(state.lastName)
                binding.editEmail.setText(state.email)
                binding.editPhone.setText(state.phone)
                binding.editAddress.setText(state.address)
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
                    )
                }
                is AccountUiEvent.PartialSaveError -> {
                    setEditMode(false)
                    showSuccessDialog(
                        title   = getString(R.string.account_changes_saved),
                        message = getString(R.string.my_details_partial_save_warning, event.message)
                    )
                }
                is AccountUiEvent.ValidationError -> {
                    when (event.field) {
                        "firstName" -> binding.editFirstName.error = event.message
                        "email"     -> binding.editEmail.error     = event.message
                    }
                }
                is AccountUiEvent.Error -> {
                    showSuccessDialog(
                        title   = getString(R.string.dialog_error_title),
                        message = event.message
                    )
                }
                else -> { /* handled elsewhere */ }
            }
            viewModel.clearEvent()
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Click listeners
    // ─────────────────────────────────────────────────────────────────────────

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        binding.btnNotifications.setOnClickListener {
            findNavController().navigate(R.id.notificationsFragment)
        }

        // Edit button: toggle edit mode, or cancel and revert
        binding.btnEdit.setOnClickListener {
            if (isEditMode) {
                // Cancel — restore last server values
                setEditMode(false)
                viewModel.profileState.value?.let { state ->
                    binding.editFirstName.setText(state.firstName)
                    binding.editLastName.setText(state.lastName)
                    binding.editEmail.setText(state.email)
                    binding.editPhone.setText(state.phone)
                    binding.editAddress.setText(state.address)
                }
            } else {
                setEditMode(true)
            }
        }

        // Save button — submits all 5 fields
        binding.buttonSubmit.setOnClickListener {
            viewModel.saveMyDetails(
                firstName = binding.editFirstName.text.toString(),
                lastName  = binding.editLastName.text.toString(),
                email     = binding.editEmail.text.toString(),
                phone     = binding.editPhone.text.toString(),
                address   = binding.editAddress.text.toString()
            )
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Edit mode toggle
    // ─────────────────────────────────────────────────────────────────────────

    private fun setEditMode(edit: Boolean) {
        isEditMode = edit

        listOf(
            binding.editFirstName,
            binding.editLastName,
            binding.editEmail,
            binding.editPhone,
            binding.editAddress
        ).forEach { field ->
            field.isEnabled             = edit
            field.isFocusable           = edit
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
