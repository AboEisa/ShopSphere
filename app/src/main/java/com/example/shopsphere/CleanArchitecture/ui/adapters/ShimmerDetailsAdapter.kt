package com.example.shopsphere.CleanArchitecture.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.shopsphere.databinding.ItemShimmerDetailsBinding
import com.example.shopsphere.databinding.ItemShimmerHomeBinding
import com.facebook.shimmer.Shimmer

class ShimmerDetailsAdapter(private val itemCount: Int = 6) : RecyclerView.Adapter<ShimmerDetailsAdapter.ShimmerViewHolder>() {



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShimmerViewHolder {
        val binding = ItemShimmerDetailsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ShimmerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ShimmerViewHolder, position: Int) {
        // Optionally customize shimmer programmatically (advanced usage)
         val shimmer = Shimmer.AlphaHighlightBuilder()
             .setDuration(1500)
             .setBaseAlpha(0.7f)
             .setHighlightAlpha(1f)
             .setDirection(Shimmer.Direction.LEFT_TO_RIGHT)
             .build()
         holder.binding.shimmerLayout.setShimmer(shimmer)
        holder.binding.shimmerLayout.startShimmer()
    }

    override fun getItemCount(): Int = itemCount

    override fun onViewDetachedFromWindow(holder: ShimmerViewHolder) {
        super.onViewDetachedFromWindow(holder)
        holder.binding.shimmerLayout.stopShimmer()
    }


    inner class ShimmerViewHolder(val binding: ItemShimmerDetailsBinding) : RecyclerView.ViewHolder(binding.root)
}