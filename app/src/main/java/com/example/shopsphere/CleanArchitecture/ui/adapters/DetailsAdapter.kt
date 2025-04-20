package com.example.shopsphere.CleanArchitecture.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.shopsphere.CleanArchitecture.ui.adapters.DetailsAdapter.Holder
import com.example.shopsphere.CleanArchitecture.ui.models.PresentationProductResult
import com.example.shopsphere.databinding.ItemDetailsBinding

class DetailsAdapter : RecyclerView.Adapter<Holder>() {

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
//                productTitle.text = product.title
//                originalPrice.text = "$${product.price}"
//                discountPercentage.text = "${product.rating.rate}%"
//                reviewCount.text = "${product.rating.count} reviews"
//                description.text = product.description
//                Glide.with(binding.root)
//                    .load(product.image)
//                    .into(productImage)
            }
        }
    }
}