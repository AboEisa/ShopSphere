package com.example.shopsphere.CleanArchitecture.ui.adapters

import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.shopsphere.CleanArchitecture.ui.adapters.SavedAdapter.Holder
import com.example.shopsphere.CleanArchitecture.ui.models.PresentationProductResult
import com.example.shopsphere.CleanArchitecture.utils.formatEgpPrice
import com.example.shopsphere.R
import com.example.shopsphere.databinding.ItemSavedBinding


class SavedAdapter(
    private val onItemClick: (Int) -> Unit,
    private val onFavoriteClick: (PresentationProductResult) -> Unit,
    private val isFavorite: (Int) -> Boolean
) : ListAdapter<PresentationProductResult, Holder>(DIFF) {

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long = getItem(position).id.toLong()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemSavedBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class Holder(val binding: ItemSavedBinding) : RecyclerView.ViewHolder(binding.root) {

        private val redColor = ContextCompat.getColor(binding.root.context, R.color.red)
        private val grayColor = ContextCompat.getColor(binding.root.context, R.color.gray)

        fun bind(product: PresentationProductResult) {
            binding.apply {
                productTitle.text = product.title
                originalPrice.text = formatEgpPrice(product.price)
                discountPercentage.text = "\u2605 ${String.format("%.1f", product.rating.rate)}"

                Glide.with(root)
                    .load(product.image)
                    .placeholder(R.drawable.ic_image)
                    .error(R.drawable.ic_image)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(productImage)

                root.setOnClickListener { onItemClick(product.id) }
                updateFavoriteIcon(product.id)
                favoriteButton.setOnClickListener {
                    onFavoriteClick(product)
                    updateFavoriteIcon(product.id)
                }
            }
        }

        private fun updateFavoriteIcon(productId: Int) {
            val color = if (isFavorite(productId)) redColor else grayColor
            binding.favoriteButton.setColorFilter(color, PorterDuff.Mode.SRC_IN)
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<PresentationProductResult>() {
            override fun areItemsTheSame(
                oldItem: PresentationProductResult,
                newItem: PresentationProductResult
            ): Boolean = oldItem.id == newItem.id

            override fun areContentsTheSame(
                oldItem: PresentationProductResult,
                newItem: PresentationProductResult
            ): Boolean = oldItem == newItem
        }
    }
}
