package com.example.shopsphere.CleanArchitecture.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.shopsphere.R
import com.example.shopsphere.databinding.ItemReviewEntryBinding

data class ReviewUiItem(
    val reviewerName: String,
    val daysAgo: String,
    val comment: String,
    val rating: Int
)

class ReviewsAdapter : RecyclerView.Adapter<ReviewsAdapter.ReviewViewHolder>() {

    private val items = mutableListOf<ReviewUiItem>()

    fun submitList(data: List<ReviewUiItem>) {
        items.clear()
        items.addAll(data)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val binding = ItemReviewEntryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ReviewViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        holder.bind(items[position])
    }

    inner class ReviewViewHolder(private val binding: ItemReviewEntryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ReviewUiItem) {
            binding.textReviewComment.text = item.comment
            binding.textReviewMeta.text = itemView.context.getString(
                R.string.reviews_meta_text,
                item.reviewerName,
                item.daysAgo
            )

            val activeColor = ContextCompat.getColor(itemView.context, R.color.review_star_active)
            val inactiveColor = ContextCompat.getColor(itemView.context, android.R.color.darker_gray)
            val starViews = listOf(
                binding.starOne,
                binding.starTwo,
                binding.starThree,
                binding.starFour,
                binding.starFive
            )

            starViews.forEachIndexed { index, imageView ->
                imageView.setColorFilter(if (index < item.rating) activeColor else inactiveColor)
            }
        }
    }
}
