package com.example.ku_connect.ui.feed

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ku_connect.R
import com.example.ku_connect.databinding.FragmentFeedBinding
import com.example.ku_connect.ui.common.PostAdapter
import com.example.ku_connect.util.Extensions.showToast
import com.example.ku_connect.viewmodel.FeedViewModel
import com.example.ku_connect.viewmodel.UiState
import com.google.firebase.auth.FirebaseAuth

class FeedFragment : Fragment() {
    private var _binding: FragmentFeedBinding? = null
    private val binding get() = _binding!!
    private val feedViewModel: FeedViewModel by activityViewModels()
    private lateinit var adapter: PostAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentFeedBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        adapter = PostAdapter(
            currentUserId = uid,
            onLike = { post, liked -> feedViewModel.toggleLike(post.id, uid, liked) },
            onComment = { post ->
                val bundle = Bundle().apply {
                    putString("postId", post.id)
                    putString("postTitle", post.title)
                }
                findNavController().navigate(R.id.action_feed_to_postDetail, bundle)
            },
            onClickDetail = { post ->
                val bundle = Bundle().apply {
                    putString("postId", post.id)
                    putString("postTitle", post.title)
                }
                findNavController().navigate(R.id.action_feed_to_postDetail, bundle)
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

        binding.etSearch.addTextChangedListener { text ->
            if (text.isNullOrBlank()) {
                feedViewModel.loadPopularPosts()
            } else {
                feedViewModel.searchPosts(text.toString())
            }
        }

        observeData()
        feedViewModel.loadPopularPosts()
    }

    private fun observeData() {
        feedViewModel.popularPosts.observe(viewLifecycleOwner) { state ->
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

        feedViewModel.allPosts.observe(viewLifecycleOwner) { state ->
            if (state is UiState.Success) {
                adapter.submitList(state.data)
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