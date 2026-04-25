package com.example.shopsphere.CleanArchitecture.ui.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.shopsphere.CleanArchitecture.data.local.notifications.NotificationEntity
import com.example.shopsphere.CleanArchitecture.data.local.notifications.NotificationsRepository
import com.example.shopsphere.CleanArchitecture.ui.adapters.NotificationsAdapter
import com.example.shopsphere.R
import com.example.shopsphere.databinding.FragmentNotificationsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class NotificationsFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var notificationsRepository: NotificationsRepository

    private val adapter by lazy {
        NotificationsAdapter(onClick = ::onNotificationClick)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        binding.btnBell.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                notificationsRepository.markAllRead()
            }
        }
        binding.recyclerNotifications.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerNotifications.adapter = adapter

        observeNotifications()
    }

    private fun observeNotifications() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                notificationsRepository.observe().collect { list ->
                    adapter.submitList(list)
                    val empty = list.isEmpty()
                    binding.emptyStateContainer.visibility =
                        if (empty) View.VISIBLE else View.GONE
                    binding.recyclerNotifications.visibility =
                        if (empty) View.GONE else View.VISIBLE
                }
            }
        }
    }

    private fun onNotificationClick(item: NotificationEntity) {
        viewLifecycleOwner.lifecycleScope.launch {
            notificationsRepository.markRead(item.id)
        }
        val link = item.deepLink.orEmpty()
        when {
            link.startsWith("track_order:") -> {
                val orderId = link.substringAfter(":")
                val bundle = Bundle().apply { putString("orderId", orderId) }
                runCatching {
                    findNavController().navigate(R.id.trackOrderFragment, bundle)
                }
            }
            link == "chatbot" -> {
                runCatching {
                    findNavController().navigate(R.id.chatBotFragment)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
