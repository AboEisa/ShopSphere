import SwiftUI

struct ReviewRow: View {
    let review: ReviewData

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                // Avatar
                Circle()
                    .fill(AppTheme.primaryGreen.opacity(0.2))
                    .frame(width: 40, height: 40)
                    .overlay(
                        Text(String(review.author.prefix(1)))
                            .font(.system(size: 16, weight: .bold))
                            .foregroundColor(AppTheme.primaryGreen)
                    )

                VStack(alignment: .leading, spacing: 2) {
                    Text(review.author)
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundColor(AppTheme.textPrimary)

                    // Star Rating
                    HStack(spacing: 2) {
                        ForEach(1...5, id: \.self) { star in
                            Image(systemName: starImage(for: star))
                                .font(.system(size: 12))
                                .foregroundColor(AppTheme.reviewStarActive)
                        }
                    }
                }

                Spacer()

                Text(review.date)
                    .font(.system(size: 12))
                    .foregroundColor(AppTheme.textSecondary)
            }

            Text(review.comment)
                .font(.system(size: 14))
                .foregroundColor(AppTheme.textSecondary)
                .lineSpacing(4)
        }
        .padding(12)
        .background(Color.white)
        .cornerRadius(AppTheme.cornerRadius)
    }

    private func starImage(for position: Int) -> String {
        let rating = review.rating
        if Double(position) <= rating {
            return "star.fill"
        } else if Double(position) - 0.5 <= rating {
            return "star.leadinghalf.filled"
        } else {
            return "star"
        }
    }
}
