package com.example.shopsphere.CleanArchitecture.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.shopsphere.CleanArchitecture.ui.models.PresentationProductResult
import com.example.shopsphere.databinding.ItemCartBinding
import java.text.DecimalFormat
import java.util.Locale

class CartAdapter(
    private val onItemClick: (Int) -> Unit,
    private val onRemoveClick: (Int) -> Unit,
    private val onQuantityChanged: (Int, Int) -> Unit
) : RecyclerView.Adapter<CartAdapter.Holder>() {

    private var products: MutableList<PresentationProductResult> = mutableListOf()

    // without $
    private val currencyFormat = DecimalFormat("#,##0.00").apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }

    private val productQuantities = mutableMapOf<Int, Int>()

    // Function لتنسيق السعر
    private fun formatPrice(price: Double): String {
        return "EGP${currencyFormat.format(price)}"
    }

    fun submitList(product: List<PresentationProductResult>) {
        products.clear()
        products.addAll(product)
        // Always sync quantities from the product list
        product.forEach {
            productQuantities[it.id] = it.quantity ?: 1
        }
        notifyDataSetChanged()
    }

    fun getItems(): List<PresentationProductResult> = products.toList()

    fun getTotalPrice(): Double {
        return products.sumOf { product ->
            val quantity = productQuantities[product.id] ?: 1
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
        val quantity = productQuantities[product.id] ?: 1
        holder.bind(product, quantity)
    }

    override fun getItemCount(): Int = products.size

    inner class Holder(private val binding: ItemCartBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(product: PresentationProductResult, quantity: Int) {
            binding.apply {
                productTitle.text = product.title
                val unitPrice = product.price
                val totalPrice = unitPrice * quantity

                productPrice.text = formatPrice(totalPrice)
                productQuantity.text = quantity.toString()

                Glide.with(root)
                    .load(product.image)
                    .into(productImage)

                root.setOnClickListener { onItemClick(product.id) }
                removeButton.setOnClickListener { onRemoveClick(product.id) }

                btnIncrease.setOnClickListener {
                    val newQuantity = quantity + 1
                    productQuantities[product.id] = newQuantity
                    productQuantity.text = newQuantity.toString()

                    val newTotalPrice = unitPrice * newQuantity
                    productPrice.text = formatPrice(newTotalPrice)

                    onQuantityChanged(product.id, newQuantity)
                }

                btnDecrease.setOnClickListener {
                    if (quantity > 1) {
                        val newQuantity = quantity - 1
                        productQuantities[product.id] = newQuantity
                        productQuantity.text = newQuantity.toString()
                        val newTotalPrice = unitPrice * newQuantity
                        productPrice.text = formatPrice(newTotalPrice)

                        onQuantityChanged(product.id, newQuantity)
                    }
                }
            }
        }
    }
}