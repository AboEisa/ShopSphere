package com.example.shopsphere.CleanArchitecture.ui.views

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.shopsphere.R
import com.example.shopsphere.databinding.FragmentCustomerServiceBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

class CustomerServiceFragment : Fragment() {

    private var _binding: FragmentCustomerServiceBinding? = null
    private val binding get() = _binding!!

    private val imagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri == null || _binding == null) return@registerForActivityResult
            addImageBubble(uri, true)
            addTime(getString(R.string.customer_service_time_now), true)
            queueAutoReply()
            scrollToBottom()
        }

    private val voicePermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                launchVoiceRecognizer()
            } else if (isAdded) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.customer_service_voice_permission),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    private val voiceRecognizerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (_binding == null) return@registerForActivityResult
            if (result.resultCode != Activity.RESULT_OK || result.data == null) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.customer_service_voice_empty),
                    Toast.LENGTH_SHORT
                ).show()
                return@registerForActivityResult
            }

            val transcript = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                .orEmpty()
                .trim()

            if (transcript.isBlank()) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.customer_service_voice_empty),
                    Toast.LENGTH_SHORT
                ).show()
                return@registerForActivityResult
            }

            addVoiceBubble(transcript, true)
            addTime(getString(R.string.customer_service_time_now), true)
            queueAutoReply()
            scrollToBottom()
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCustomerServiceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        binding.buttonCall.setOnClickListener {
            startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${getString(R.string.help_support_phone_raw)}")))
        }
        binding.buttonAttachImage.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }
        binding.buttonSendMessage.setOnClickListener {
            if (binding.editMessage.text?.toString().orEmpty().trim().isNotBlank()) {
                sendTextMessage()
            } else {
                startVoiceMessage()
            }
        }
        binding.editMessage.addTextChangedListener {
            updateComposerAction()
        }
        seedMessages()
        updateComposerAction()
    }

    private fun seedMessages() {
        if (binding.messagesContainer.childCount > 0) return
        addBubble(getString(R.string.customer_service_message_1), false)
        addBubble(getString(R.string.customer_service_message_2), false)
        addTime("10:41 pm", false)
        addBubble(getString(R.string.customer_service_message_3), true)
        addBubble(getString(R.string.customer_service_message_4), true)
        addTime("10:50 pm", true)
        addBubble(getString(R.string.customer_service_message_5), false)
        addBubble(getString(R.string.customer_service_message_6), false)
        addTime("10:51 pm", false)
    }

    private fun sendTextMessage() {
        val text = binding.editMessage.text?.toString().orEmpty().trim()
        if (text.isBlank()) {
            Toast.makeText(requireContext(), getString(R.string.customer_service_send_empty), Toast.LENGTH_SHORT).show()
            return
        }

        addBubble(text, true)
        addTime(getString(R.string.customer_service_time_now), true)
        binding.editMessage.text?.clear()
        queueAutoReply()
        scrollToBottom()
    }

    private fun startVoiceMessage() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
            == PackageManager.PERMISSION_GRANTED
        ) {
            launchVoiceRecognizer()
        } else {
            voicePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun launchVoiceRecognizer() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.customer_service_voice_prompt))
        }

        if (intent.resolveActivity(requireActivity().packageManager) == null) {
            Toast.makeText(
                requireContext(),
                getString(R.string.customer_service_voice_not_available),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        voiceRecognizerLauncher.launch(intent)
    }

    private fun queueAutoReply() {
        viewLifecycleOwner.lifecycleScope.launch {
            delay(650)
            if (_binding == null) return@launch
            addBubble(getString(R.string.customer_service_auto_reply), false)
            scrollToBottom()
        }
    }

    private fun updateComposerAction() {
        if (_binding == null) return
        val hasTypedText = binding.editMessage.text?.toString().orEmpty().trim().isNotBlank()
        binding.buttonSendMessage.icon = ContextCompat.getDrawable(
            requireContext(),
            if (hasTypedText) R.drawable.ic_send else R.drawable.ic_mic
        )
    }

    private fun addBubble(message: String, outgoing: Boolean) {
        val context = requireContext()
        val wrapper = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 10
            }
            gravity = if (outgoing) Gravity.END else Gravity.START
        }

        val bubble = TextView(context).apply {
            text = message
            setTextColor(if (outgoing) Color.WHITE else Color.parseColor("#4A4A4A"))
            textSize = 16f
            setLineSpacing(0f, 1.2f)
            background = ContextCompat.getDrawable(
                context,
                if (outgoing) R.drawable.bg_chat_bubble_outgoing else R.drawable.bg_chat_bubble_incoming
            )
            setPadding(20, 16, 20, 16)
            layoutParams = LinearLayout.LayoutParams(
                (resources.displayMetrics.widthPixels * if (outgoing) 0.58f else 0.64f).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        wrapper.addView(bubble)
        binding.messagesContainer.addView(wrapper)
    }

    private fun addVoiceBubble(transcript: String, outgoing: Boolean) {
        val context = requireContext()
        val wrapper = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 10
            }
            gravity = if (outgoing) Gravity.END else Gravity.START
        }

        val bubble = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = ContextCompat.getDrawable(
                context,
                if (outgoing) R.drawable.bg_chat_bubble_outgoing else R.drawable.bg_chat_bubble_incoming
            )
            setPadding(18, 16, 18, 16)
            layoutParams = LinearLayout.LayoutParams(
                (resources.displayMetrics.widthPixels * if (outgoing) 0.62f else 0.66f).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val icon = ImageView(context).apply {
            setImageResource(R.drawable.ic_mic)
            imageTintList = ContextCompat.getColorStateList(
                context,
                if (outgoing) android.R.color.white else R.color._1a1a1a
            )
            layoutParams = LinearLayout.LayoutParams(18, 18)
        }

        val text = TextView(context).apply {
            this.text = getString(R.string.customer_service_voice_label, transcript)
            setTextColor(if (outgoing) Color.WHITE else Color.parseColor("#4A4A4A"))
            textSize = 15f
            layoutParams = LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                marginStart = 10
            }
        }

        bubble.addView(icon)
        bubble.addView(text)
        wrapper.addView(bubble)
        binding.messagesContainer.addView(wrapper)
    }

    private fun addImageBubble(imageUri: Uri, outgoing: Boolean) {
        val context = requireContext()
        val wrapper = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 10
            }
            gravity = if (outgoing) Gravity.END else Gravity.START
        }

        val card = CardView(context).apply {
            radius = 24f
            cardElevation = 0f
            layoutParams = LinearLayout.LayoutParams(
                (resources.displayMetrics.widthPixels * 0.56f).toInt(),
                (resources.displayMetrics.widthPixels * 0.56f).toInt()
            )
            setCardBackgroundColor(Color.TRANSPARENT)
        }

        val image = ImageView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            scaleType = ImageView.ScaleType.CENTER_CROP
            contentDescription = getString(R.string.customer_service_image)
        }

        Glide.with(this)
            .load(imageUri)
            .into(image)

        card.addView(image)
        wrapper.addView(card)
        binding.messagesContainer.addView(wrapper)
    }

    private fun addTime(label: String, outgoing: Boolean) {
        val time = TextView(requireContext()).apply {
            text = label
            textSize = 12f
            setTextColor(Color.parseColor("#B3B3B3"))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 8
            }
            gravity = if (outgoing) Gravity.END else Gravity.START
        }
        binding.messagesContainer.addView(time)
    }

    private fun scrollToBottom() {
        binding.messagesScroll.post {
            binding.messagesScroll.fullScroll(View.FOCUS_DOWN)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
