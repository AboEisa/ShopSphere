package com.example.shopsphere.CleanArchitecture.ui.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.shopsphere.CleanArchitecture.data.local.SharedPreference
import com.example.shopsphere.CleanArchitecture.utils.showSuccessDialog
import com.bumptech.glide.Glide
import com.example.shopsphere.R
import com.example.shopsphere.databinding.FragmentMyDetailsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MyDetailsFragment : Fragment() {

    private var _binding: FragmentMyDetailsBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var firebaseAuth: FirebaseAuth

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
        bindUserData()
        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        binding.btnSaveDetails.setOnClickListener {
            saveProfileChanges()
        }
    }

    private fun bindUserData() {
        val user = firebaseAuth.currentUser
        val localName = sharedPreference.getProfileName()
        val localEmail = sharedPreference.getProfileEmail()
        val localPhone = sharedPreference.getProfilePhone()

        binding.editFullName.setText(localName.ifBlank { user?.displayName.orEmpty() })
        binding.editEmail.setText(localEmail.ifBlank { user?.email.orEmpty() })
        binding.editPhone.setText(localPhone)

        user?.photoUrl?.let { uri ->
            Glide.with(this)
                .load(uri)
                .placeholder(R.drawable.ic_user)
                .error(R.drawable.ic_user)
                .into(binding.imageProfile)
        }
    }

    private fun saveProfileChanges() {
        val name = binding.editFullName.text.toString().trim()
        val email = binding.editEmail.text.toString().trim()
        val phone = binding.editPhone.text.toString().trim()

        if (name.isBlank()) {
            binding.editFullName.error = getString(R.string.account_full_name)
            return
        }
        if (email.isBlank()) {
            binding.editEmail.error = getString(R.string.account_email)
            return
        }

        sharedPreference.saveProfile(name = name, email = email, phone = phone)

        val user = firebaseAuth.currentUser
        if (user != null && name != user.displayName) {
            val updates = UserProfileChangeRequest.Builder().setDisplayName(name).build()
            user.updateProfile(updates)
        }

        showSuccessDialog(
            title = getString(R.string.account_changes_saved),
            message = getString(R.string.account_changes_saved)
        ) {
            findNavController().navigateUp()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
