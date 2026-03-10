package com.example.ku_connect.ui.common


import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.ku_connect.R
import com.example.ku_connect.data.model.Comment
import com.example.ku_connect.databinding.ItemCommentDetailBinding
import com.example.ku_connect.util.Extensions.toAvatarColors
import com.example.ku_connect.util.Extensions.toInitial
import com.example.ku_connect.util.Extensions.toRelativeTime

class CommentAdapter : ListAdapter<Comment, CommentAdapter.ViewHolder>(DIFF) {
    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Comment>() {
            override fun areItemsTheSame(a: Comment, b: Comment) = a.id == b.id
            override fun areContentsTheSame(a: Comment, b: Comment) = a == b
        }
    }

    inner class ViewHolder(private val binding: ItemCommentDetailBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(comment: Comment) {
            val (bgColor, textColor) = comment.authorName.toAvatarColors(comment.authorColor)
            val oval = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(bgColor)
            }

            binding.tvAvatar.background = oval
            binding.tvAvatar.text       = comment.authorName.toInitial()
            binding.tvAvatar.setTextColor(textColor)

            binding.tvAuthor.text = comment.authorName
            binding.tvTime.text   = comment.createdAt?.toRelativeTime() ?: ""

            if (comment.content.isNotBlank()) {
                binding.tvContent.visibility = View.VISIBLE
                binding.tvContent.text = comment.content
            } else {
                binding.tvContent.visibility = View.GONE
            }

            if (!comment.imageUrl.isNullOrBlank()) {
                binding.ivImage.visibility = View.VISIBLE
                Glide.with(binding.root)
                    .load(comment.imageUrl)
                    .centerCrop()
                    .placeholder(R.color.ios_fill)
                    .into(binding.ivImage)
            } else {
                binding.ivImage.visibility = View.GONE
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCommentDetailBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )

        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}