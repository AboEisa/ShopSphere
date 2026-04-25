package com.example.shopsphere.CleanArchitecture.ui.adapters

import android.graphics.PorterDuff
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.shopsphere.CleanArchitecture.ui.models.PresentationProductResult
import com.example.shopsphere.CleanArchitecture.utils.formatEgpPrice
import com.example.shopsphere.R
import com.example.shopsphere.databinding.ItemHomeBinding

class HomeProductsAdapter(
    private val onFavoriteClick: (Int) -> Unit,
    private val isFavorite: (Int) -> Boolean,
    private val onItemClick: (Int) -> Unit
) : ListAdapter<PresentationProductResult, HomeProductsAdapter.Holder>(DIFF) {

    /**
     * Optional canonical favorite-id set, updated from the fragment whenever the
     * favorites LiveData emits. When present, the adapter re-binds the heart
     * icon for every visible product so the state always matches the source of
     * truth, even after a scroll/refresh. Fallback is [isFavorite].
     */
    private var favoriteIds: Set<Int> = emptySet()

    fun updateFavoriteIds(ids: Set<Int>) {
        if (ids == favoriteIds) return
        favoriteIds = ids
        notifyItemRangeChanged(0, itemCount, PAYLOAD_FAV)
    }

    init {
        setHasStableIds(true)
    }

    /** Legacy shim — callers still invoke `adapter.products = ...` or `submitList(list)`. */
    var products: List<PresentationProductResult>
        get() = currentList
        set(value) = submitList(value)

    override fun getItemId(position: Int): Long = getItem(position).id.toLong()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemHomeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onBindViewHolder(holder: Holder, position: Int, payloads: MutableList<Any>) {
        if (payloads.contains(PAYLOAD_FAV)) {
            holder.rebindFavoriteOnly(getItem(position).id)
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    /**
     * After every `submitList` commit — including when the new list is identical
     * to the old one and DiffUtil skips rebinds — re-notify every row with the
     * favorite payload. Without this, returning to Home after toggling a heart
     * on Details leaves the grid still showing the pre-toggle colors because
     * the product data itself didn't change.
     */
    override fun onCurrentListChanged(
        previousList: MutableList<PresentationProductResult>,
        currentList: MutableList<PresentationProductResult>
    ) {
        super.onCurrentListChanged(previousList, currentList)
        if (itemCount > 0) {
            notifyItemRangeChanged(0, itemCount, PAYLOAD_FAV)
        }
    }

    inner class Holder(private val binding: ItemHomeBinding) : RecyclerView.ViewHolder(binding.root) {

        private val redColor = ContextCompat.getColor(binding.root.context, R.color.red)
        private val grayColor = ContextCompat.getColor(binding.root.context, R.color.gray)

        fun bind(product: PresentationProductResult) {
            binding.apply {
                productTitle.text = product.title
                originalPrice.text = formatEgpPrice(product.price)
                discountPercentage.text = "\u2605 ${String.format("%.1f", product.rating.rate)}"

                Glide.with(root)
                    .load(product.image)
                    .placeholder(R.drawable.ic_image)
                    .error(R.drawable.ic_image)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(productImage)

                updateFavoriteIcon(product.id)

                favoriteButton.setOnClickListener {
                    onFavoriteClick(product.id)
                    updateFavoriteIcon(product.id)
                }
                root.setOnClickListener { onItemClick(product.id) }
            }
        }

        fun rebindFavoriteOnly(productId: Int) {
            updateFavoriteIcon(productId)
        }

        private fun updateFavoriteIcon(productId: Int) {
            // Single source of truth: the canonical LiveData-backed set pushed in
            // from SavedViewModel via updateFavoriteIds(). The lambda fallback is
            // only used on the very first bind, before the favorites observer has
            // fired — after that, every rebind reads from favoriteIds.
            val isFav = productId in favoriteIds || (favoriteIds.isEmpty() && isFavorite(productId))
            val color = if (isFav) redColor else grayColor
            binding.favoriteButton.setColorFilter(color, PorterDuff.Mode.SRC_IN)
        }
    }

    companion object {
        private const val PAYLOAD_FAV = "payload_fav"

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
