package com.example.shopsphere.CleanArchitecture.ui.adapters

import android.animation.ValueAnimator
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
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
    private val isInCart: (Int) -> Boolean
) : RecyclerView.Adapter<DetailsAdapter.Holder>() {

    var products: MutableList<PresentationProductResult> = mutableListOf()

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
                productPrice.text = "$${product.price}"
                productDescription.text = product.description
                rate.text = "${product.rating.rate}/5"
                rateCount.text = "(${product.rating.count} reviews)"

                Glide.with(binding.root)
                    .load(product.image)
                    .into(productImage)

                updateFavoriteIcon(product.id)
                updateCartButton(product.id)

                favoriteButton.setOnClickListener {
                    onFavoriteClick(product.id)
                    updateFavoriteIcon(product.id)
                }


                addToCartButton.setOnClickListener {
                    if (isInCart(product.id)) {
                        removeFromCart(product.id)
                        updateCartButton(product.id)
                    } else {
                        onAddToCartClick(product.id)
                        updateCartButton(product.id)
                    }
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

        private var isFirstInteraction = false // بدل isFirstTime

        private fun updateCartButton(productId: Int) {
            val button = binding.addToCartButton
            val context = binding.root.context

            val progressBar = binding.buttonProgressBar
            progressBar.visibility = View.VISIBLE
            progressBar.isIndeterminate = true

            val inCart = isInCart(productId)

            button.animate()
                .scaleX(0.9f)
                .scaleY(0.9f)
                .alpha(0.6f)
                .setDuration(200)
                .withEndAction {
                    button.text = if (inCart) "Remove from Cart" else "Add to Cart"
                    button.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .alpha(1f)
                        .setDuration(200)
                        .withEndAction {
                            progressBar.visibility = View.GONE
                        }
                        .start()
                }
                .start()

            // هنا الاختلاف
            val fromColor = if (inCart) ContextCompat.getColor(context, R.color.black) else ContextCompat.getColor(context, R.color.red)
            val toColor = if (inCart) ContextCompat.getColor(context, R.color.red) else ContextCompat.getColor(context, R.color.black)

            val colorAnim = ValueAnimator.ofArgb(fromColor, toColor)
            colorAnim.duration = 300
            colorAnim.addUpdateListener { animator ->
                button.setBackgroundColor(animator.animatedValue as Int)
            }

            if (isFirstInteraction) {
                colorAnim.start() // لو خلاص حصل أول تفاعل، ابدأ الأنيميشن عادي
            } else {
                button.setBackgroundColor(ContextCompat.getColor(context, R.color.black)) // أول ظهور ثابت
                isFirstInteraction = true // بعدها خلاص نسمح بالأنيميشن
            }
        }




    }
}