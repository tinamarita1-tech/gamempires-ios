import SwiftUI

struct ChatDetailView: View {
    let chat: Chat
    @Environment(\.dismiss) var dismiss
    @Environment(\.colorScheme) var colorScheme
    @State private var inputText = ""
    @State private var replyingTo: Message? = nil
    @State private var isRecording = false
    @State private var recordingSeconds = 0
    @State private var pinDismissed = false
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

            // 1. Pinned Announcement Banner
            if !pinDismissed {
                HStack(spacing: 10) {
                    ZStack {
                        RoundedRectangle(cornerRadius: 8)
                            .fill(AppColors.corporateBlue)
                            .frame(width: 26, height: 26)
                        Image(systemName: "pin.fill")
                            .font(.system(size: 12))
                            .foregroundColor(.white)
                    }

                    VStack(alignment: .leading, spacing: 1) {
                        Text("PINNED CONVERSATION")
                            .font(.system(size: 10, weight: .bold))
                            .foregroundColor(AppColors.corporateBlue)
                        Text(chat.title)
                            .font(.system(size: 12, weight: .medium))
                            .foregroundColor(AppColors.textPrimary(for: colorScheme))
                            .lineLimit(1)
                    }

                    Spacer()

                    Button(action: { pinDismissed = true }) {
                        Image(systemName: "xmark")
                            .font(.system(size: 12, weight: .bold))
                            .foregroundColor(AppColors.textSecondary(for: colorScheme))
                    }
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 8)
                .background(AppColors.corporateBlue.opacity(0.08))
                
                Divider()
            }

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
                                    .onTapGesture {
                                        replyingTo = msg
                                    }
                                Spacer(minLength: 40)
                            }
                        }
                    }
                }
                .padding(16)
            }
            .background(AppColors.background(for: colorScheme))

            // 2. Quoted Reply Banner
            if let replyMsg = replyingTo {
                HStack(spacing: 10) {
                    RoundedRectangle(cornerRadius: 2)
                        .fill(AppColors.corporateBlue)
                        .frame(width: 3, height: 28)
                    
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Replying to \(replyMsg.senderName)")
                            .font(.system(size: 11, weight: .bold))
                            .foregroundColor(AppColors.corporateBlue)
                        Text(replyMsg.content)
                            .font(.system(size: 12))
                            .foregroundColor(AppColors.textSecondary(for: colorScheme))
                            .lineLimit(1)
                    }

                    Spacer()

                    Button(action: { replyingTo = nil }) {
                        Image(systemName: "xmark")
                            .font(.system(size: 12, weight: .bold))
                            .foregroundColor(AppColors.textSecondary(for: colorScheme))
                    }
                }
                .padding(.horizontal, 14)
                .padding(.vertical, 8)
                .background(AppColors.surfaceVariant(for: colorScheme))
                .cornerRadius(12)
                .padding(.horizontal, 12)
                .padding(.top, 4)
            }

            // 3. Voice Recording Bar or Input Field
            if isRecording {
                HStack(spacing: 12) {
                    Circle()
                        .fill(Color.red)
                        .frame(width: 10, height: 10)
                    
                    Text("\(recordingSeconds / 60):\(recordingSeconds % 60 < 10 ? "0" : "")\(recordingSeconds % 60)")
                        .font(.system(size: 14, weight: .bold, design: .monospaced))
                        .foregroundColor(.red)

                    Text("Recording voice note...")
                        .font(.system(size: 12))
                        .foregroundColor(AppColors.textSecondary(for: colorScheme))

                    Spacer()

                    Button(action: { isRecording = false }) {
                        Image(systemName: "trash")
                            .font(.system(size: 16))
                            .foregroundColor(.red)
                    }

                    Button(action: { isRecording = false }) {
                        Image(systemName: "paperplane.fill")
                            .font(.system(size: 15))
                            .foregroundColor(.white)
                            .frame(width: 36, height: 36)
                            .background(AppColors.corporateBlue)
                            .clipShape(Circle())
                    }
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 12)
                .background(AppColors.cardSurface(for: colorScheme))
            } else {
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

                    if !inputText.trimmingCharacters(in: .whitespaces).isEmpty {
                        Button(action: {
                            let newMsg = Message(id: UUID().uuidString, chatId: chat.id, senderId: "me", senderName: "You", content: inputText, timestamp: Date(), isSelf: true)
                            messages.append(newMsg)
                            inputText = ""
                            replyingTo = nil
                        }) {
                            Image(systemName: "paperplane.fill")
                                .font(.system(size: 15))
                                .foregroundColor(.white)
                                .frame(width: 40, height: 40)
                                .background(AppColors.corporateBlue)
                                .clipShape(Circle())
                        }
                    } else {
                        Button(action: {
                            isRecording = true
                            recordingSeconds = 0
                        }) {
                            Image(systemName: "mic.fill")
                                .font(.system(size: 16))
                                .foregroundColor(AppColors.corporateBlue)
                                .frame(width: 40, height: 40)
                                .background(AppColors.surfaceVariant(for: colorScheme))
                                .clipShape(Circle())
                                .overlay(Circle().stroke(AppColors.border(for: colorScheme), lineWidth: 1))
                        }
                    }
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 10)
                .background(AppColors.cardSurface(for: colorScheme))
            }
        }
        .navigationBarHidden(true)
    }
}
