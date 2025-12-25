package com.example.shopsphere.CleanArchitecture.ui.adapters

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.shopsphere.CleanArchitecture.ui.models.PresentationProductResult
import com.example.shopsphere.R
import com.example.shopsphere.databinding.ItemDetailsBinding

class DetailsAdapter(
    private val onFavoriteClick: (Int) -> Unit,
    private val isFavorite: (Int) -> Boolean,
    private val onAddToCartClick: (Int) -> Unit,
    private val removeFromCart: (Int) -> Unit,
    private val isInCart: (Int) -> Boolean,
    private val onViewReviews: (Int) -> Unit,
    private val onOutOfStockClick: (String) -> Unit
) : RecyclerView.Adapter<DetailsAdapter.Holder>() {

    var products: MutableList<PresentationProductResult> = mutableListOf()
    private val selectedSizes = mutableMapOf<Int, String>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemDetailsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val product = products[position]
        holder.bind(product)
    }

    override fun getItemCount(): Int {
        return products.size
    }

    inner class Holder(val binding: ItemDetailsBinding): RecyclerView.ViewHolder(binding.root) {

        fun bind(product: PresentationProductResult) {
            binding.apply {
                productTitle.text = product.title
                productPrice.text = "EGP ${"%,.0f".format(product.price)}"
                productDescription.text = product.description
                rate.text = "${product.rating.rate}/5"
                rateCount.text = root.context.getString(
                    R.string.reviews_count_inline,
                    product.rating.count.coerceAtLeast(0)
                )

                Glide.with(binding.root)
                    .load(product.image)
                    .into(productImage)

                updateFavoriteIcon(product.id)
                updateCartButton(product)
                val showSizeSelector = shouldShowSizeSelector(product.category)
                textSizeLabel.isVisible = showSizeSelector
                layoutSizeOptions.isVisible = showSizeSelector

                favoriteButton.setOnClickListener {
                    onFavoriteClick(product.id)
                    updateFavoriteIcon(product.id)
                }


                addToCartButton.setOnClickListener {
                    val stock = product.rating.count.coerceAtLeast(0)
                    if (isInCart(product.id)) {
                        removeFromCart(product.id)
                        itemView.post { updateCartButton(product) }
                    } else {
                        if (stock <= 0) {
                            onOutOfStockClick(product.title)
                            return@setOnClickListener
                        }
                        onAddToCartClick(product.id)
                        itemView.post { updateCartButton(product) }
                    }
                }

                buttonViewReviews.setOnClickListener {
                    onViewReviews(product.id)
                }

                if (showSizeSelector) {
                    setupSizeSelector(product.id)
                }
            }
        }

        private fun shouldShowSizeSelector(category: String): Boolean {
            val normalized = category.trim().lowercase()
            val clothingKeywords = listOf(
                "clothing", "shirt", "dress", "top", "jacket",
                "jeans", "trouser", "pants", "hoodie", "sweater", "coat"
            )
            return clothingKeywords.any { keyword -> normalized.contains(keyword) }
        }

        private fun setupSizeSelector(productId: Int) {
            val selectedSize = selectedSizes[productId] ?: "M"
            selectedSizes[productId] = selectedSize

            val options = mapOf(
                "S" to binding.sizeS,
                "M" to binding.sizeM,
                "L" to binding.sizeL,
                "XL" to binding.sizeXL
            )

            options.forEach { (size, view) ->
                view.setOnClickListener {
                    selectedSizes[productId] = size
                    renderSelectedSize(productId)
                }
            }
            renderSelectedSize(productId)
        }

        private fun renderSelectedSize(productId: Int) {
            val selected = selectedSizes[productId] ?: "M"
            val options = mapOf(
                "S" to binding.sizeS,
                "M" to binding.sizeM,
                "L" to binding.sizeL,
                "XL" to binding.sizeXL
            )

            options.forEach { (size, view) ->
                val isSelected = selected == size
                view.background = ContextCompat.getDrawable(
                    view.context,
                    if (isSelected) R.drawable.bg_size_chip_selected else R.drawable.bg_size_chip_unselected
                )
                view.setTextColor(
                    ContextCompat.getColor(
                        view.context,
                        if (isSelected) android.R.color.white else R.color._1a1a1a
                    )
                )
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



        private fun updateCartButton(product: PresentationProductResult) {
            val button = binding.addToCartButton
            val inCart = isInCart(product.id)
            val stock = product.rating.count.coerceAtLeast(0)

            if (inCart) {
                button.text = button.context.getString(R.string.remove_from_cart)
                button.backgroundTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(button.context, R.color.ff3434))
                button.isEnabled = true
            } else if (stock <= 0) {
                button.text = button.context.getString(R.string.out_of_stock)
                button.backgroundTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(button.context, R.color.gray))
                button.isEnabled = false
            } else {
                button.text = button.context.getString(R.string.add_to_cart)
                button.backgroundTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(button.context, R.color.bright_green))
                button.isEnabled = true
            }
        }
    }
}
