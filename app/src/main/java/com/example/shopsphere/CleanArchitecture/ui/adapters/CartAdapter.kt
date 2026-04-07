package com.example.shopsphere.CleanArchitecture.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.shopsphere.CleanArchitecture.ui.models.PresentationProductResult
import com.example.shopsphere.databinding.ItemCartBinding
import java.text.DecimalFormat

class CartAdapter(
    private val onItemClick: (Int) -> Unit,
    private val onRemoveClick: (String) -> Unit,
    private val onQuantityChanged: (String, Int) -> Unit,
    private val onStockLimitReached: (String, Int) -> Unit
) : RecyclerView.Adapter<CartAdapter.Holder>() {

    private var products: MutableList<PresentationProductResult> = mutableListOf()

    // Currency formatter without $ symbol
    private val currencyFormat = DecimalFormat("#,##0.00").apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }

    private val productQuantities = mutableMapOf<String, Int>()

    // Function to format price
    private fun formatPrice(price: Double): String {
        return "EGP ${currencyFormat.format(price)}" // Added space after EGP
    }

    fun submitList(product: List<PresentationProductResult>) {
        products.clear()
        products.addAll(product)
        product.forEach {
            productQuantities[it.cartLineId] = it.quantity
        }
        notifyDataSetChanged()
    }

    fun getItems(): List<PresentationProductResult> {
        return products.map { product ->
            product.copy(quantity = productQuantities[product.cartLineId] ?: product.quantity)
        }
    }

    fun getTotalPrice(): Double {
        return products.sumOf { product ->
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
        val product = products[position]
        val quantity = productQuantities[product.cartLineId] ?: 1
        holder.bind(product, quantity)
    }

    override fun getItemCount(): Int = products.size

    inner class Holder(private val binding: ItemCartBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(product: PresentationProductResult, quantity: Int) {
            binding.apply {
                productTitle.text = product.title
                val hasSize = product.selectedSize.isNotBlank()
                textProductSize.isVisible = hasSize
                if (hasSize) {
                    textProductSize.text = root.context.getString(
                        com.example.shopsphere.R.string.orders_size_value,
                        product.selectedSize
                    )
                }

                val unitPrice = product.price
                val totalPrice = unitPrice * quantity

                productPrice.text = formatPrice(totalPrice)
                productQuantity.text = quantity.toString()

                Glide.with(root)
                    .load(product.image)
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

                    val newTotalPrice = unitPrice * newQuantity
                    productPrice.text = formatPrice(newTotalPrice)

                    onQuantityChanged(product.cartLineId, newQuantity)
                }

                btnDecrease.setOnClickListener {
                    val currentQuantity = productQuantities[product.cartLineId] ?: quantity
                    if (currentQuantity > 1) {
                        val newQuantity = currentQuantity - 1
                        productQuantities[product.cartLineId] = newQuantity
                        productQuantity.text = newQuantity.toString()

                        val newTotalPrice = unitPrice * newQuantity
                        productPrice.text = formatPrice(newTotalPrice)

                        onQuantityChanged(product.cartLineId, newQuantity)
                    }
                }
            }
        }
    }
}
