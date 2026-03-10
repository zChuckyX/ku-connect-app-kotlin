package com.example.ku_connect.ui.market

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
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.ku_connect.data.model.MarketItem
import com.example.ku_connect.databinding.DialogAddShopBinding
import com.example.ku_connect.databinding.FragmentMarketBinding
import com.example.ku_connect.service.CloudinaryService
import com.example.ku_connect.ui.common.MarketAdapter
import com.example.ku_connect.util.Extensions.showToast
import com.example.ku_connect.viewmodel.AuthViewModel
import com.example.ku_connect.viewmodel.MarketViewModel
import com.example.ku_connect.viewmodel.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MarketFragment : Fragment() {
    private var _binding: FragmentMarketBinding? = null
    private val binding get() = _binding!!
    private val marketViewModel: MarketViewModel by activityViewModels()
    private val authViewModel: AuthViewModel by activityViewModels()
    private lateinit var adapter: MarketAdapter
    private var selectedImageUri: Uri? = null
    private var onImagePicked: ((Uri) -> Unit)? = null

    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri

            onImagePicked?.invoke(uri)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMarketBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = MarketAdapter { item ->
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.lineOpenChatUrl))
                startActivity(intent)
            } catch (e: Exception) {
                requireContext().showToast("ไม่สามารถเปิดลิงก์ได้")
            }
        }

        binding.rvMarket.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMarket.adapter = adapter

        binding.btnAddShop.setOnClickListener {
            showAddShopDialog()
        }

        binding.etSearch.addTextChangedListener { text ->
            if (text.isNullOrBlank()) marketViewModel.loadItems()
            else marketViewModel.searchItems(text.toString())
        }

        observeData()
        marketViewModel.loadItems()
        authViewModel.loadCurrentUser()
    }

    private fun showAddShopDialog() {
        val dialogBinding = DialogAddShopBinding.inflate(layoutInflater)

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
            .setTitle("เพิ่มร้านค้า")
            .setView(dialogBinding.root)
            .setPositiveButton("เผยแพร่", null)
            .setNegativeButton("ยกเลิก") { _, _ -> onImagePicked = null }
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val shopName    = dialogBinding.etShopName.text.toString().trim()
                val description = dialogBinding.etDescription.text.toString().trim()
                val lineUrl     = dialogBinding.etLineUrl.text.toString().trim()
                val user        = authViewModel.currentUser.value

                if (shopName.isBlank() || lineUrl.isBlank()) {
                    requireContext().showToast("กรุณากรอกชื่อร้านและลิงก์ Line")
                    return@setOnClickListener
                }

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

                    val item = MarketItem(
                        sellerId     = user?.uid ?: "",
                        sellerName   = user?.username ?: "ผู้ขาย",
                        shopName     = shopName,
                        description  = description,
                        imageUrl     = imageUrl,
                        lineOpenChatUrl = lineUrl
                    )

                    marketViewModel.addItem(item) { _, msg ->
                        requireContext().showToast(msg)
                    }
                }
            }
        }

        dialog.show()
    }

    private fun observeData() {
        marketViewModel.items.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.layoutEmpty.visibility = View.GONE
                }
                is UiState.Success -> {
                    binding.progressBar.visibility = View.GONE

                    val markets = state.data

                    if (markets.isEmpty()) {
                        binding.layoutEmpty.visibility = View.VISIBLE
                    } else {
                        binding.layoutEmpty.visibility = View.GONE

                        adapter.submitList(markets.toList())
                    }
                }
                is UiState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.layoutEmpty.visibility = View.VISIBLE

                    requireContext().showToast(state.message)
                }
            }
        }

        marketViewModel.createState.observe(viewLifecycleOwner) { state ->
            if (state is UiState.Success) {
                marketViewModel.loadItems()

                binding.rvMarket.scrollToPosition(0)
            } else if (state is UiState.Error) {
                requireContext().showToast(state.message)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.rvMarket.scrollToPosition(0)
    }

    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null
    }
}