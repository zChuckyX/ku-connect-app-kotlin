package com.example.ku_connect.ui.feed

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.ku_connect.R
import com.example.ku_connect.data.model.Post
import com.example.ku_connect.databinding.DialogCreatePostBinding
import com.example.ku_connect.databinding.FragmentPostsBinding
import com.example.ku_connect.service.CloudinaryService
import com.example.ku_connect.ui.common.PostAdapter
import com.example.ku_connect.util.Extensions.showToast
import com.example.ku_connect.viewmodel.AuthViewModel
import com.example.ku_connect.viewmodel.FeedViewModel
import com.example.ku_connect.viewmodel.UiState
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PostsFragment : Fragment() {
    private var _binding: FragmentPostsBinding? = null
    private val binding get() = _binding!!
    private val feedViewModel: FeedViewModel by activityViewModels()
    private val authViewModel: AuthViewModel by activityViewModels()
    private lateinit var adapter: PostAdapter
    private var selectedImageUri: Uri? = null
    private var onImagePicked: ((Uri) -> Unit)? = null

    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                selectedImageUri = it

                onImagePicked?.invoke(it)
            }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPostsBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        adapter = PostAdapter(
            onClickDetail = { post ->
                val bundle = Bundle().apply {
                    putString("postId", post.id)
                    putString("postTitle", post.title)
                }

                findNavController().navigate(R.id.action_posts_to_postDetail, bundle)
            },
            currentUserId = uid,
            onLike = { post, liked -> feedViewModel.toggleLike(post.id, uid, liked) },
            onComment = { post ->
                val bundle = Bundle().apply {
                    putString("postId", post.id)
                    putString("postTitle", post.title)
                }
                findNavController().navigate(R.id.action_posts_to_postDetail, bundle)
            },
            onShare = { post ->
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, "${post.title}\n${post.content}")
                }
                startActivity(Intent.createChooser(shareIntent, "แชร์กระทู้"))
            }
        )

        binding.rvPosts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPosts.adapter = adapter
        binding.btnAddPost.setOnClickListener {
            showCreatePostDialog()
        }
        binding.etSearch.addTextChangedListener { text ->
            if (text.isNullOrBlank()) {
                feedViewModel.loadAllPosts()
            } else {
                feedViewModel.searchPosts(text.toString())
            }
        }

        observeData()
        feedViewModel.loadAllPosts()
        authViewModel.loadCurrentUser()
    }

    private fun showCreatePostDialog() {
        val dialogBinding = DialogCreatePostBinding.inflate(layoutInflater)

        selectedImageUri = null

        fun showPreview(uri: Uri) {
            val ctx = requireContext()

            dialogBinding.cardImagePreview.visibility = View.VISIBLE

            Glide.with(ctx)
                .load(uri)
                .centerCrop()
                .into(dialogBinding.ivPreview)

            val name = uri.lastPathSegment?.substringAfterLast('/')
                ?: ctx.contentResolver.getType(uri)
                ?: "image"

            dialogBinding.tvImageName.text = name
            dialogBinding.btnPickImage.text = "เปลี่ยนรูปภาพ"
        }

        fun clearPreview() {
            selectedImageUri = null

            dialogBinding.cardImagePreview.visibility = View.GONE
            dialogBinding.btnPickImage.text = "เพิ่มรูปภาพ (ไม่บังคับ)"
        }

        onImagePicked = { uri ->
            showPreview(uri)
        }

        dialogBinding.btnPickImage.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

        dialogBinding.btnRemoveImage.setOnClickListener {
            clearPreview()
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("สร้างกระทู้ใหม่")
            .setView(dialogBinding.root)
            .setPositiveButton("เผยแพร่", null)
            .setNegativeButton("ยกเลิก") { _, _ -> onImagePicked = null }
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val title   = dialogBinding.etTitle.text.toString().trim()
                val content = dialogBinding.etContent.text.toString().trim()
                val user    = authViewModel.currentUser.value

                if (title.isBlank()) {
                    dialogBinding.tilTitle.error = "กรุณากรอกหัวข้อ"
                    return@setOnClickListener
                }

                dialogBinding.tilTitle.error = null

                val capturedUri = selectedImageUri
                dialog.dismiss()
                onImagePicked = null

                viewLifecycleOwner.lifecycleScope.launch {
                    var imageUrl: String? = null

                    if (capturedUri != null) {
                        try {
                            imageUrl = withContext(Dispatchers.IO) {
                                CloudinaryService.uploadFile(
                                    requireContext(), capturedUri, "posts"
                                )
                            }
                        } catch (e: Exception) {
                            requireContext().showToast("อัปโหลดรูปภาพไม่สำเร็จ: ${e.message}")
                        }
                    }

                    val post = Post(
                        authorId    = user?.uid ?: "",
                        authorName  = user?.username ?: "ผู้ใช้",
                        authorColor = user?.profileColor ?: "#C8E6C9",
                        title       = title,
                        content     = content,
                        imageUrl    = imageUrl
                    )

                    feedViewModel.createPost(post) { _, msg ->
                        requireContext().showToast(msg)
                    }
                }
            }
        }

        dialog.show()
    }

    private fun observeData() {
        feedViewModel.allPosts.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.layoutEmpty.visibility = View.GONE
                }
                is UiState.Success -> {
                    binding.progressBar.visibility = View.GONE

                    val posts = state.data

                    if (posts.isEmpty()) {
                        binding.layoutEmpty.visibility = View.VISIBLE
                    } else {
                        binding.layoutEmpty.visibility = View.GONE

                        adapter.submitList(posts.toList())
                    }
                }
                is UiState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.layoutEmpty.visibility = View.VISIBLE

                    requireContext().showToast(state.message)
                }
            }
        }

        feedViewModel.createState.observe(viewLifecycleOwner) { state ->
            if (state is UiState.Success) {
                feedViewModel.loadAllPosts()

                binding.rvPosts.scrollToPosition(0)
            } else if (state is UiState.Error) {
                requireContext().showToast(state.message)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.rvPosts.scrollToPosition(0)
    }

    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null
    }
}