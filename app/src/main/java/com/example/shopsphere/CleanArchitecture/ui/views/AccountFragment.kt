package com.example.shopsphere.CleanArchitecture.ui.views

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.card.MaterialCardView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.shopsphere.CleanArchitecture.data.local.SharedPreference
import com.example.shopsphere.R
import com.example.shopsphere.CleanArchitecture.domain.LogoutUseCase
import com.example.shopsphere.CleanArchitecture.utils.showConfirmDialog
import com.example.shopsphere.CleanArchitecture.utils.showSuccessDialog
import com.example.shopsphere.databinding.FragmentAccountBinding
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AccountFragment : Fragment() {

    private var _binding: FragmentAccountBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var firebaseAuth: FirebaseAuth

    @Inject
    lateinit var sharedPreference: SharedPreference

    @Inject
    lateinit var logoutUseCase: LogoutUseCase

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAccountBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindProfileHeader()
        setupClicks()
    }

    override fun onResume() {
        super.onResume()
        bindProfileHeader()
    }

    private fun bindProfileHeader() {
        // Source of truth is the signup/login form input we persisted in
        // SharedPreference. We deliberately do NOT fall back to Firebase here —
        // the app's auth backend is the REST API, and Firebase is only used for
        // the Google/Facebook OAuth handshake. Mixing displayName from Firebase
        // would surface stale or inconsistent data after the user signed up
        // through the email/password flow.
        val storedName = sharedPreference.getProfileName()
        val storedEmail = sharedPreference.getProfileEmail()

        // Email/password login only collects email — name isn't known until the
        // user signs up or fills My Details. When name is missing but we have
        // an email, derive a friendly display name from the local part
        // (e.g. "yara.hassan@example.com" → "Yara Hassan") so the header never
        // looks like a generic "Guest User" placeholder for a real account.
        val name = storedName.ifBlank {
            deriveNameFromEmail(storedEmail) ?: getString(R.string.account_guest_user)
        }
        val email = storedEmail.ifBlank { getString(R.string.account_signed_in_as) }

        binding.textProfileName.text = name
        binding.textProfileEmail.text = email
        binding.textAvatarInitials.text = computeInitials(name)
    }

    private fun deriveNameFromEmail(email: String): String? {
        val local = email.substringBefore('@', "").trim()
        if (local.isBlank()) return null
        // Split on common separators ('.', '_', '-', '+') and capitalize each
        // chunk. Drop trailing digits so "yara.hassan99" → "Yara Hassan".
        return local.split('.', '_', '-', '+')
            .map { chunk -> chunk.trimEnd { it.isDigit() } }
            .filter { it.isNotBlank() }
            .joinToString(" ") { chunk ->
                chunk.replaceFirstChar { c -> c.uppercaseChar() }
            }
            .ifBlank { null }
    }

    private fun computeInitials(fullName: String): String {
        val parts = fullName.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        if (parts.isEmpty()) return "?"
        val first = parts.first().firstOrNull()?.uppercaseChar() ?: return "?"
        val second = if (parts.size > 1) parts[1].firstOrNull()?.uppercaseChar() else null
        return if (second != null) "$first$second" else first.toString()
    }

    private fun setupClicks() {
        binding.btnBack.setOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }
        binding.btnNotifications.setOnClickListener {
            findNavController().navigate(R.id.notificationsFragment)
        }

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

        // Help Center row now opens the Sphere AI chatbot.
        binding.layoutHelpCenter.setOnClickListener {
            findNavController().navigate(R.id.action_accountFragment_to_chatBotFragment)
        }

        binding.layoutNotifications.setOnClickListener {
            findNavController().navigate(R.id.notificationsFragment)
        }

        binding.layoutAbout.setOnClickListener {
            showSuccessDialog(
                title = getString(R.string.account_about_title),
                message = getString(R.string.account_about_message)
            )
        }

        binding.layoutLanguage.setOnClickListener {
            showLanguageDialog()
        }

        binding.layoutLogout.setOnClickListener {
            showConfirmDialog(
                title = getString(R.string.dialog_logout_title),
                message = getString(R.string.dialog_logout_message),
                positiveText = getString(R.string.dialog_logout_title)
            ) {
                // Best-effort: tell the server first (clears its session/token),
                // then drop local state. LogoutUseCase already swallows server
                // failures so the user always gets logged out locally.
                viewLifecycleOwner.lifecycleScope.launch {
                    logoutUseCase()
                }
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

    private fun showLanguageDialog() {
        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.dialog_language_picker, null)
        dialog.setContentView(view)

        val cardEnglish = view.findViewById<MaterialCardView>(R.id.cardEnglish)
        val cardArabic = view.findViewById<MaterialCardView>(R.id.cardArabic)
        val checkEnglish = view.findViewById<ImageView>(R.id.checkEnglish)
        val checkArabic = view.findViewById<ImageView>(R.id.checkArabic)

        val current = sharedPreference.getLanguage().ifEmpty { "en" }
        updateLanguageSelectionUi(
            current,
            cardEnglish, cardArabic,
            checkEnglish, checkArabic
        )

        cardEnglish.setOnClickListener {
            applyLanguage("en", dialog)
        }
        cardArabic.setOnClickListener {
            applyLanguage("ar", dialog)
        }

        dialog.show()
    }

    private fun updateLanguageSelectionUi(
        current: String,
        cardEnglish: MaterialCardView,
        cardArabic: MaterialCardView,
        checkEnglish: ImageView,
        checkArabic: ImageView
    ) {
        val selectedStroke = resources.getColor(R.color.bright_green, null)
        val unselectedStroke = android.graphics.Color.parseColor("#E5E7EB")
        if (current == "ar") {
            cardArabic.strokeColor = selectedStroke
            cardArabic.strokeWidth = (resources.displayMetrics.density * 2).toInt()
            cardEnglish.strokeColor = unselectedStroke
            cardEnglish.strokeWidth = (resources.displayMetrics.density * 1).toInt()
            checkArabic.visibility = View.VISIBLE
            checkEnglish.visibility = View.GONE
        } else {
            cardEnglish.strokeColor = selectedStroke
            cardEnglish.strokeWidth = (resources.displayMetrics.density * 2).toInt()
            cardArabic.strokeColor = unselectedStroke
            cardArabic.strokeWidth = (resources.displayMetrics.density * 1).toInt()
            checkEnglish.visibility = View.VISIBLE
            checkArabic.visibility = View.GONE
        }
    }

    private fun applyLanguage(tag: String, dialog: BottomSheetDialog) {
        sharedPreference.setLanguage(tag)
        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.forLanguageTags(tag)
        )
        dialog.dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
