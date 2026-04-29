package com.example.shopsphere.CleanArchitecture.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.shopsphere.CleanArchitecture.ui.models.PresentationProductResult
import com.example.shopsphere.CleanArchitecture.utils.formatEgpPrice
import com.example.shopsphere.databinding.ItemCheckoutCartProductBinding

class CheckoutCartItemsAdapter :
    ListAdapter<PresentationProductResult, CheckoutCartItemsAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCheckoutCartProductBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemCheckoutCartProductBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PresentationProductResult) {
            binding.txtProductTitle.text = item.title
            binding.txtProductQuantity.text = itemView.context.getString(
                com.example.shopsphere.R.string.qty_label,
                item.quantity.coerceAtLeast(1)
            )
            binding.txtProductPrice.text = formatEgpPrice(item.price * item.quantity.coerceAtLeast(1))

            val placeholderDrawable = androidx.core.content.ContextCompat.getDrawable(
                itemView.context,
                com.example.shopsphere.R.drawable.ic_image
            )

            Glide.with(itemView.context)
                .load(item.image)
                .placeholder(placeholderDrawable)
                .error(placeholderDrawable)
                .into(binding.imgProduct)
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<PresentationProductResult>() {
            override fun areItemsTheSame(
                oldItem: PresentationProductResult,
                newItem: PresentationProductResult
            ): Boolean {
                return oldItem.id == newItem.id && oldItem.cartLineId == newItem.cartLineId
            }

            override fun areContentsTheSame(
                oldItem: PresentationProductResult,
                newItem: PresentationProductResult
            ): Boolean {
                return oldItem == newItem
            }
        }
    }
}
