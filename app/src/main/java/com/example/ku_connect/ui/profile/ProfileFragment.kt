package com.example.ku_connect.ui.profile

import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.ku_connect.R
import com.example.ku_connect.databinding.FragmentProfileBinding
import com.example.ku_connect.ui.auth.LoginFragment
import com.example.ku_connect.util.Extensions.showToast
import com.example.ku_connect.util.Extensions.toAvatarColors
import com.example.ku_connect.util.Extensions.toInitial
import com.example.ku_connect.viewmodel.AuthViewModel
import com.example.ku_connect.viewmodel.ProfileViewModel

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val profileViewModel: ProfileViewModel by activityViewModels()
    private val authViewModel: AuthViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnUpdateUsername.setOnClickListener {
            val newName = binding.etUsername.text.toString()

            profileViewModel.updateUsername(newName) { success, msg ->
                requireContext().showToast(msg)

                if (success) {
                    binding.tilUsername.error = null
                } else {
                    binding.tilUsername.error = msg
                }
            }
        }

        binding.btnSeeAllPosts.setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_myPosts)
        }

        binding.lvPostCount.setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_myPosts)
        }

        binding.btnLogout.setOnClickListener {
            authViewModel.logout()

            startActivity(Intent(requireContext(), LoginFragment::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
        }

        observeProfile()
        profileViewModel.loadProfile()
    }

    private fun observeProfile() {
        profileViewModel.user.observe(viewLifecycleOwner) { user ->
            if (user != null) {
                val (bgColor, textColor) = user.username.toAvatarColors(user.profileColor)
                val d = GradientDrawable()

                d.shape = GradientDrawable.OVAL
                d.setColor(bgColor)

                binding.tvAvatarLarge.background = d
                binding.tvAvatarLarge.text = user.username.toInitial()
                binding.tvAvatarLarge.setTextColor(textColor)

                binding.tvUsername.text = user.username
                binding.tvEmail.text = user.email
                binding.etUsername.setText(user.username)
                binding.btnUpdateUsername.isEnabled = false

                binding.etUsername.addTextChangedListener {
                    val newUsername = it.toString().trim()

                    binding.btnUpdateUsername.isEnabled = newUsername.isNotEmpty() && newUsername != user.username
                }
            }
        }

        profileViewModel.postCount.observe(viewLifecycleOwner) { value ->
            binding.tvPostCount.text = value.toString()
        }

        profileViewModel.marketCount.observe(viewLifecycleOwner) { value ->
            binding.tvItemCount.text = value.toString()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null
    }
}