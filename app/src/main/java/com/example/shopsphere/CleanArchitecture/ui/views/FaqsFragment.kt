package com.example.shopsphere.CleanArchitecture.ui.views

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.shopsphere.R
import com.example.shopsphere.databinding.FragmentFaqsBinding
import com.example.shopsphere.databinding.ItemFaqEntryBinding

class FaqsFragment : Fragment() {

    private var _binding: FragmentFaqsBinding? = null
    private val binding get() = _binding!!
    private val faqEntries by lazy {
        listOf(
            FaqEntry(setOf("general", "service"), R.string.faq_q5, R.string.faq_a5),
            FaqEntry(setOf("general", "payment"), R.string.faq_q6, R.string.faq_a6),
            FaqEntry(setOf("general", "service"), R.string.faq_q1, R.string.faq_a1),
            FaqEntry(setOf("general", "service"), R.string.faq_q7, R.string.faq_a7),
            FaqEntry(setOf("general", "service"), R.string.faq_q8, R.string.faq_a8),
            FaqEntry(setOf("account"), R.string.faq_q9, R.string.faq_a9),
            FaqEntry(setOf("account"), R.string.faq_q2, R.string.faq_a2),
            FaqEntry(setOf("payment"), R.string.faq_q3, R.string.faq_a3),
            FaqEntry(setOf("service"), R.string.faq_q4, R.string.faq_a4)
        )
    }
    private var selectedCategory = "general"
    private var expandedQuestionRes: Int? = R.string.faq_q5

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFaqsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        binding.btnNotifications.setOnClickListener {
            findNavController().navigate(R.id.notificationsFragment)
        }
        binding.buttonVoiceSearch.setOnClickListener { binding.editSearch.requestFocus() }
        binding.chipGeneral.setOnClickListener { selectCategory("general") }
        binding.chipAccount.setOnClickListener { selectCategory("account") }
        binding.chipService.setOnClickListener { selectCategory("service") }
        binding.chipPayment.setOnClickListener { selectCategory("payment") }
        binding.editSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                renderFaqs()
            }
        })
        updateChips()
        renderFaqs()
    }

    private fun selectCategory(category: String) {
        selectedCategory = category
        updateChips()
        renderFaqs()
    }

    private fun updateChips() {
        val activeText = ContextCompat.getColor(requireContext(), android.R.color.white)
        val inactiveText = ContextCompat.getColor(requireContext(), R.color._1a1a1a)
        listOf(
            "general" to binding.chipGeneral,
            "account" to binding.chipAccount,
            "service" to binding.chipService,
            "payment" to binding.chipPayment
        ).forEach { (category, chip) ->
            val selected = category == selectedCategory
            chip.setBackgroundResource(
                if (selected) R.drawable.bg_faq_chip_active else R.drawable.bg_faq_chip_inactive
            )
            chip.setTextColor(if (selected) activeText else inactiveText)
        }
    }

    private fun renderFaqs() {
        val query = binding.editSearch.text.toString().trim().lowercase()
        val filtered = faqEntries.filter { entry ->
            selectedCategory in entry.categories &&
                (query.isBlank() ||
                    getString(entry.questionRes).lowercase().contains(query) ||
                    getString(entry.answerRes).lowercase().contains(query))
        }

        binding.faqListContainer.removeAllViews()
        binding.textNoResults.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE

        filtered.forEach { entry ->
            val itemBinding = ItemFaqEntryBinding.inflate(layoutInflater, binding.faqListContainer, false)
            val expanded = expandedQuestionRes == entry.questionRes
            itemBinding.textQuestion.text = getString(entry.questionRes)
            itemBinding.textAnswer.text = getString(entry.answerRes)
            itemBinding.textAnswer.visibility = if (expanded) View.VISIBLE else View.GONE
            itemBinding.imageChevron.setImageResource(
                if (expanded) R.drawable.ic_chevron_up else R.drawable.ic_chevron_down
            )
            itemBinding.rootFaqEntry.setOnClickListener {
                expandedQuestionRes = if (expandedQuestionRes == entry.questionRes) null else entry.questionRes
                renderFaqs()
            }
            binding.faqListContainer.addView(itemBinding.root)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private data class FaqEntry(
        val categories: Set<String>,
        val questionRes: Int,
        val answerRes: Int
    )
}
