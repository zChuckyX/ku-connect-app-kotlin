package com.example.ku_connect.ui.common

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.ku_connect.R
import com.example.ku_connect.data.model.MarketItem
import com.example.ku_connect.databinding.ItemMarketBinding
import com.example.ku_connect.util.AppConfig

class MarketAdapter(
    private val onOpenLine: (MarketItem) -> Unit
) : ListAdapter<MarketItem, MarketAdapter.MarketViewHolder>(DIFF) {
    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<MarketItem>() {
            override fun areItemsTheSame(a: MarketItem, b: MarketItem) = a.id == b.id
            override fun areContentsTheSame(a: MarketItem, b: MarketItem) = a == b
        }
    }

    inner class MarketViewHolder(private val binding: ItemMarketBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MarketItem) {
            binding.tvShopName.text = item.shopName
            binding.tvSellerName.text = "โพสต์โดย ${item.sellerName}"
            binding.tvDescription.text = item.description

            if (!item.imageUrl.isNullOrBlank()) {
                Glide.with(binding.root)
                    .load(item.imageUrl)
                    .centerCrop()
                    .placeholder(R.color.ios_fill)
                    .into(binding.ivShopImage)
            } else {
                Glide.with(binding.root)
                    .load(AppConfig.DEFAULT_SHOP_PICTURE)
                    .centerCrop()
                    .placeholder(R.color.ios_fill)
                    .into(binding.ivShopImage)
            }

            binding.tvOpenLine.setOnClickListener {
                onOpenLine(item)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MarketViewHolder {
        val binding = ItemMarketBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)
        return MarketViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MarketViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}