package com.example.shopsphere.CleanArchitecture.ui.views

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.shopsphere.CleanArchitecture.data.local.SharedPreference
import com.example.shopsphere.R
import com.example.shopsphere.CleanArchitecture.utils.showConfirmDialog
import com.example.shopsphere.CleanArchitecture.utils.showSuccessDialog
import com.example.shopsphere.databinding.FragmentAccountBinding
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AccountFragment : Fragment() {

    private var _binding: FragmentAccountBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var firebaseAuth: FirebaseAuth

    @Inject
    lateinit var sharedPreference: SharedPreference

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAccountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindProfileInfo()
        setupClicks()
    }

    override fun onResume() {
        super.onResume()
        if (_binding != null) {
            bindProfileInfo()
        }
    }

    private fun bindProfileInfo() {
        val user = firebaseAuth.currentUser
        val localName = sharedPreference.getProfileName()
        val localEmail = sharedPreference.getProfileEmail()
        binding.textProfileName.text =
            localName.ifBlank {
                user?.displayName?.takeIf { it.isNotBlank() } ?: getString(R.string.account_guest_user)
            }
        binding.textProfileEmail.text =
            localEmail.ifBlank { user?.email ?: getString(R.string.account_signed_in_as) }

        user?.photoUrl?.let { uri ->
            Glide.with(this)
                .load(uri)
                .placeholder(R.drawable.ic_user)
                .error(R.drawable.ic_user)
                .into(binding.imageProfile)
        }
    }

    private fun setupClicks() {
        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        binding.layoutOrders.setOnClickListener {
            findNavController().navigate(R.id.ordersFragment)
        }

        binding.layoutPaymentMethods.setOnClickListener {
            findNavController().navigate(R.id.paymentMethodsFragment)
        }

        binding.layoutAddressBook.setOnClickListener {
            findNavController().navigate(R.id.addressBookFragment)
        }

        binding.layoutDetails.setOnClickListener {
            findNavController().navigate(R.id.myDetailsFragment)
        }

        binding.layoutFaqs.setOnClickListener {
            findNavController().navigate(R.id.faqsFragment)
        }

        binding.layoutHelpCenter.setOnClickListener {
            findNavController().navigate(R.id.helpCenterFragment)
        }

        binding.layoutLogout.setOnClickListener {
            showConfirmDialog(
                title = getString(R.string.dialog_logout_title),
                message = getString(R.string.dialog_logout_message),
                positiveText = getString(R.string.dialog_logout_title)
            ) {
                firebaseAuth.signOut()
                sharedPreference.clear()
                sharedPreference.saveIsLoggedIn(false)
                sharedPreference.clearUid()

                showSuccessDialog(
                    title = getString(R.string.dialog_logout_success_title),
                    message = getString(R.string.dialog_logout_success_message)
                ) {
                    val intent = Intent(requireContext(), MainActivity::class.java).apply {
                        putExtra(MainActivity.EXTRA_OPEN_HOME, false)
                        putExtra(MainActivity.EXTRA_OPEN_LOGIN, true)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}
