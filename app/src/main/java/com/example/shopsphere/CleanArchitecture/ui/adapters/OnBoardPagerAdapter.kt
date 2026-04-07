package com.example.shopsphere.CleanArchitecture.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.recyclerview.widget.RecyclerView
import com.example.shopsphere.databinding.ItemOnboardPageBinding

class OnBoardPagerAdapter(
    private val pages: List<OnBoardPageUiModel>
) : RecyclerView.Adapter<OnBoardPagerAdapter.OnBoardPageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OnBoardPageViewHolder {
        val binding = ItemOnboardPageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return OnBoardPageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OnBoardPageViewHolder, position: Int) {
        holder.bind(pages[position])
    }

    override fun getItemCount(): Int = pages.size

    inner class OnBoardPageViewHolder(
        private val binding: ItemOnboardPageBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: OnBoardPageUiModel) = with(binding) {
            textPageTitle.setText(item.titleRes)
            textPageSubtitle.setText(item.subtitleRes)
            imageHero.setImageResource(item.heroImageRes)

            imageHero.alpha = 0f
            imageHero.scaleX = 0.94f
            imageHero.scaleY = 0.94f
            illustrationArea.translationY = 0f
            illustrationArea.alpha = 1f
            illustrationArea.animate()
                .translationY(-8f)
                .alpha(1f)
                .setDuration(420L)
                .start()
            imageHero.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(520L)
                .start()
        }
    }

    data class OnBoardPageUiModel(
        @DrawableRes val heroImageRes: Int,
        @StringRes val titleRes: Int,
        @StringRes val subtitleRes: Int
    )
}
