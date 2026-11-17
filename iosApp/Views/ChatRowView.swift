import SwiftUI

struct ChatRowView: View {
    let chat: Chat
    @Environment(\.colorScheme) var colorScheme

    var presenceColor: Color {
        switch chat.presenceStatus {
        case .online: return AppColors.statusOnline
        case .busy: return AppColors.statusAway
        case .focus: return AppColors.statusFocus
        case .away, .offline: return AppColors.statusOffline
        }
    }

    var body: some View {
        HStack(spacing: 12) {
            // Rounded Avatar with presence dot
            ZStack(alignment: .bottomTrailing) {
                RoundedRectangle(cornerRadius: 14)
                    .fill(AppColors.corporateBlue50)
                    .frame(width: 44, height: 44)
                    .overlay(
                        Text(chat.avatarInitials)
                            .font(.system(size: 14, weight: .bold))
                            .foregroundColor(AppColors.corporateBlue)
                    )
                
                Circle()
                    .fill(presenceColor)
                    .frame(width: 11, height: 11)
                    .overlay(
                        Circle().stroke(AppColors.cardSurface(for: colorScheme), lineWidth: 2)
                    )
                    .offset(x: 2, y: 2)
            }

            // Chat title and last message preview
            VStack(alignment: .leading, spacing: 3) {
                HStack {
                    Text(chat.title)
                        .font(.system(size: 14, weight: .bold))
                        .foregroundColor(AppColors.textPrimary(for: colorScheme))
                        .lineLimit(1)
                    Spacer()
                    Text(chat.timestamp)
                        .font(.system(size: 11, weight: .medium))
                        .foregroundColor(AppColors.textSecondary(for: colorScheme))
                }
                
                Text(chat.lastMessage)
                    .font(.system(size: 12))
                    .foregroundColor(AppColors.textSecondary(for: colorScheme))
                    .lineLimit(1)
            }

            // Pinned indicator & unread badge
            VStack(alignment: .trailing, spacing: 4) {
                if chat.isPinned {
                    Image(systemName: "pin.fill")
                        .font(.system(size: 11))
                        .foregroundColor(AppColors.corporateBlue)
                }
                
                if chat.unreadCount > 0 {
                    Text("\(chat.unreadCount)")
                        .font(.system(size: 10, weight: .bold))
                        .foregroundColor(.white)
                        .frame(width: 18, height: 18)
                        .background(AppColors.corporateBlue)
                        .clipShape(Circle())
                }
            }
        }
        .padding(14)
        .background(AppColors.cardSurface(for: colorScheme))
        .cornerRadius(18)
        .overlay(
            RoundedRectangle(cornerRadius: 18)
                .stroke(AppColors.border(for: colorScheme), lineWidth: 1)
        )
    }
}
