package com.example.shopsphere.CleanArchitecture.ui.adapters

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.annotation. SuppressLint
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view. LayoutInflater
import android.view. ViewGroup
import androidx.core.content. ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.shopsphere.databinding. ItemTypesBinding
import com.example. shopsphere.R

class TypesAdapter : RecyclerView. Adapter<TypesAdapter.Holder>() {

    var list: ArrayList<String>? = null
    var onTypeClick: ((String) -> Unit)? = null
    private var selectedPosition: Int = 0

    fun submitTypes(items: List<String>, selectedType: String) {
        list = ArrayList(items)
        val index = list?.indexOfFirst { it.equals(selectedType, ignoreCase = true) } ?: -1
        selectedPosition = if (index >= 0) index else 0
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemTypesBinding.inflate(LayoutInflater. from(parent.context), parent, false)
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, @SuppressLint("RecyclerView") position: Int) {
        val type = list?.get(position)
        holder.textType. text = type

        val isSelected = selectedPosition == position

        // Apply initial state without animation
        holder. bind(isSelected)

        holder.itemView.setOnClickListener {
            val previousPosition = selectedPosition
            selectedPosition = position
            notifyItemChanged(previousPosition)
            notifyItemChanged(selectedPosition)
            type?. let { onTypeClick?.invoke(it) }
        }
    }

    override fun getItemCount(): Int = list?.size ?: 0

    inner class Holder(private val binding: ItemTypesBinding) : RecyclerView.ViewHolder(binding.root) {
        val textType = binding.textType

        fun bind(isSelected: Boolean) {
            val context = itemView.context
            val greenColor = ContextCompat.getColor(context, R.color.bright_green)
            val whiteColor = ContextCompat.getColor(context, android.R.color. white)
            val grayBorderColor = Color.parseColor("#BDBDBD")
            val borderWidth = (1.5f * context.resources.displayMetrics.density).toInt()

            val startColor = if (isSelected) whiteColor else greenColor
            val endColor = if (isSelected) greenColor else whiteColor

            val colorAnimation = ValueAnimator.ofObject(ArgbEvaluator(), startColor, endColor)
            colorAnimation.duration = 250
            colorAnimation.addUpdateListener { animator ->
                val animatedColor = animator.animatedValue as Int
                val drawable = textType. background as?  GradientDrawable
                drawable?.setColor(animatedColor)

                // Set border: gray when not selected, green when selected
                if (isSelected) {
                    drawable?.setStroke(borderWidth, greenColor)
                } else {
                    drawable?. setStroke(borderWidth, grayBorderColor)
                }
            }
            colorAnimation.start()

            textType.setTextColor(
                if (isSelected) Color.WHITE else Color.BLACK
            )
        }
    }
}
