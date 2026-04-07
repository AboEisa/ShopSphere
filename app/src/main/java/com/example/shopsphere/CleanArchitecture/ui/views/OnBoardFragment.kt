package com.example.shopsphere.CleanArchitecture.ui.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.shopsphere.R
import com.example.shopsphere.CleanArchitecture.ui.adapters.OnBoardPagerAdapter
import com.example.shopsphere.databinding.FragmentOnBoardBinding

class OnBoardFragment : Fragment() {

    private var _binding: FragmentOnBoardBinding? = null
    private val binding get() = _binding!!

    private val pages by lazy {
        listOf(
            OnBoardPagerAdapter.OnBoardPageUiModel(
                heroImageRes = R.drawable.onboard_discover,
                titleRes = R.string.onboard_page_one_title,
                subtitleRes = R.string.onboard_page_one_subtitle
            ),
            OnBoardPagerAdapter.OnBoardPageUiModel(
                heroImageRes = R.drawable.onboard_payment,
                titleRes = R.string.onboard_page_two_title,
                subtitleRes = R.string.onboard_page_two_subtitle
            ),
            OnBoardPagerAdapter.OnBoardPageUiModel(
                heroImageRes = R.drawable.onboard_delivery,
                titleRes = R.string.onboard_page_three_title,
                subtitleRes = R.string.onboard_page_three_subtitle
            )
        )
    }

    private val pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            updateIndicators(position)
            binding.buttonContinue.text = if (position == pages.lastIndex) {
                getString(R.string.onboard_get_started)
            } else {
                getString(R.string.onboard_next)
            }
        }
    }

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
        setupPager()
        setupClicks()
        updateIndicators(0)
    }

    private fun setupPager() {
        binding.viewPagerOnboard.adapter = OnBoardPagerAdapter(pages)
        binding.viewPagerOnboard.offscreenPageLimit = 1
        binding.viewPagerOnboard.registerOnPageChangeCallback(pageChangeCallback)
        (binding.viewPagerOnboard[0] as RecyclerView).overScrollMode = RecyclerView.OVER_SCROLL_NEVER

        binding.viewPagerOnboard.setPageTransformer { page, position ->
            when {
                position < -1f || position > 1f -> {
                    page.alpha = 0f
                }

                position <= 0f -> {
                    page.alpha = 1f
                    page.translationX = 0f
                    page.translationY = 0f
                    page.scaleX = 1f
                    page.scaleY = 1f
                }

                else -> {
                    val scaleFactor = 0.92f + ((1f - position) * 0.08f)
                    page.alpha = 1f - position
                    page.translationX = -page.width * position
                    page.translationY = 20f * position
                    page.scaleX = scaleFactor
                    page.scaleY = scaleFactor
                }
            }
        }
    }

    private fun setupClicks() {
        binding.textSkip.setOnClickListener { navigateToLogin() }
        binding.buttonContinue.setOnClickListener {
            val current = binding.viewPagerOnboard.currentItem
            if (current == pages.lastIndex) {
                navigateToLogin()
            } else {
                binding.viewPagerOnboard.currentItem = current + 1
            }
        }
    }

    private fun updateIndicators(selectedPosition: Int) {
        val indicators = listOf(
            binding.indicatorOne,
            binding.indicatorTwo,
            binding.indicatorThree
        )

        indicators.forEachIndexed { index, view ->
            val isSelected = index == selectedPosition
            view.background = ContextCompat.getDrawable(
                requireContext(),
                if (isSelected) R.drawable.bg_onboard_dot_active else R.drawable.bg_onboard_dot_inactive
            )
            val layoutParams = view.layoutParams as LinearLayout.LayoutParams
            val density = resources.displayMetrics.density
            layoutParams.width = if (isSelected) (18 * density).toInt() else (6 * density).toInt()
            layoutParams.height = (6 * density).toInt()
            view.layoutParams = layoutParams
        }
    }

    private fun navigateToLogin() {
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

    override fun onDestroyView() {
        binding.viewPagerOnboard.unregisterOnPageChangeCallback(pageChangeCallback)
        _binding = null
        super.onDestroyView()
    }
}
