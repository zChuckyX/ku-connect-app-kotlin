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
import com.example.ku_connect.data.model.Post
import com.example.ku_connect.databinding.ItemMyPostBinding
import com.example.ku_connect.util.Extensions.toAvatarColors
import com.example.ku_connect.util.Extensions.toInitial
import com.example.ku_connect.util.Extensions.toRelativeTime

class MyPostAdapter(
    private val onEdit: (Post) -> Unit,
    private val onDelete: (Post) -> Unit,
    private val onDetail: (Post) -> Unit
) : ListAdapter<Post, MyPostAdapter.ViewHolder>(DIFF) {
    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Post>() {
            override fun areItemsTheSame(a: Post, b: Post) = a.id == b.id
            override fun areContentsTheSame(a: Post, b: Post) = a == b
        }
    }

    inner class ViewHolder(private val binding: ItemMyPostBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(post: Post) {
            val (bgColor, textColor) = post.authorName.toAvatarColors(post.authorColor)
            val oval = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(bgColor)
            }

            binding.tvAvatar.background = oval
            binding.tvAvatar.text       = post.authorName.toInitial()
            binding.tvAvatar.setTextColor(textColor)

            binding.tvAuthor.text      = post.authorName
            binding.tvTime.text        = post.createdAt?.toRelativeTime() ?: ""
            binding.tvTitle.text       = post.title
            binding.tvContent.text     = post.content
            binding.tvLikeCount.text   = "${post.likeCount} ถูกใจ"
            binding.tvCommentCount.text = "${post.commentCount} ความคิดเห็น"

            if (!post.imageUrl.isNullOrBlank()) {
                binding.ivPostImage.visibility = View.VISIBLE
                Glide.with(binding.root)
                    .load(post.imageUrl)
                    .centerCrop()
                    .placeholder(R.color.ios_fill)
                    .into(binding.ivPostImage)
            } else {
                binding.ivPostImage.visibility = View.GONE
            }

            binding.btnEdit.setOnClickListener {
                onEdit(post)
            }

            binding.btnDelete.setOnClickListener {
                onDelete(post)
            }

            binding.root.setOnClickListener {
                onDetail(post)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMyPostBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}