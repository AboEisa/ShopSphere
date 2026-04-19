package com.example.shopsphere.CleanArchitecture.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.shopsphere.CleanArchitecture.ui.models.PresentationProductResult
import com.example.shopsphere.R
import com.example.shopsphere.databinding.ItemSearchBinding

class SearchAdapter(
    private val onItemClick: (Int) -> Unit
) : ListAdapter<PresentationProductResult, SearchAdapter.Holder>(DIFF) {

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long = getItem(position).id.toLong()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemSearchBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class Holder(private val binding: ItemSearchBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(product: PresentationProductResult) {
            binding.apply {
                productTitle.text = product.title
                Glide.with(root)
                    .load(product.image)
                    .placeholder(R.drawable.ic_image)
                    .error(R.drawable.ic_image)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(productImage)
                root.setOnClickListener { onItemClick(product.id) }
            }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<PresentationProductResult>() {
            override fun areItemsTheSame(
                oldItem: PresentationProductResult,
                newItem: PresentationProductResult
            ): Boolean = oldItem.id == newItem.id

            override fun areContentsTheSame(
                oldItem: PresentationProductResult,
                newItem: PresentationProductResult
            ): Boolean = oldItem == newItem
        }
    }
}
