package com.example.shopsphere.CleanArchitecture.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.shopsphere.CleanArchitecture.ui.adapters.SavedAdapter.Holder
import com.example.shopsphere.CleanArchitecture.ui.models.PresentationProductResult
import com.example.shopsphere.R
import com.example.shopsphere.databinding.ItemSavedBinding


class SavedAdapter(private val onItemClick: (Int) -> Unit, private val onFavoriteClick: (PresentationProductResult) -> Unit,private val isFavorite: (Int) -> Boolean): RecyclerView.Adapter<Holder>() {

    private val movieList = mutableListOf<PresentationProductResult>()

    fun submitList(movies: List<PresentationProductResult>) {
        movieList.clear()
        movieList.addAll(movies)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
       val binding = ItemSavedBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(movieList[position])
    }

    override fun getItemCount(): Int {
        return movieList.size
    }


    inner class Holder(val binding: ItemSavedBinding): RecyclerView.ViewHolder(binding.root){

        fun bind(product: PresentationProductResult) {
            binding.apply {
                productTitle.text = product.title
                originalPrice.text = "EGP ${product.price}"
                discountPercentage.text = "${product.rating.rate}%"
                Glide.with(binding.root)
                    .load(product.image)
                    .into(productImage)

                root.setOnClickListener {
                    product.id?.let { productsId ->
                        onItemClick(productsId)
                    }
                }
                updateFavoriteIcon(product.id)
                favoriteButton.setOnClickListener {
                    onFavoriteClick(product)
                    updateFavoriteIcon(product.id)
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