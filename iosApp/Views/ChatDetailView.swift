import SwiftUI

struct ChatDetailView: View {
    let chat: Chat
    @Environment(\.dismiss) var dismiss
    @Environment(\.colorScheme) var colorScheme
    @State private var inputText = ""
    @State private var messages: [Message] = [
        Message(id: "1", chatId: "1", senderId: "other", senderName: "Team Shadow", content: "Hi! How is the corporate theme integration looking on mobile?", timestamp: Date().addingTimeInterval(-3600), isSelf: false),
        Message(id: "2", chatId: "1", senderId: "me", senderName: "You", content: "Everything is completely matched with the corporate design tokens. The blue theme (#2563EB) and 24.dp rounded cards are implemented.", timestamp: Date().addingTimeInterval(-1800), isSelf: true),
        Message(id: "3", chatId: "1", senderId: "other", senderName: "Team Shadow", content: "Awesome! Let's prepare the cloud build for iOS as well.", timestamp: Date().addingTimeInterval(-600), isSelf: false)
    ]

    var body: some View {
        VStack(spacing: 0) {
            // Header Bar
            HStack(spacing: 12) {
                Button(action: { dismiss() }) {
                    Image(systemName: "chevron.left")
                        .font(.system(size: 16, weight: .bold))
                        .foregroundColor(AppColors.textPrimary(for: colorScheme))
                }

                HStack(spacing: 10) {
                    Text(chat.avatarInitials)
                        .font(.system(size: 12, weight: .bold))
                        .foregroundColor(AppColors.corporateBlue)
                        .frame(width: 36, height: 36)
                        .background(AppColors.corporateBlue50)
                        .cornerRadius(12)

                    VStack(alignment: .leading, spacing: 2) {
                        Text(chat.title)
                            .font(.system(size: 15, weight: .bold))
                            .foregroundColor(AppColors.textPrimary(for: colorScheme))
                        
                        Text("Active now")
                            .font(.system(size: 11))
                            .foregroundColor(AppColors.statusOnline)
                    }
                }

                Spacer()

                // Green Audio Call Button
                Button(action: {}) {
                    Image(systemName: "phone.fill")
                        .font(.system(size: 16))
                        .foregroundColor(AppColors.statusOnline)
                        .frame(width: 36, height: 36)
                        .background(AppColors.statusOnline.opacity(0.12))
                        .clipShape(Circle())
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 10)
            .background(AppColors.cardSurface(for: colorScheme))
            
            Divider()

            // Messages ScrollView
            ScrollView {
                LazyVStack(spacing: 12) {
                    ForEach(messages) { msg in
                        HStack {
                            if msg.isSelf {
                                Spacer(minLength: 40)
                                Text(msg.content)
                                    .font(.system(size: 14))
                                    .foregroundColor(.white)
                                    .padding(.horizontal, 16)
                                    .padding(.vertical, 10)
                                    .background(AppColors.corporateBlue)
                                    .cornerRadius(18)
                            } else {
                                Text(msg.content)
                                    .font(.system(size: 14))
                                    .foregroundColor(AppColors.textPrimary(for: colorScheme))
                                    .padding(.horizontal, 16)
                                    .padding(.vertical, 10)
                                    .background(AppColors.cardSurface(for: colorScheme))
                                    .cornerRadius(18)
                                    .overlay(
                                        RoundedRectangle(cornerRadius: 18)
                                            .stroke(AppColors.border(for: colorScheme), lineWidth: 1)
                                    )
                                Spacer(minLength: 40)
                            }
                        }
                    }
                }
                .padding(16)
            }
            .background(AppColors.background(for: colorScheme))

            // Bottom Input Bar
            HStack(spacing: 10) {
                Button(action: {}) {
                    Image(systemName: "paperclip")
                        .font(.system(size: 18))
                        .foregroundColor(AppColors.textSecondary(for: colorScheme))
                }

                TextField("Write a message...", text: $inputText)
                    .font(.system(size: 14))
                    .padding(.horizontal, 14)
                    .padding(.vertical, 10)
                    .background(AppColors.surfaceVariant(for: colorScheme))
                    .clipShape(Capsule())
                    .overlay(Capsule().stroke(AppColors.border(for: colorScheme), lineWidth: 1))

                Button(action: {
                    if !inputText.trimmingCharacters(in: .whitespaces).isEmpty {
                        let newMsg = Message(id: UUID().uuidString, chatId: chat.id, senderId: "me", senderName: "You", content: inputText, timestamp: Date(), isSelf: true)
                        messages.append(newMsg)
                        inputText = ""
                    }
                }) {
                    Image(systemName: "paperplane.fill")
                        .font(.system(size: 15))
                        .foregroundColor(.white)
                        .frame(width: 40, height: 40)
                        .background(AppColors.corporateBlue)
                        .clipShape(Circle())
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 10)
            .background(AppColors.cardSurface(for: colorScheme))
        }
        .navigationBarHidden(true)
    }
}
