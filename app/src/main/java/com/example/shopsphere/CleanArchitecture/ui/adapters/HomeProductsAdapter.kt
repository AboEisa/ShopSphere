package com.example.shopsphere.CleanArchitecture.ui.adapters

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.shopsphere.CleanArchitecture.ui.models.PresentationProductResult
import com.example.shopsphere.R
import com.example.shopsphere.databinding.ItemHomeBinding

class HomeProductsAdapter(private val onFavoriteClick: (Int) -> Unit, private val isFavorite: (Int) -> Boolean, private val onItemClick: (Int) -> Unit) : RecyclerView.Adapter<HomeProductsAdapter.Holder>() {

    var products: MutableList<PresentationProductResult> = mutableListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemHomeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val product = products[position]
        holder.bind(product)
    }

    override fun getItemCount(): Int = products.size

    inner class Holder(private val binding: ItemHomeBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(product: PresentationProductResult) {
            binding.apply {
                productTitle.text = product.title
                originalPrice.text = "EGP${product.price}"
                discountPercentage.text = "${product.rating.rate}%"

                Glide.with(binding.root)
                    .load(product.image)
                    .into(productImage)


                updateFavoriteIcon(product.id)

                favoriteButton.setOnClickListener {
                    onFavoriteClick(product.id)
                    updateFavoriteIcon(product.id)
                }
                root.setOnClickListener {
                    onItemClick(product.id)
                }
            }
        }

        private fun updateFavoriteIcon(productId: Int) {
            if (isFavorite(productId)) {
                binding.favoriteButton.setColorFilter(
                    ContextCompat.getColor(binding.root.context, R.color.red),
                    android.graphics.PorterDuff.Mode.SRC_IN
                )
            } else {
                binding.favoriteButton.setColorFilter(
                    ContextCompat.getColor(binding.root.context, R.color.gray),
                    android.graphics.PorterDuff.Mode.SRC_IN
                )
            }
        }

    }
}

