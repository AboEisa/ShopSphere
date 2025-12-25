package com.example.shopsphere.CleanArchitecture.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.shopsphere.CleanArchitecture.ui.models.AddressBookItem
import com.example.shopsphere.R
import com.example.shopsphere.databinding.ItemAddressBookBinding

class AddressBookAdapter(
    private val onAddressClicked: (AddressBookItem) -> Unit
) : RecyclerView.Adapter<AddressBookAdapter.AddressViewHolder>() {

    private val addresses = mutableListOf<AddressBookItem>()

    fun submitList(items: List<AddressBookItem>) {
        addresses.clear()
        addresses.addAll(items)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AddressViewHolder {
        val binding = ItemAddressBookBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AddressViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AddressViewHolder, position: Int) {
        holder.bind(addresses[position])
    }

    override fun getItemCount(): Int = addresses.size

    inner class AddressViewHolder(private val binding: ItemAddressBookBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: AddressBookItem) {
            binding.textTitle.text = item.title
            binding.textAddress.text = item.address
            binding.textDefaultBadge.visibility = if (item.isDefault) android.view.View.VISIBLE else android.view.View.GONE

            val rootBackground = if (item.isSelected) {
                R.drawable.bg_wallet_card_selected
            } else {
                R.drawable.bg_wallet_card
            }
            binding.rootAddress.background = ContextCompat.getDrawable(itemView.context, rootBackground)

            val indicatorDrawable = if (item.isSelected) {
                R.drawable.bg_select_indicator_selected
            } else {
                R.drawable.bg_select_indicator
            }
            binding.selectIndicator.setImageDrawable(
                ContextCompat.getDrawable(itemView.context, indicatorDrawable)
            )

            binding.rootAddress.setOnClickListener { onAddressClicked(item) }
        }
    }
}
