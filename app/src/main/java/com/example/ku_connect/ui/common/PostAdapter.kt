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
import com.example.ku_connect.databinding.ItemPostBinding
import com.example.ku_connect.util.Extensions.toAvatarColors
import com.example.ku_connect.util.Extensions.toInitial
import com.example.ku_connect.util.Extensions.toRelativeTime

class PostAdapter(
    private val onClickDetail: (post: Post) -> Unit,
    private val currentUserId: String,
    private val onLike: (Post, Boolean) -> Unit,
    private val onComment: (Post) -> Unit,
    private val onShare: (Post) -> Unit
) : ListAdapter<Post, PostAdapter.PostViewHolder>(DIFF) {
    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Post>() {
            override fun areItemsTheSame(a: Post, b: Post) = a.id == b.id
            override fun areContentsTheSame(a: Post, b: Post) = a == b
        }
    }

    inner class PostViewHolder(private val binding: ItemPostBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(post: Post) {
            val (bgColor, textColor) = post.authorName.toAvatarColors(post.authorColor)

            (binding.tvAvatar.background as? GradientDrawable)?.setColor(bgColor)
                ?: run {
                    val d = GradientDrawable()
                    d.shape = GradientDrawable.OVAL
                    d.setColor(bgColor)
                    binding.tvAvatar.background = d
                }
            binding.tvAvatar.text = post.authorName.toInitial()
            binding.tvAvatar.setTextColor(textColor)

            binding.tvAuthor.text = post.authorName
            binding.tvTime.text = post.createdAt?.toRelativeTime() ?: ""
            binding.tvTitle.text = post.title
            binding.tvContent.text = post.content

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

            var liked = post.likedBy.contains(currentUserId)
            binding.ivLike.setImageResource(
                if (liked) R.drawable.ic_heart_filled
                else R.drawable.ic_heart_outline
            )
            binding.tvLikeCount.text = post.likeCount.toString()
            binding.tvCommentCount.text = post.commentCount.toString()

            binding.btnDetailPost.setOnClickListener {
                onClickDetail(post)
            }

            binding.btnLike.setOnClickListener {
                liked = !liked

                binding.ivLike.setImageResource(
                    if (liked) {
                        R.drawable.ic_heart_filled
                    } else {
                        R.drawable.ic_heart_outline
                    }
                )

                val current = binding.tvLikeCount.text.toString().toInt();

                if (liked) {
                    binding.tvLikeCount.text = (current + 1).toString()
                } else {
                    binding.tvLikeCount.text = (current - 1).toString()
                }

                onLike(post, liked)
            }

            binding.btnComment.setOnClickListener { onComment(post) }
            binding.btnShare.setOnClickListener { onShare(post) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = ItemPostBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)
        return PostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}