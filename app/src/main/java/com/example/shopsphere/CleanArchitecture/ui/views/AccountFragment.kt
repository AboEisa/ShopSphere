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
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.shopsphere.CleanArchitecture.data.local.SharedPreference
import com.example.shopsphere.R
import com.example.shopsphere.CleanArchitecture.ui.viewmodels.AccountUiEvent
import com.example.shopsphere.CleanArchitecture.ui.viewmodels.AccountViewModel
import com.example.shopsphere.CleanArchitecture.utils.showConfirmDialog
import com.example.shopsphere.CleanArchitecture.utils.showSuccessDialog
import com.example.shopsphere.databinding.FragmentAccountBinding
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Account screen — shows the profile header and all account menu rows.
 *
 * Changes from the original:
 *  - Replaced direct use-case injections + manual coroutine scope with [AccountViewModel].
 *  - Profile header data (name, email, initials) now comes from [AccountViewModel.profileState],
 *    which is cached-first + server-refreshed, eliminating the "Guest User" flash.
 *  - Logout flow delegates to [AccountViewModel.logout] instead of calling use cases inline;
 *    the ViewModel emits [AccountUiEvent.LogoutSuccess] to trigger navigation.
 */
@AndroidEntryPoint
class AccountFragment : Fragment() {

    private var _binding: FragmentAccountBinding? = null
    private val binding get() = _binding!!

    // ViewModel owns all business logic; Fragment only handles navigation + UI
    private val viewModel: AccountViewModel by viewModels()

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
        observeViewModel()
        setupClicks()
    }

    override fun onResume() {
        super.onResume()
        // Silent background refresh so the header reflects any edits made in MyDetailsFragment.
        viewModel.refreshFromServer()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Observers
    // ─────────────────────────────────────────────────────────────────────────

    private fun observeViewModel() {
        viewModel.profileState.observe(viewLifecycleOwner) { state ->
            binding.textProfileName.text = state.fullName.ifBlank {
                getString(R.string.account_guest_user)
            }
            binding.textProfileEmail.text = state.email.ifBlank {
                getString(R.string.account_signed_in_as)
            }
            binding.textAvatarInitials.text = state.initials
        }

        viewModel.uiEvent.observe(viewLifecycleOwner) { event ->
            event ?: return@observe
            when (event) {
                is AccountUiEvent.LogoutSuccess -> {
                    // Firebase sign-out (Google/Facebook OAuth cleanup)
                    firebaseAuth.signOut()
                    // Navigate to login, clearing the back stack
                    val intent = Intent(requireContext(), MainActivity::class.java).apply {
                        putExtra(MainActivity.EXTRA_OPEN_HOME, false)
                        putExtra(MainActivity.EXTRA_OPEN_LOGIN, true)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                }
                is AccountUiEvent.Error -> {
                    // Show error (could use a Snackbar; matching existing project style here)
                    showSuccessDialog(title = getString(R.string.dialog_error_title), message = event.message)
                }
                else -> { /* Other events handled in MyDetailsFragment */ }
            }
            viewModel.clearEvent()
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Click handlers
    // ─────────────────────────────────────────────────────────────────────────

    private fun setupClicks() {
        binding.btnBack.setOnClickListener { requireActivity().onBackPressedDispatcher.onBackPressed() }
        binding.btnNotifications.setOnClickListener {
            findNavController().navigate(R.id.notificationsFragment)
        }
        binding.layoutOrders.setOnClickListener {
            findNavController().navigate(R.id.action_accountFragment_to_ordersFragment)
        }
        binding.layoutDetails.setOnClickListener {
            findNavController().navigate(R.id.myDetailsFragment)
        }
        binding.layoutFaqs.setOnClickListener {
            findNavController().navigate(R.id.faqsFragment)
        }
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
        binding.layoutLanguage.setOnClickListener { showLanguageDialog() }
        binding.layoutLogout.setOnClickListener { confirmLogout() }
    }

    private fun confirmLogout() {
        showConfirmDialog(
            title = getString(R.string.dialog_logout_title),
            message = getString(R.string.dialog_logout_message),
            positiveText = getString(R.string.dialog_logout_title)
        ) {
            // Clear all local prefs first so the next resume doesn't see stale data.
            sharedPreference.clear()
            sharedPreference.saveIsLoggedIn(false)
            sharedPreference.clearUid()

            // Delegate server-side logout + navigation event to the ViewModel.
            viewModel.logout()

            // The LogoutSuccess event observer above will fire once the coroutine
            // completes and trigger the navigation + success dialog.
            showSuccessDialog(
                title = getString(R.string.dialog_logout_success_title),
                message = getString(R.string.dialog_logout_success_message)
            )
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Language picker (unchanged from original)
    // ─────────────────────────────────────────────────────────────────────────

    private fun showLanguageDialog() {
        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.dialog_language_picker, null)
        dialog.setContentView(view)

        val cardEnglish = view.findViewById<MaterialCardView>(R.id.cardEnglish)
        val cardArabic = view.findViewById<MaterialCardView>(R.id.cardArabic)
        val checkEnglish = view.findViewById<ImageView>(R.id.checkEnglish)
        val checkArabic = view.findViewById<ImageView>(R.id.checkArabic)

        val current = sharedPreference.getLanguage().ifEmpty { "en" }
        updateLanguageSelectionUi(current, cardEnglish, cardArabic, checkEnglish, checkArabic)

        cardEnglish.setOnClickListener { applyLanguage("en", dialog) }
        cardArabic.setOnClickListener { applyLanguage("ar", dialog) }
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
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
        dialog.dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
