package com.example.shopsphere.CleanArchitecture.ui.views

import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.shopsphere.CleanArchitecture.ui.adapters.ReviewUiItem
import com.example.shopsphere.CleanArchitecture.ui.adapters.ReviewsAdapter
import com.example.shopsphere.R
import com.example.shopsphere.databinding.FragmentReviewsBinding
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.math.roundToInt

@AndroidEntryPoint
class ReviewsFragment : Fragment() {

    private var _binding: FragmentReviewsBinding? = null
    private val binding get() = _binding!!

    private val args: ReviewsFragmentArgs by navArgs()
    private val reviewsAdapter by lazy { ReviewsAdapter() }

    @Inject
    lateinit var firestore: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReviewsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        binding.recyclerReviews.adapter = reviewsAdapter
        loadReviews()
    }

    private fun loadReviews() {
        showLoading(true)

        val productDocRef = firestore.collection(PRODUCTS_COLLECTION)
            .document(args.productId.toString())

        productDocRef.get()
            .addOnSuccessListener { productSnapshot ->
                val fallbackRate = productSnapshot.getDouble("ratingRate")
                    ?: productSnapshot.getDouble("rating")
                    ?: 0.0

                val fallbackCount = productSnapshot.getLong("reviewsCount")?.toInt()
                    ?: productSnapshot.getLong("ratingCount")?.toInt()
                    ?: 0

                loadReviewsFromSources(fallbackRate, fallbackCount)
            }
            .addOnFailureListener {
                loadReviewsFromSources(0.0, 0)
            }
    }

    private fun loadReviewsFromSources(
        fallbackRate: Double,
        fallbackCount: Int
    ) {
        val subCollection = firestore.collection(PRODUCTS_COLLECTION)
            .document(args.productId.toString())
            .collection(REVIEWS_SUB_COLLECTION)

        subCollection.get()
            .addOnSuccessListener { snapshot ->
                val subCollectionReviews = snapshot.documents
                    .mapNotNull { it.toReviewUiItem() }
                    .sortedByDescending { it.timestampMillis }

                if (subCollectionReviews.isNotEmpty()) {
                    renderReviews(subCollectionReviews.map { it.asUiItem() }, fallbackRate, fallbackCount)
                    return@addOnSuccessListener
                }

                firestore.collection(REVIEWS_COLLECTION)
                    .whereEqualTo("productId", args.productId)
                    .get()
                    .addOnSuccessListener { topLevelSnapshot ->
                        val integerMatched = topLevelSnapshot.documents
                            .mapNotNull { it.toReviewUiItem() }
                            .sortedByDescending { it.timestampMillis }

                        if (integerMatched.isNotEmpty()) {
                            renderReviews(
                                integerMatched.map { it.asUiItem() },
                                fallbackRate,
                                fallbackCount
                            )
                            return@addOnSuccessListener
                        }

                        firestore.collection(REVIEWS_COLLECTION)
                            .whereEqualTo("productId", args.productId.toString())
                            .get()
                            .addOnSuccessListener { stringSnapshot ->
                                val stringMatched = stringSnapshot.documents
                                    .mapNotNull { it.toReviewUiItem() }
                                    .sortedByDescending { it.timestampMillis }

                                renderReviews(
                                    stringMatched.map { it.asUiItem() },
                                    fallbackRate,
                                    fallbackCount
                                )
                            }
                            .addOnFailureListener {
                                renderReviews(emptyList(), fallbackRate, fallbackCount)
                            }
                    }
                    .addOnFailureListener {
                        renderReviews(emptyList(), fallbackRate, fallbackCount)
                    }
            }
            .addOnFailureListener {
                renderReviews(emptyList(), fallbackRate, fallbackCount)
            }
    }

    private fun renderReviews(
        reviews: List<ReviewUiItem>,
        fallbackRate: Double,
        fallbackCount: Int
    ) {
        showLoading(false)
        reviewsAdapter.submitList(reviews)

        val average = when {
            reviews.isNotEmpty() -> reviews.map { it.rating }.average()
            fallbackRate > 0.0 -> fallbackRate
            else -> 0.0
        }

        val displayCount = maxOf(reviews.size, fallbackCount)
        val roundedAverage = (average * 10.0).roundToInt() / 10.0

        binding.textAverageRating.text = String.format("%.1f", roundedAverage)
        binding.textRatingCount.text = getString(R.string.reviews_rating_count, displayCount)
        binding.textReviewsCount.text = getString(R.string.reviews_count_value, reviews.size)
        binding.textNoReviews.visibility = if (reviews.isEmpty()) View.VISIBLE else View.GONE

        // Rating distribution: count per star level (5..1)
        val distribution = IntArray(6) { 0 }
        reviews.forEach { distribution[it.rating]++ }
        val maxCount = distribution.maxOrNull()?.coerceAtLeast(1) ?: 1
        val bars = listOf(
            binding.bar5,
            binding.bar4,
            binding.bar3,
            binding.bar2,
            binding.bar1
        )
        bars.forEachIndexed { index, bar ->
            val starLevel = 5 - index
            val count = distribution[starLevel]
            bar.progress = (count * 100 / maxCount).coerceIn(0, 100)
        }
    }

    private fun showLoading(isLoading: Boolean) {
        if (_binding == null) return
        binding.reviewsLoading.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.textNoReviews.visibility = View.GONE
    }

    private fun DocumentSnapshot.toReviewUiItem(): FirestoreReview? {
        val comment = getString("comment")
            ?: getString("review")
            ?: getString("message")
            ?: return null

        val reviewerName = getString("reviewerName")
            ?: getString("userName")
            ?: getString("author")
            ?: getString("name")
            ?: getString("userDisplayName")
            ?: getString("userEmail")
            ?: getString("uid")
            ?: "Anonymous"

        val rating = getLong("rating")?.toInt()
            ?: getDouble("rating")?.roundToInt()
            ?: getLong("rate")?.toInt()
            ?: getDouble("rate")?.roundToInt()
            ?: 0

        val createdMillis = when (val createdAt = get("createdAt")) {
            is Timestamp -> createdAt.toDate().time
            is Long -> createdAt
            is Number -> createdAt.toLong()
            else -> System.currentTimeMillis()
        }

        return FirestoreReview(
            reviewerName = reviewerName,
            comment = comment,
            rating = rating.coerceIn(1, 5),
            timestampMillis = createdMillis
        )
    }

    private fun FirestoreReview.asUiItem(): ReviewUiItem {
        val relativeTime = DateUtils.getRelativeTimeSpanString(
            timestampMillis,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS
        ).toString()

        return ReviewUiItem(
            reviewerName = reviewerName,
            daysAgo = if (relativeTime.isBlank()) getString(R.string.reviews_recently) else relativeTime,
            comment = comment,
            rating = rating
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private data class FirestoreReview(
        val reviewerName: String,
        val comment: String,
        val rating: Int,
        val timestampMillis: Long
    )

    companion object {
        private const val PRODUCTS_COLLECTION = "products"
        private const val REVIEWS_COLLECTION = "reviews"
        private const val REVIEWS_SUB_COLLECTION = "reviews"
    }
}
