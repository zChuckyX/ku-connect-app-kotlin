package com.example.ku_connect.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ku_connect.R
import com.example.ku_connect.databinding.DialogEditPostBinding
import com.example.ku_connect.databinding.FragmentMyPostsBinding
import com.example.ku_connect.ui.common.MyPostAdapter
import com.example.ku_connect.util.Extensions.showToast
import com.example.ku_connect.viewmodel.ProfileViewModel
import com.example.ku_connect.viewmodel.UiState

class MyPostsFragment : Fragment() {
    private var _binding: FragmentMyPostsBinding? = null
    private val binding get() = _binding!!
    private val profileViewModel: ProfileViewModel by activityViewModels()
    private lateinit var adapter: MyPostAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMyPostsBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupToolbar()
        observePosts()

        profileViewModel.refreshMyPosts()
    }

    private fun setupRecyclerView() {
        adapter = MyPostAdapter(
            onEdit   = { post -> showEditDialog(post.id, post.title, post.content) },
            onDelete = { post -> showDeleteConfirm(post.id, post.title) },
            onDetail = { post ->
                val bundle = Bundle().apply {
                    putString("postId",    post.id)
                    putString("postTitle", post.title)
                }

                findNavController().navigate(R.id.action_myPosts_to_postDetail, bundle)
            }
        )

        binding.rvMyPosts.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter        = this@MyPostsFragment.adapter

            setHasFixedSize(false)
        }
    }

    private fun setupToolbar() {
        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
    }

    private fun observePosts() {
        profileViewModel.myPosts.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.layoutEmpty.visibility = View.GONE
                    binding.rvMyPosts.visibility   = View.GONE
                }

                is UiState.Success -> {
                    val posts = state.data

                    binding.progressBar.visibility = View.GONE
                    binding.tvPostCount.text = "${posts.size} โพสต์"

                    if (posts.isEmpty()) {
                        binding.layoutEmpty.visibility = View.VISIBLE
                        binding.rvMyPosts.visibility   = View.GONE
                    } else {
                        binding.layoutEmpty.visibility = View.GONE
                        binding.rvMyPosts.visibility   = View.VISIBLE

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
    }

    private fun showEditDialog(postId: String, currentTitle: String, currentContent: String) {
        val dialogBinding = DialogEditPostBinding.inflate(layoutInflater)

        dialogBinding.etTitle.setText(currentTitle)
        dialogBinding.etContent.setText(currentContent)

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("แก้ไขกระทู้")
            .setView(dialogBinding.root)
            .setPositiveButton("บันทึก", null)
            .setNegativeButton("ยกเลิก", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val newTitle   = dialogBinding.etTitle.text.toString().trim()
                val newContent = dialogBinding.etContent.text.toString().trim()

                if (newTitle.isBlank()) {
                    dialogBinding.tilTitle.error = "กรุณากรอกหัวข้อ"
                    return@setOnClickListener
                }

                dialogBinding.tilTitle.error = null

                dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false

                profileViewModel.updatePost(postId, newTitle, newContent) { success, msg ->
                    requireContext().showToast(msg)

                    if (success) {
                        dialog.dismiss()
                    } else {
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
                    }
                }
            }
        }

        dialog.show()
    }

    private fun showDeleteConfirm(postId: String, title: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("ลบกระทู้")
            .setMessage("คุณต้องการลบ \"$title\" ใช่หรือไม่?\nจะไม่สามารถย้อนกลับได้")
            .setPositiveButton("ลบ") { _, _ ->
                profileViewModel.deletePost(postId) { success, msg ->
                    requireContext().showToast(msg)
                }
            }
            .setNegativeButton("ยกเลิก", null)
            .create()
            .apply {
                setOnShowListener {
                    getButton(AlertDialog.BUTTON_POSITIVE)
                        .setTextColor(requireContext().getColor(R.color.ios_red))
                }
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null
    }
}