import SwiftUI

struct FAQsView: View {
    @State private var expandedIndex: Int?
    @Environment(NavigationRouter.self) private var router

    var body: some View {
        VStack(spacing: 0) {
            // Header
            HStack {
                CircleBackButton { router.pop() }
                Spacer()
                Text("FAQs")
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundColor(AppTheme.textPrimary)
                Spacer()
                Color.clear.frame(width: 40, height: 40)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 8)

            ScrollView {
                VStack(spacing: 8) {
                    ForEach(Array(AppConstants.faqs.enumerated()), id: \.offset) { index, faq in
                        VStack(alignment: .leading, spacing: 0) {
                            // Question
                            Button {
                                withAnimation(.easeInOut(duration: 0.2)) {
                                    expandedIndex = expandedIndex == index ? nil : index
                                }
                            } label: {
                                HStack {
                                    Text(faq.question)
                                        .font(.system(size: 15, weight: .medium))
                                        .foregroundColor(AppTheme.textPrimary)
                                        .multilineTextAlignment(.leading)
                                    Spacer()
                                    Image(systemName: expandedIndex == index ? "chevron.up" : "chevron.down")
                                        .font(.system(size: 12, weight: .semibold))
                                        .foregroundColor(AppTheme.textSecondary)
                                }
                                .padding(16)
                            }

                            // Answer
                            if expandedIndex == index {
                                Text(faq.answer)
                                    .font(.system(size: 14))
                                    .foregroundColor(AppTheme.textSecondary)
                                    .lineSpacing(4)
                                    .padding(.horizontal, 16)
                                    .padding(.bottom, 16)
                                    .transition(.opacity)
                            }
                        }
                        .background(Color.white)
                        .cornerRadius(AppTheme.cornerRadius)
                        .shadow(color: .black.opacity(0.04), radius: 2, x: 0, y: 1)
                    }
                }
                .padding(16)
            }
        }
        .background(AppTheme.surface)
        .navigationBarHidden(true)
    }
}
