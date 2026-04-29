package com.example.shopsphere.CleanArchitecture.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.shopsphere.CleanArchitecture.ui.models.ChatAction
import com.example.shopsphere.CleanArchitecture.ui.models.ChatMessage
import com.example.shopsphere.R
import com.example.shopsphere.databinding.ItemChatMessageBotBinding
import com.example.shopsphere.databinding.ItemChatMessageUserBinding
import com.example.shopsphere.databinding.ItemChatTypingBinding

/**
 * Three view types: user bubble (right-aligned green), bot bubble (left-aligned
 * gray, with error variant), and the animated typing indicator.
 *
 * We animate new messages in with a subtle fade+slide using res/anim/animation.xml
 * (already present in the project).
 */
class ChatMessagesAdapter(
    private val onRetry: () -> Unit,
    private val onActionClick: (ChatAction) -> Unit = {}
) : ListAdapter<ChatMessage, RecyclerView.ViewHolder>(DIFF) {

    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is ChatMessage.UserMessage -> TYPE_USER
        is ChatMessage.BotMessage -> TYPE_BOT
        is ChatMessage.TypingIndicator -> TYPE_TYPING
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_USER -> UserVH(ItemChatMessageUserBinding.inflate(inflater, parent, false))
            TYPE_BOT -> BotVH(ItemChatMessageBotBinding.inflate(inflater, parent, false), onRetry, onActionClick)
            else -> TypingVH(ItemChatTypingBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)
        when (holder) {
            is UserVH -> holder.bind(message as ChatMessage.UserMessage)
            is BotVH -> holder.bind(message as ChatMessage.BotMessage)
            is TypingVH -> holder.bind()
        }
        // Fade-slide in new messages. Only animate items that aren't already
        // onscreen (rebinds during scroll are skipped by the fromLastPosition check).
        animateItem(holder.itemView, position)
    }

    private var lastAnimatedPosition = -1
    private fun animateItem(view: View, position: Int) {
        if (position > lastAnimatedPosition) {
            val anim = AnimationUtils.loadAnimation(view.context, R.anim.animation)
            view.startAnimation(anim)
            lastAnimatedPosition = position
        }
    }

    class UserVH(private val b: ItemChatMessageUserBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(msg: ChatMessage.UserMessage) {
            b.textMessage.text = msg.text
        }
    }

    class BotVH(
        private val b: ItemChatMessageBotBinding,
        private val onRetry: () -> Unit,
        private val onActionClick: (ChatAction) -> Unit
    ) : RecyclerView.ViewHolder(b.root) {
        fun bind(msg: ChatMessage.BotMessage) {
            b.textMessage.text = msg.text
            b.buttonRetry.visibility = if (msg.isError) View.VISIBLE else View.GONE
            b.buttonRetry.setOnClickListener { onRetry() }

            // Render deep-link action chips under the bubble.
            if (msg.actions.isEmpty()) {
                b.actionsScroll.visibility = View.GONE
                b.actionsContainer.removeAllViews()
            } else {
                b.actionsScroll.visibility = View.VISIBLE
                b.actionsContainer.removeAllViews()
                val inflater = LayoutInflater.from(b.root.context)
                msg.actions.forEach { action ->
                    val chip = inflater.inflate(
                        R.layout.item_chat_action_chip,
                        b.actionsContainer,
                        false
                    ) as android.widget.TextView
                    chip.text = action.label
                    chip.setOnClickListener { onActionClick(action) }
                    b.actionsContainer.addView(chip)
                }
            }
        }
    }

    class TypingVH(b: ItemChatTypingBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind() {
            // Dots scale animation lives in the XML (AnimatedVectorDrawable / alpha).
        }
    }

    companion object {
        private const val TYPE_USER = 1
        private const val TYPE_BOT = 2
        private const val TYPE_TYPING = 3

        private val DIFF = object : DiffUtil.ItemCallback<ChatMessage>() {
            override fun areItemsTheSame(old: ChatMessage, new: ChatMessage) = old.id == new.id
            override fun areContentsTheSame(old: ChatMessage, new: ChatMessage) = old == new
        }
    }
}