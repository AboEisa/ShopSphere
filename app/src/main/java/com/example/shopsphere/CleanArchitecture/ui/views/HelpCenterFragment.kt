package com.example.shopsphere.CleanArchitecture.ui.views

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.shopsphere.R
import com.example.shopsphere.databinding.FragmentHelpCenterBinding

class HelpCenterFragment : Fragment() {

    private var _binding: FragmentHelpCenterBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHelpCenterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        binding.actionEmailSupport.setOnClickListener { openEmailSupport() }
        binding.actionCallSupport.setOnClickListener { openCallSupport() }
        binding.actionOpenFaqs.setOnClickListener { findNavController().navigate(R.id.faqsFragment) }
    }

    private fun openEmailSupport() {
        val email = getString(R.string.help_support_email)
        val subject = getString(R.string.help_email_subject)
        val body = getString(R.string.help_email_body_template)
        val chooserTitle = getString(R.string.help_email_chooser_title)

        val sendToIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse(
                "mailto:$email?subject=${Uri.encode(subject)}&body=${Uri.encode(body)}"
            )
        }

        val packageManager = requireContext().packageManager
        if (sendToIntent.resolveActivity(packageManager) != null) {
            startActivity(Intent.createChooser(sendToIntent, chooserTitle))
            return
        }

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "message/rfc822"
            putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        }
        if (sendIntent.resolveActivity(packageManager) != null) {
            startActivity(Intent.createChooser(sendIntent, chooserTitle))
            return
        }

        copySupportEmailToClipboard(email)
        Toast.makeText(requireContext(), getString(R.string.help_open_mail_failed), Toast.LENGTH_SHORT).show()
    }

    private fun openCallSupport() {
        val phone = getString(R.string.help_support_phone_raw)
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
        launchExternalIntent(intent, getString(R.string.help_open_dialer_failed))
    }

    private fun launchExternalIntent(intent: Intent, fallbackMessage: String) {
        val packageManager = requireContext().packageManager
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            Toast.makeText(requireContext(), fallbackMessage, Toast.LENGTH_SHORT).show()
        }
    }

    private fun copySupportEmailToClipboard(email: String) {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("support_email", email)
        clipboard.setPrimaryClip(clip)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
