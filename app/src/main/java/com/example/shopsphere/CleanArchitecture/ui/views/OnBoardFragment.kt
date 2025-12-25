package com.example.shopsphere.CleanArchitecture.ui.views

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.example.shopsphere.R
import com.example.shopsphere.databinding.FragmentOnBoardBinding


class OnBoardFragment : Fragment() {

    private var _binding: FragmentOnBoardBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnBoardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onClicks()
    }

    private fun onClicks(){
        binding.getStartedBtn.setOnClickListener {
            findNavController().navigate(
                R.id.action_onBoardFragment_to_loginFragment,
                null,
                navOptions {
                    popUpTo(R.id.onBoardFragment) {
                        inclusive = true
                    }
                    launchSingleTop = true
                }
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
