package com.example.shopsphere.CleanArchitecture.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.shopsphere.CleanArchitecture.ui.models.PresentationProductResult
import com.example.shopsphere.R
import com.example.shopsphere.databinding.ItemDetailsBinding

class DetailsAdapter(
    private val onFavoriteClick: (Int) -> Unit,
    private val isFavorite: (Int) -> Boolean,
    private val onAddToCartClick: (Int, String) -> Unit,
    private val removeFromCart: (Int, String) -> Unit,
    private val isInCart: (Int, String) -> Boolean,
    private val getCartQuantity: (Int, String) -> Int,
    private val onIncreaseQuantity: (Int, String) -> Unit,
    private val onDecreaseQuantity: (Int, String) -> Unit,
    private val onViewReviews: (Int) -> Unit,
    private val onOutOfStockClick: (String) -> Unit
) : RecyclerView.Adapter<DetailsAdapter.Holder>() {

    var products: MutableList<PresentationProductResult> = mutableListOf()
    private val selectedSizes = mutableMapOf<Int, String>()

    // Optimistic local quantity overrides keyed by "productId:size". null means
    // cart state confirmed by the ViewModel — fall back to real lookups.
    private val optimisticCartQty = mutableMapOf<String, Int>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemDetailsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(products[position])
    }

    override fun getItemCount(): Int = products.size

    /** Called by the fragment whenever the real cart state changes — drop stale overrides. */
    fun syncFromRealCart() {
        optimisticCartQty.clear()
        notifyDataSetChanged()
    }

    private fun cartKey(productId: Int, size: String) = "$productId:${size.uppercase()}"

    inner class Holder(val binding: ItemDetailsBinding) : RecyclerView.ViewHolder(binding.root) {

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
                    .placeholder(R.drawable.ic_image)
                    .error(R.drawable.ic_image)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(productImage)

                updateFavoriteIcon(product.id)
                val showSizeSelector = shouldShowSizeSelector(product.category)
                textSizeLabel.isVisible = showSizeSelector
                layoutSizeOptions.isVisible = showSizeSelector
                if (!showSizeSelector) {
                    selectedSizes.remove(product.id)
                }

                renderCartState(product, showSizeSelector)

                favoriteButton.setOnClickListener {
                    onFavoriteClick(product.id)
                    updateFavoriteIcon(product.id)
                }

                addToCartButton.setOnClickListener {
                    val size = resolveCartSize(product, showSizeSelector)
                    val stock = product.stock.coerceAtLeast(0)
                    if (stock <= 0) {
                        onOutOfStockClick(product.title)
                        return@setOnClickListener
                    }
                    // Optimistic: flip to stepper with qty=1 immediately.
                    optimisticCartQty[cartKey(product.id, size)] = 1
                    renderCartState(product, showSizeSelector)
                    onAddToCartClick(product.id, size)
                }

                btnIncreaseDetails.setOnClickListener {
                    val size = resolveCartSize(product, showSizeSelector)
                    val stock = product.stock.coerceAtLeast(0)
                    val current = currentCartQty(product.id, size)
                    if (stock in 1..current) {
                        onOutOfStockClick(product.title)
                        return@setOnClickListener
                    }
                    optimisticCartQty[cartKey(product.id, size)] = current + 1
                    renderCartState(product, showSizeSelector)
                    onIncreaseQuantity(product.id, size)
                }

                btnDecreaseDetails.setOnClickListener {
                    val size = resolveCartSize(product, showSizeSelector)
                    val current = currentCartQty(product.id, size)
                    if (current <= 1) {
                        optimisticCartQty[cartKey(product.id, size)] = 0
                        renderCartState(product, showSizeSelector)
                        removeFromCart(product.id, size)
                    } else {
                        optimisticCartQty[cartKey(product.id, size)] = current - 1
                        renderCartState(product, showSizeSelector)
                        onDecreaseQuantity(product.id, size)
                    }
                }

                buttonViewReviews.setOnClickListener {
                    onViewReviews(product.id)
                }

                if (showSizeSelector) {
                    setupSizeSelector(product)
                }
            }
        }

        private fun renderCartState(product: PresentationProductResult, showSizeSelector: Boolean) {
            val size = resolveCartSize(product, showSizeSelector)
            val qty = currentCartQty(product.id, size)
            val stock = product.stock.coerceAtLeast(0)

            if (qty > 0) {
                binding.addToCartButton.isVisible = false
                binding.layoutQuantityStepper.isVisible = true
                binding.textQuantityDetails.text = qty.toString()
                binding.btnIncreaseDetails.isEnabled = stock <= 0 || qty < stock
                binding.btnIncreaseDetails.alpha = if (binding.btnIncreaseDetails.isEnabled) 1f else 0.4f
            } else {
                binding.layoutQuantityStepper.isVisible = false
                binding.addToCartButton.isVisible = true
                val button = binding.addToCartButton
                if (stock <= 0) {
                    button.text = button.context.getString(R.string.out_of_stock)
                    button.backgroundTintList =
                        android.content.res.ColorStateList.valueOf(
                            ContextCompat.getColor(button.context, R.color.gray)
                        )
                    button.isEnabled = false
                } else {
                    button.text = button.context.getString(R.string.add_to_cart)
                    button.backgroundTintList =
                        android.content.res.ColorStateList.valueOf(
                            ContextCompat.getColor(button.context, R.color.bright_green)
                        )
                    button.isEnabled = true
                }
            }
        }

        private fun currentCartQty(productId: Int, size: String): Int {
            optimisticCartQty[cartKey(productId, size)]?.let { return it }
            val real = getCartQuantity(productId, size)
            if (real > 0) return real
            return if (isInCart(productId, size)) 1 else 0
        }

        private fun shouldShowSizeSelector(category: String): Boolean {
            val normalized = category.trim().lowercase()
            val clothingKeywords = listOf(
                "clothing", "shirt", "dress", "top", "jacket",
                "jeans", "trouser", "pants", "hoodie", "sweater", "coat"
            )
            return clothingKeywords.any { keyword -> normalized.contains(keyword) }
        }

        private fun setupSizeSelector(product: PresentationProductResult) {
            val productId = product.id
            selectedSizes[productId] = currentSelectedSize(productId)

            val options = mapOf(
                "S" to binding.sizeS,
                "M" to binding.sizeM,
                "L" to binding.sizeL
            )

            options.forEach { (size, view) ->
                view.setOnClickListener {
                    selectedSizes[productId] = size
                    renderSelectedSize(productId)
                    renderCartState(product, showSizeSelector = true)
                }
            }
            renderSelectedSize(productId)
        }

        private fun renderSelectedSize(productId: Int) {
            val selected = currentSelectedSize(productId)
            val options = mapOf(
                "S" to binding.sizeS,
                "M" to binding.sizeM,
                "L" to binding.sizeL
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

        private fun currentSelectedSize(productId: Int): String {
            return selectedSizes[productId] ?: "M"
        }

        private fun resolveCartSize(
            product: PresentationProductResult,
            showSizeSelector: Boolean
        ): String {
            return if (showSizeSelector) currentSelectedSize(product.id) else ""
        }

        private fun updateFavoriteIcon(productId: Int) {
            val color = ContextCompat.getColor(
                binding.root.context,
                if (isFavorite(productId)) R.color.red else R.color.gray
            )
            binding.favoriteButton.setColorFilter(color, android.graphics.PorterDuff.Mode.SRC_IN)
        }
    }
}
