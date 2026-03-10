package com.example.ku_connect.ui.feed

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.ku_connect.R
import com.example.ku_connect.data.model.Comment
import com.example.ku_connect.databinding.FragmentPostDetailBinding
import com.example.ku_connect.service.CloudinaryService
import com.example.ku_connect.ui.common.CommentAdapter
import com.example.ku_connect.util.Extensions.showToast
import com.example.ku_connect.util.Extensions.toAvatarColors
import com.example.ku_connect.util.Extensions.toInitial
import com.example.ku_connect.util.Extensions.toRelativeTime
import com.example.ku_connect.viewmodel.AuthViewModel
import com.example.ku_connect.viewmodel.FeedViewModel
import com.example.ku_connect.viewmodel.UiState
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PostDetailFragment : Fragment() {
    private var _binding: FragmentPostDetailBinding? = null
    private val binding get() = _binding!!
    private val feedViewModel: FeedViewModel by activityViewModels()
    private val authViewModel: AuthViewModel by activityViewModels()
    private lateinit var commentAdapter: CommentAdapter
    private var pendingImageUri: Uri?   = null
    private val Int.dp: Int get() = (this * resources.displayMetrics.density).toInt()
    private val postId by lazy { arguments?.getString("postId")    ?: "" }

    private val imagePicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            pendingImageUri = uri
            addPreviewImage(uri)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPostDetailBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupCommentList()
        setupInputBar()
        observePost()
        observeComments()

        feedViewModel.loadAllPosts()
        feedViewModel.loadComments(postId)
        authViewModel.loadCurrentUser()
    }

    private fun setupCommentList() {
        commentAdapter = CommentAdapter()

        binding.rvComments.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter        = commentAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupInputBar() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnAttachImage.setOnClickListener {
            imagePicker.launch("image/*")
        }

        binding.btnSend.setOnClickListener {
            submitComment()
        }
    }

    private fun observePost() {
        feedViewModel.allPosts.observe(viewLifecycleOwner) { state ->
            if (state !is UiState.Success) {
                return@observe
            }

            val post = state.data.find { it.id == postId } ?: return@observe
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
            val (bgColor, textColor) = post.authorName.toAvatarColors(post.authorColor)
            val oval = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(bgColor)
            }

            binding.tvAvatar.background = oval
            binding.tvAvatar.text       = post.authorName.toInitial()
            binding.tvAvatar.setTextColor(textColor)

            binding.tvAuthor.text = post.authorName
            binding.tvTime.text   = post.createdAt?.toRelativeTime() ?: ""
            binding.tvTitle.text  = post.title
            binding.tvContent.text = post.content

            if (!post.imageUrl.isNullOrBlank()) {
                binding.ivPostImage.visibility = View.VISIBLE

                Glide.with(this)
                    .load(post.imageUrl)
                    .centerCrop()
                    .placeholder(R.color.ios_fill)
                    .into(binding.ivPostImage)
            } else {
                binding.ivPostImage.visibility = View.GONE
            }

            binding.tvLikeCount.text    = "${post.likeCount} ถูกใจ"
            binding.tvCommentCount.text = "${post.commentCount} ความคิดเห็น"

            val liked = post.likedBy.contains(uid)

            binding.ivLike.setImageResource(
                if (liked) {
                    R.drawable.ic_heart_filled
                } else {
                    R.drawable.ic_heart_outline
                }
            )

            binding.btnLike.setOnClickListener {
                feedViewModel.toggleLike(post.id, uid, !liked)
                feedViewModel.loadAllPosts()
            }

            binding.btnShare.setOnClickListener {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, "${post.title}\n\n${post.content}")
                }

                startActivity(Intent.createChooser(intent, "แชร์กระทู้"))
            }
        }
    }

    private fun observeComments() {
        feedViewModel.comments.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                }
                is UiState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    commentAdapter.submitList(state.data)

                    if (state.data.isEmpty()) {
                        binding.tvEmptyComments.visibility = View.VISIBLE
                        binding.rvComments.visibility      = View.GONE
                    } else {
                        binding.tvEmptyComments.visibility = View.GONE
                        binding.rvComments.visibility      = View.VISIBLE
                    }

                    val count = state.data.size
                    binding.tvCommentHeader.text = if (count > 0) "ความคิดเห็น ($count)" else "ความคิดเห็น"

                    if (count > 0) {
                        binding.nestedScroll.post {
                            binding.nestedScroll
                                .smoothScrollTo(0, binding.nestedScroll.getChildAt(0).height)
                        }
                    }
                }
                is UiState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    requireContext().showToast(state.message)
                }
            }
        }
    }

    private fun submitComment() {
        val text    = binding.etComment.text?.toString()?.trim() ?: ""
        val imgUri  = pendingImageUri
        val user    = authViewModel.currentUser.value

        if (text.isBlank() && imgUri == null) {
            requireContext().showToast("กรุณาพิมพ์ข้อความหรืออัพโหลดรูปภาพ")
            return
        }

        binding.btnSend.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.Main).launch {
            var uploadedImageUrl: String? = null

            try {
                if (imgUri != null) {
                    uploadedImageUrl = withContext(Dispatchers.IO) {
                        CloudinaryService.uploadFile(requireContext(), imgUri, "comments/images")
                    }
                }
            } catch (e: Exception) {
                requireContext().showToast("อัปโหลดรูปภาพไม่สำเร็จ: ${e.message}")

                binding.btnSend.isEnabled = true
                binding.progressBar.visibility = View.GONE

                return@launch
            }

            val comment = Comment(
                postId      = postId,
                authorId    = user?.uid ?: "",
                authorName  = user?.username ?: "ผู้ใช้",
                authorColor = user?.profileColor ?: "#C8E6C9",
                content     = text,
                imageUrl    = uploadedImageUrl
            )

            feedViewModel.addComment(postId, comment)

            binding.etComment.text?.clear()
            clearPendingAttachments()
            binding.btnSend.isEnabled = true
            binding.progressBar.visibility = View.GONE
        }
    }

    private fun addPreviewImage(uri: Uri) {
        binding.previewStrip.visibility = View.VISIBLE

        val iv = ImageView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(64.dp, 64.dp).also {
                it.marginEnd = 8.dp
            }
            scaleType = ImageView.ScaleType.CENTER_CROP
            background = GradientDrawable().apply {
                cornerRadius = 8f
                setColor(Color.parseColor("#E5E5EA"))
            }
            clipToOutline = true
        }
        Glide.with(this).load(uri).centerCrop().into(iv)

        iv.setOnClickListener {
            binding.previewContainer.removeView(iv)
            pendingImageUri = null
            if (binding.previewContainer.childCount == 0) {
                binding.previewStrip.visibility = View.GONE
            }
        }
        binding.previewContainer.addView(iv)
    }

    private fun clearPendingAttachments() {
        pendingImageUri = null
        binding.previewContainer.removeAllViews()
        binding.previewStrip.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null
    }
}