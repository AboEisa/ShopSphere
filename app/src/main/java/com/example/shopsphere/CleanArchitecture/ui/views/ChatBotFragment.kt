package com.example.shopsphere.CleanArchitecture.ui.views

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.core.content.getSystemService
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.shopsphere.CleanArchitecture.ui.adapters.ChatMessagesAdapter
import com.example.shopsphere.CleanArchitecture.ui.viewmodels.ChatBotViewModel
import com.example.shopsphere.CleanArchitecture.ui.viewmodels.CheckoutSharedViewModel
import com.example.shopsphere.databinding.FragmentChatbotBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Sphere AI chat screen — user types, we try FAQ rules first, fall back
 * to Gemini 2.0 Flash for anything complex. Auto-scrolls on new messages,
 * shows quick-reply chips when empty, and hosts the typing animation.
 */
@AndroidEntryPoint
class ChatBotFragment : Fragment() {

    private var _binding: FragmentChatbotBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ChatBotViewModel by viewModels()
    private val sharedViewModel: CheckoutSharedViewModel by activityViewModels()

    private val messagesAdapter by lazy {
        ChatMessagesAdapter(onRetry = { viewModel.retryLast() })
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatbotBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecycler()
        setupTopBar()
        setupInput()
        setupQuickReplyChips()
        setupPulseAnimation()
        observeOrders()
        observeState()
    }

    private fun setupRecycler() {
        binding.recyclerMessages.apply {
            layoutManager = LinearLayoutManager(requireContext()).apply {
                stackFromEnd = true
            }
            adapter = messagesAdapter
            itemAnimator = null // we animate ourselves inside the adapter
        }
    }

    private fun setupTopBar() {
        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
    }

    private fun setupInput() {
        binding.btnSend.setOnClickListener { sendCurrent() }
        binding.editMessage.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendCurrent()
                true
            } else false
        }
    }

    private fun setupQuickReplyChips() {
        binding.chipTrack.setOnClickListener { sendText("Track my order") }
        binding.chipReturns.setOnClickListener { sendText("What's the returns policy?") }
        binding.chipDeals.setOnClickListener { sendText("Any deals for me today?") }
        binding.chipHuman.setOnClickListener { sendText("Talk to a human") }
    }

    /**
     * Subtle scale+alpha pulse on the little green dot next to the avatar —
     * signals "live / online" without being loud.
     */
    private fun setupPulseAnimation() {
        val scale = ObjectAnimator.ofFloat(binding.pulseDot, "scaleX", 1f, 1.35f, 1f).apply {
            duration = 1400
            repeatCount = ValueAnimator.INFINITE
        }
        val scaleY = ObjectAnimator.ofFloat(binding.pulseDot, "scaleY", 1f, 1.35f, 1f).apply {
            duration = 1400
            repeatCount = ValueAnimator.INFINITE
        }
        scale.start()
        scaleY.start()
    }

    private fun observeOrders() {
        // Feed the last 5 orders into the chat use case's system prompt so
        // Gemini can answer "where's my latest order?" accurately.
        sharedViewModel.orderHistory.observe(viewLifecycleOwner) { orders ->
            viewModel.updateOrderContext(orders.orEmpty())
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    messagesAdapter.submitList(state.messages) {
                        if (state.messages.isNotEmpty()) {
                            binding.recyclerMessages.scrollToPosition(state.messages.size - 1)
                        }
                    }
                    val hasMessages = state.messages.isNotEmpty()
                    binding.emptyState.visibility = if (hasMessages) View.GONE else View.VISIBLE
                    // Show quick-reply chips only when conversation is empty.
                    binding.chipsScroll.visibility = if (hasMessages) View.GONE else View.VISIBLE
                }
            }
        }
    }

    private fun sendCurrent() {
        val text = binding.editMessage.text?.toString().orEmpty()
        if (text.isBlank()) return
        sendText(text)
        binding.editMessage.setText("")
    }

    private fun sendText(text: String) {
        viewModel.sendMessage(text)
        hideKeyboard()
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService<InputMethodManager>()
        imm?.hideSoftInputFromWindow(binding.editMessage.windowToken, 0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
