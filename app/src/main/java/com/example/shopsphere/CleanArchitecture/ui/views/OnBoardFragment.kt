package com.example.shopsphere.CleanArchitecture.ui.views

import android.animation.Animator
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.bumptech.glide.Glide
import com.example.shopsphere.R
import com.example.shopsphere.databinding.FragmentOnBoardBinding

class OnBoardFragment : Fragment() {

    private var _binding: FragmentOnBoardBinding? = null
    private val binding get() = _binding!!
    private val ambientAnimators = mutableListOf<Animator>()

    private val fashionVisual = OnboardVisual(
        imageUrl = "https://cdn.dummyjson.com/product-images/tops/blue-frock/1.webp",
        title = "Blue Frock",
        price = "EGP 1,199"
    )

    private val beautyVisual = OnboardVisual(
        imageUrl = "https://cdn.dummyjson.com/product-images/beauty/essence-mascara-lash-princess/1.webp",
        title = "Mascara Lash Princess"
    )

    private val techVisual = OnboardVisual(
        imageUrl = "https://cdn.dummyjson.com/product-images/smartphones/iphone-13-pro/1.webp",
        title = "iPhone 13 Pro"
    )

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
        bindVisuals()
        animateIntro()
        binding.root.postDelayed({ if (_binding != null) startAmbientAnimations() }, 420L)
        onClicks()
    }

    private fun bindVisuals() {
        binding.textHeroTitle.text = fashionVisual.title
        binding.textHeroPrice.text = fashionVisual.price
        binding.textBeautyTitle.text = beautyVisual.title
        binding.textTechTitle.text = techVisual.title

        Glide.with(this)
            .load(fashionVisual.imageUrl)
            .centerCrop()
            .into(binding.imageHero)

        Glide.with(this)
            .load(beautyVisual.imageUrl)
            .centerCrop()
            .into(binding.imageBeauty)

        Glide.with(this)
            .load(techVisual.imageUrl)
            .centerCrop()
            .into(binding.imageTech)
    }

    private fun animateIntro() {
        binding.textBadge.apply {
            alpha = 0f
            translationY = -18f
            animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(260L)
                .start()
        }

        binding.heroScene.apply {
            alpha = 0f
            scaleX = 0.9f
            scaleY = 0.9f
            translationY = 34f
            animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .translationY(0f)
                .setDuration(460L)
                .setStartDelay(70L)
                .start()
        }

        binding.textOnboardTitle.apply {
            alpha = 0f
            translationY = 22f
            animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(300L)
                .setStartDelay(150L)
                .start()
        }

        binding.textOnboardSubtitle.apply {
            alpha = 0f
            translationY = 22f
            animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(300L)
                .setStartDelay(210L)
                .start()
        }

        binding.getStartedBtn.apply {
            alpha = 0f
            translationY = 24f
            animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(320L)
                .setStartDelay(270L)
                .start()
        }
    }

    private fun startAmbientAnimations() {
        val density = resources.displayMetrics.density
        listOf(
            binding.cardHero,
            binding.cardBeauty,
            binding.cardTech,
            binding.pillOffer,
            binding.pillTrack
        ).forEach { view ->
            view.cameraDistance = 14000f * density
        }

        ambientAnimators.clear()
        ambientAnimators += floatingAnimator(
            view = binding.cardHero,
            translateXValues = floatArrayOf(0f, 8f, 0f, -8f, 0f),
            translateYValues = floatArrayOf(0f, -16f, -4f, 10f, 0f),
            rotationValues = floatArrayOf(-6f, -3f, -6f, -8f, -6f),
            rotationYValues = floatArrayOf(-8f, -3f, 1f, -4f, -8f),
            rotationXValues = floatArrayOf(2f, 6f, 2f, -2f, 2f),
            duration = 5400L,
            startDelay = 120L
        )

        ambientAnimators += floatingAnimator(
            view = binding.cardBeauty,
            translateXValues = floatArrayOf(0f, -10f, 0f, 8f, 0f),
            translateYValues = floatArrayOf(0f, 8f, 0f, -12f, 0f),
            rotationValues = floatArrayOf(-11f, -8f, -12f, -14f, -11f),
            rotationYValues = floatArrayOf(10f, 16f, 8f, 14f, 10f),
            rotationXValues = floatArrayOf(0f, -4f, 0f, 3f, 0f),
            duration = 4700L,
            startDelay = 250L
        )

        ambientAnimators += floatingAnimator(
            view = binding.cardTech,
            translateXValues = floatArrayOf(0f, 10f, 0f, -8f, 0f),
            translateYValues = floatArrayOf(0f, -10f, 0f, 10f, 0f),
            rotationValues = floatArrayOf(9f, 6f, 9f, 12f, 9f),
            rotationYValues = floatArrayOf(-10f, -16f, -8f, -13f, -10f),
            rotationXValues = floatArrayOf(0f, 4f, 0f, -3f, 0f),
            duration = 5000L,
            startDelay = 180L
        )

        ambientAnimators += pulseAnimator(binding.stageGlowLeft, 0.9f, 1.12f, 3600L)
        ambientAnimators += pulseAnimator(binding.stageGlowRight, 0.88f, 1.14f, 4200L)
        ambientAnimators += pulseAnimator(binding.pillOffer, 1f, 1.06f, 2400L)
        ambientAnimators += pulseAnimator(binding.pillTrack, 1f, 1.04f, 3000L)
        ambientAnimators += breatheButton()

        ambientAnimators.forEach { it.start() }
    }

    private fun floatingAnimator(
        view: View,
        translateXValues: FloatArray,
        translateYValues: FloatArray,
        rotationValues: FloatArray,
        rotationYValues: FloatArray,
        rotationXValues: FloatArray,
        duration: Long,
        startDelay: Long = 180L
    ): Animator {
        return ObjectAnimator.ofPropertyValuesHolder(
            view,
            PropertyValuesHolder.ofFloat(View.TRANSLATION_X, *translateXValues),
            PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, *translateYValues),
            PropertyValuesHolder.ofFloat(View.ROTATION, *rotationValues),
            PropertyValuesHolder.ofFloat(View.ROTATION_Y, *rotationYValues),
            PropertyValuesHolder.ofFloat(View.ROTATION_X, *rotationXValues)
        ).apply {
            this.duration = duration
            this.startDelay = startDelay
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            interpolator = AccelerateDecelerateInterpolator()
        }
    }

    private fun pulseAnimator(
        view: View,
        fromScale: Float,
        toScale: Float,
        duration: Long
    ): Animator {
        return ObjectAnimator.ofPropertyValuesHolder(
            view,
            PropertyValuesHolder.ofFloat(View.SCALE_X, fromScale, toScale, fromScale),
            PropertyValuesHolder.ofFloat(View.SCALE_Y, fromScale, toScale, fromScale),
            PropertyValuesHolder.ofFloat(View.ALPHA, 0.82f, 1f, 0.82f)
        ).apply {
            this.duration = duration
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            interpolator = AccelerateDecelerateInterpolator()
            startDelay = 220L
        }
    }

    private fun breatheButton(): Animator {
        return ObjectAnimator.ofPropertyValuesHolder(
            binding.getStartedBtn,
            PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.025f, 1f),
            PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.025f, 1f)
        ).apply {
            duration = 2300L
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            interpolator = AccelerateDecelerateInterpolator()
            startDelay = 480L
        }
    }

    private fun onClicks() {
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
        ambientAnimators.forEach { it.cancel() }
        ambientAnimators.clear()
        _binding = null
    }

    private data class OnboardVisual(
        val imageUrl: String,
        val title: String,
        val price: String = ""
    )
}
