package com.example.shopsphere.CleanArchitecture.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.shopsphere.CleanArchitecture.ui.models.PresentationProductResult
import com.example.shopsphere.CleanArchitecture.utils.formatEgpPrice
import com.example.shopsphere.R
import com.example.shopsphere.databinding.ItemCartBinding

class CartAdapter(
    private val onItemClick: (Int) -> Unit,
    private val onRemoveClick: (String) -> Unit,
    private val onQuantityChanged: (String, Int) -> Unit,
    private val onStockLimitReached: (String, Int) -> Unit
) : ListAdapter<PresentationProductResult, CartAdapter.Holder>(DIFF) {

    /** Locally tracked quantities — decoupled from the list so +/- taps don't rebuild the adapter. */
    private val productQuantities = mutableMapOf<String, Int>()

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long {
        return getItem(position).cartLineId.hashCode().toLong()
    }

    private fun formatPrice(price: Double): String = formatEgpPrice(price)

    override fun submitList(list: List<PresentationProductResult>?) {
        list?.forEach { productQuantities[it.cartLineId] = it.quantity }
        super.submitList(list)
    }

    fun getItems(): List<PresentationProductResult> {
        return currentList.map { product ->
            product.copy(quantity = productQuantities[product.cartLineId] ?: product.quantity)
        }
    }

    fun getTotalPrice(): Double {
        return currentList.sumOf { product ->
            val quantity = productQuantities[product.cartLineId] ?: 1
            product.price * quantity
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemCartBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val product = getItem(position)
        val quantity = productQuantities[product.cartLineId] ?: 1
        holder.bind(product, quantity)
    }

    inner class Holder(private val binding: ItemCartBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(product: PresentationProductResult, quantity: Int) {
            binding.apply {
                productTitle.text = product.title
                val hasSize = product.selectedSize.isNotBlank()
                textProductSize.isVisible = hasSize
                if (hasSize) {
                    textProductSize.text = root.context.getString(
                        R.string.orders_size_value,
                        product.selectedSize
                    )
                }

                val unitPrice = product.price
                val totalPrice = unitPrice * quantity

                productPrice.text = formatPrice(totalPrice)
                productQuantity.text = quantity.toString()

                Glide.with(root)
                    .load(product.image)
                    .placeholder(R.drawable.ic_image)
                    .error(R.drawable.ic_image)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(productImage)

                root.setOnClickListener { onItemClick(product.id) }
                removeButton.setOnClickListener { onRemoveClick(product.cartLineId) }

                btnIncrease.setOnClickListener {
                    val currentQuantity = productQuantities[product.cartLineId] ?: quantity
                    val stock = product.stock.coerceAtLeast(0)
                    if (stock <= 0) {
                        onStockLimitReached(product.title, 0)
                        return@setOnClickListener
                    }
                    if (currentQuantity >= stock) {
                        onStockLimitReached(product.title, stock)
                        return@setOnClickListener
                    }

                    val newQuantity = currentQuantity + 1
                    productQuantities[product.cartLineId] = newQuantity
                    productQuantity.text = newQuantity.toString()
                    productPrice.text = formatPrice(unitPrice * newQuantity)

                    onQuantityChanged(product.cartLineId, newQuantity)
                }

                btnDecrease.setOnClickListener {
                    val currentQuantity = productQuantities[product.cartLineId] ?: quantity
                    if (currentQuantity > 1) {
                        val newQuantity = currentQuantity - 1
                        productQuantities[product.cartLineId] = newQuantity
                        productQuantity.text = newQuantity.toString()
                        productPrice.text = formatPrice(unitPrice * newQuantity)

                        onQuantityChanged(product.cartLineId, newQuantity)
                    }
                }
            }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<PresentationProductResult>() {
            override fun areItemsTheSame(
                oldItem: PresentationProductResult,
                newItem: PresentationProductResult
            ): Boolean = oldItem.cartLineId == newItem.cartLineId

            override fun areContentsTheSame(
                oldItem: PresentationProductResult,
                newItem: PresentationProductResult
            ): Boolean = oldItem == newItem
        }
    }
}
