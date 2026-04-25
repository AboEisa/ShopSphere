package com.example.shopsphere.CleanArchitecture.ui.views

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.shopsphere.CleanArchitecture.data.local.SharedPreference
import com.example.shopsphere.CleanArchitecture.utils.showSuccessDialog
import com.example.shopsphere.R
import com.example.shopsphere.databinding.FragmentMyDetailsBinding
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class MyDetailsFragment : Fragment() {

    private var _binding: FragmentMyDetailsBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var sharedPreference: SharedPreference

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupGenderField()
        bindUserData()
        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        binding.btnNotifications.setOnClickListener {
            findNavController().navigate(R.id.notificationsFragment)
        }
        binding.buttonOpenDatePicker.setOnClickListener { openDatePicker() }
        binding.editBirthDate.setOnClickListener { openDatePicker() }
        binding.buttonSubmit.setOnClickListener {
            saveProfileChanges()
        }
    }

    private fun setupGenderField() {
        val options = listOf(
            getString(R.string.my_details_gender_male),
            getString(R.string.my_details_gender_female),
            getString(R.string.my_details_gender_other)
        )
        binding.editGender.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, options)
        )
        binding.editGender.setOnClickListener { binding.editGender.showDropDown() }
        binding.editGender.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) binding.editGender.showDropDown()
        }
    }

    private fun bindUserData() {
        val localName = sharedPreference.getProfileName()
        val localEmail = sharedPreference.getProfileEmail()
        val localPhone = sharedPreference.getProfilePhone()
        val localBirthDate = sharedPreference.getProfileBirthDate()
        val localGender = sharedPreference.getProfileGender()

        val resolvedName = localName.ifBlank { deriveNameFromEmail(localEmail).orEmpty() }

        binding.editFullName.setText(resolvedName)
        binding.editEmail.setText(localEmail)
        binding.editPhone.setText(localPhone)
        binding.editBirthDate.setText(localBirthDate)
        binding.editGender.setText(localGender, false)

        if (resolvedName.isNotBlank() && localName.isBlank()) {
            sharedPreference.saveProfile(
                name = resolvedName,
                email = localEmail,
                phone = localPhone
            )
        }
    }

    private fun deriveNameFromEmail(email: String): String? {
        val local = email.substringBefore('@', "").trim()
        if (local.isBlank()) return null
        return local.split('.', '_', '-', '+')
            .map { chunk -> chunk.trimEnd { it.isDigit() } }
            .filter { it.isNotBlank() }
            .joinToString(" ") { chunk -> chunk.replaceFirstChar { c -> c.uppercaseChar() } }
            .ifBlank { null }
    }

    private fun saveProfileChanges() {
        val name = binding.editFullName.text.toString().trim()
        val email = binding.editEmail.text.toString().trim()
        val phone = binding.editPhone.text.toString().trim()
        val birthDate = binding.editBirthDate.text.toString().trim()
        val gender = binding.editGender.text.toString().trim()

        if (name.isBlank()) {
            binding.editFullName.error = getString(R.string.my_details_full_name)
            return
        }
        if (email.isBlank()) {
            binding.editEmail.error = getString(R.string.my_details_email)
            return
        }
        if (birthDate.isBlank()) {
            binding.editBirthDate.error = getString(R.string.my_details_birth_date)
            return
        }
        if (gender.isBlank()) {
            binding.editGender.error = getString(R.string.my_details_gender)
            return
        }

        sharedPreference.saveProfile(name = name, email = email, phone = phone)
        sharedPreference.saveProfileExtras(birthDate = birthDate, gender = gender)

        showSuccessDialog(
            title = getString(R.string.account_changes_saved),
            message = getString(R.string.account_changes_saved)
        ) {
            findNavController().navigateUp()
        }
    }

    private fun openDatePicker() {
        val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)
        val calendar = Calendar.getInstance()
        runCatching {
            formatter.parse(binding.editBirthDate.text.toString())?.let { parsedDate ->
                calendar.time = parsedDate
            }
        }

        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val selected = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth)
                }
                binding.editBirthDate.setText(formatter.format(selected.time))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
