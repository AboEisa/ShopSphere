import SwiftUI

struct DraggableCartFAB: View {
    let onTap: () -> Void
    @Environment(\.container) private var container
    @State private var position = CGPoint(x: UIScreen.main.bounds.width - 50, y: UIScreen.main.bounds.height - 200)
    @State private var dragOffset = CGSize.zero
    @State private var isDragging = false
    @State private var cartCount = 0

    var body: some View {
        GeometryReader { geometry in
            ZStack {
                // FAB Button
                Circle()
                    .fill(AppTheme.primaryGreen)
                    .frame(width: 56, height: 56)
                    .shadow(color: AppTheme.primaryGreen.opacity(0.4), radius: 8, x: 0, y: 4)
                    .overlay(
                        Image(systemName: "cart.fill")
                            .font(.system(size: 22))
                            .foregroundColor(.white)
                    )
                    .scaleEffect(isDragging ? 1.1 : 1.0)

                // Badge
                if cartCount > 0 {
                    Text(cartCount > AppConstants.maxBadgeCount ? "99+" : "\(cartCount)")
                        .font(.system(size: 10, weight: .bold))
                        .foregroundColor(.white)
                        .padding(.horizontal, 5)
                        .padding(.vertical, 2)
                        .background(AppTheme.errorRed)
                        .clipShape(Capsule())
                        .offset(x: 18, y: -18)
                }
            }
            .position(
                x: min(max(position.x + dragOffset.width, 28), geometry.size.width - 28),
                y: min(max(position.y + dragOffset.height, 28), geometry.size.height - 28)
            )
            .gesture(
                DragGesture()
                    .onChanged { value in
                        isDragging = true
                        dragOffset = value.translation
                    }
                    .onEnded { value in
                        isDragging = false
                        let newX = position.x + value.translation.width
                        let newY = position.y + value.translation.height

                        // Snap to nearest edge
                        let snapX = newX < geometry.size.width / 2 ? CGFloat(40) : geometry.size.width - 40

                        withAnimation(.spring(response: 0.3, dampingFraction: 0.7)) {
                            position = CGPoint(
                                x: snapX,
                                y: min(max(newY, 80), geometry.size.height - 80)
                            )
                        }
                        dragOffset = .zero

                        // If barely moved, treat as tap
                        let totalDrag = abs(value.translation.width) + abs(value.translation.height)
                        if totalDrag < 10 {
                            onTap()
                        }
                    }
            )
        }
        .onAppear { refreshCartCount() }
        .onReceive(container.localStorage.changes) { _ in refreshCartCount() }
    }

    private func refreshCartCount() {
        cartCount = container.localStorage.getCartItemCount()
    }
}
