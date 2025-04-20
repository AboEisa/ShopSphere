package com.example.shopsphere.CleanArchitecture.ui.adapters

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.shopsphere.databinding.ItemTypesBinding

class TypesAdapter : RecyclerView.Adapter<TypesAdapter.Holder>() {

    var list: ArrayList<String>? = null
    var onTypeClick: ((String) -> Unit)? = null
    private var selectedPosition: Int = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemTypesBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, @SuppressLint("RecyclerView") position: Int) {
        val type = list?.get(position)
        holder.textType.text = type

        val isSelected = selectedPosition == position

        // Apply initial state without animation
        holder.bind(isSelected)

        holder.itemView.setOnClickListener {
            val previousPosition = selectedPosition
            selectedPosition = position
            notifyItemChanged(previousPosition)
            notifyItemChanged(selectedPosition)
            type?.let { onTypeClick?.invoke(it) }
        }
    }

    override fun getItemCount(): Int = list?.size ?: 0

    inner class Holder(private val binding: ItemTypesBinding) : RecyclerView.ViewHolder(binding.root) {
        val textType = binding.textType

        fun bind(isSelected: Boolean) {
            val startColor = if (isSelected) ContextCompat.getColor(itemView.context, android.R.color.white)
            else ContextCompat.getColor(itemView.context, android.R.color.black)

            val endColor = if (isSelected) ContextCompat.getColor(itemView.context, android.R.color.black)
            else ContextCompat.getColor(itemView.context, android.R.color.white)

            val colorAnimation = ValueAnimator.ofObject(ArgbEvaluator(), startColor, endColor)
            colorAnimation.duration = 250 // Milliseconds
            colorAnimation.addUpdateListener { animator ->
                textType.background.setTint(animator.animatedValue as Int)
            }
            colorAnimation.start()

            textType.setTextColor(
                if (isSelected) Color.WHITE else Color.BLACK
            )
        }

    }
}
