package com.example.shopsphere.CleanArchitecture.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.shopsphere.CleanArchitecture.ui.models.OrderProduct
import com.example.shopsphere.databinding.ItemOrderProductBinding
import java.util.Locale

class OrderProductsAdapter : ListAdapter<OrderProduct, OrderProductsAdapter.ProductViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ItemOrderProductBinding.inflate(
            LayoutInflater.from(parent.context), 
            parent, 
            false
        )
        return ProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ProductViewHolder(private val binding: ItemOrderProductBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(product: OrderProduct) {
            binding.textProductName.text = product.productName
            binding.textProductQuantity.text = "x${product.quantity}"
            binding.textProductPrice.text = String.format(Locale.US, "%.2f", product.price)
            
            // Load product image
            if (!product.imageUrl.isNullOrEmpty()) {
                Glide.with(binding.imageProduct.context)
                    .load(product.imageUrl)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_gallery)
                    .into(binding.imageProduct)
            } else {
                binding.imageProduct.setImageResource(android.R.drawable.ic_menu_gallery)
            }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<OrderProduct>() {
            override fun areItemsTheSame(oldItem: OrderProduct, newItem: OrderProduct): Boolean {
                return oldItem.productName == newItem.productName
            }

            override fun areContentsTheSame(oldItem: OrderProduct, newItem: OrderProduct): Boolean {
                return oldItem == newItem
            }
        }
    }
}
