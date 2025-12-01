package com.example.shopsphere.CleanArchitecture.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.shopsphere.CleanArchitecture.ui.models.PresentationProductResult
import com.example.shopsphere.databinding.ItemCartBinding
import com.example.shopsphere.databinding.ItemSearchBinding
import java.text.NumberFormat
import java.util.Locale

class SearchAdapter(
    private val onItemClick: (Int) -> Unit
) : RecyclerView.Adapter<SearchAdapter.Holder>() {

    private var products: MutableList<PresentationProductResult> = mutableListOf()
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US).apply {
        maximumFractionDigits = 2
    }

    private val productQuantities = mutableMapOf<Int, Int>()

    fun submitList(product: List<PresentationProductResult>) {
        products.clear()
        products.addAll(product)
        notifyDataSetChanged()
    }



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemSearchBinding.inflate(
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

    inner class Holder(private val binding: ItemSearchBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(product: PresentationProductResult, quantity: Int) {
            binding.apply {
                productTitle.text = product.title
                Glide.with(root)
                    .load(product.image)
                    .into(productImage)
                root.setOnClickListener { onItemClick(product.id) }
            }
        }
    }
}