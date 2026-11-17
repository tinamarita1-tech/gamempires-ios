import SwiftUI

struct ChatListView: View {
    @Environment(\.colorScheme) var colorScheme
    @State private var searchQuery = ""
    @State private var selectedFilter = "All"
    @State private var presenceStatus: PresenceStatus = .online
    @State private var showPresenceSheet = false
    @State private var chats: [Chat] = [
        Chat(id: "1", title: "Team Shadow Lead", type: .direct, avatarInitials: "TS", lastMessage: "Designs are approved, building the cloud release today!", timestamp: "10:42 AM", unreadCount: 2, isPinned: true, presenceStatus: .online),
        Chat(id: "2", title: "GamEmpires Core Team", type: .group, avatarInitials: "GE", lastMessage: "Sarah: Updated all brand guidelines and cloud workflows.", timestamp: "Yesterday", unreadCount: 0, isPinned: true, presenceStatus: .busy),
        Chat(id: "3", title: "Marcus King", type: .direct, avatarInitials: "MK", lastMessage: "Can you send the updated meeting link?", timestamp: "Nov 15", unreadCount: 0, isPinned: false, presenceStatus: .online),
        Chat(id: "4", title: "Design Systems & Tokens", type: .group, avatarInitials: "DS", lastMessage: "Tokens validated for light and dark palettes.", timestamp: "Nov 14", unreadCount: 0, isPinned: false, presenceStatus: .away)
    ]

    let filterTabs = ["All", "Direct", "Teams", "Unread"]

    var filteredChats: [Chat] {
        var list = chats
        if !searchQuery.isEmpty {
            list = list.filter { $0.title.localizedCaseInsensitiveContains(searchQuery) || $0.lastMessage.localizedCaseInsensitiveContains(searchQuery) }
        }
        switch selectedFilter {
        case "Direct": return list.filter { $0.type == .direct }
        case "Teams": return list.filter { $0.type == .group }
        case "Unread": return list.filter { $0.unreadCount > 0 }
        default: return list
        }
    }

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                // Top Brand & Availability Bar
                HStack(spacing: 10) {
                    // Corporate Blue GE Rounded Container
                    HStack(spacing: 8) {
                        Text("GE")
                            .font(.system(size: 14, weight: .bold))
                            .foregroundColor(.white)
                            .frame(width: 36, height: 36)
                            .background(AppColors.corporateBlue)
                            .cornerRadius(12)

                        HStack(spacing: 6) {
                            Text("GamEmpires")
                                .font(.system(size: 16, weight: .bold))
                                .foregroundColor(AppColors.textPrimary(for: colorScheme))
                            
                            Text("Chat")
                                .font(.system(size: 11, weight: .semibold))
                                .foregroundColor(AppColors.corporateBlue)
                                .padding(.horizontal, 8)
                                .padding(.vertical, 2)
                                .background(AppColors.corporateBlue.opacity(0.1))
                                .clipShape(Capsule())
                        }
                    }

                    Spacer()

                    // Availability Status Pill
                    Button(action: { showPresenceSheet = true }) {
                        HStack(spacing: 6) {
                            Circle()
                                .fill(AppColors.statusOnline)
                                .frame(width: 8, height: 8)
                            
                            Text(presenceStatus.rawValue)
                                .font(.system(size: 11, weight: .semibold))
                                .foregroundColor(AppColors.textPrimary(for: colorScheme))
                            
                            Text("AA")
                                .font(.system(size: 11, weight: .bold))
                                .foregroundColor(.white)
                                .frame(width: 28, height: 28)
                                .background(AppColors.corporateBlue)
                                .clipShape(Circle())
                        }
                        .padding(.leading, 10)
                        .padding(.trailing, 4)
                        .padding(.vertical, 4)
                        .background(AppColors.cardSurface(for: colorScheme))
                        .clipShape(Capsule())
                        .overlay(Capsule().stroke(AppColors.border(for: colorScheme), lineWidth: 1))
                    }
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 10)
                .background(AppColors.cardSurface(for: colorScheme))

                // Search Bar
                HStack {
                    Image(systemName: "magnifyingglass")
                        .foregroundColor(AppColors.textSecondary(for: colorScheme))
                        .font(.system(size: 14))
                    TextField("Find teammates, chats, or shared docs...", text: $searchQuery)
                        .font(.system(size: 13))
                    if !searchQuery.isEmpty {
                        Button(action: { searchQuery = "" }) {
                            Image(systemName: "xmark.circle.fill")
                                .foregroundColor(AppColors.textSecondary(for: colorScheme))
                        }
                    }
                }
                .padding(.horizontal, 14)
                .padding(.vertical, 10)
                .background(AppColors.surfaceVariant(for: colorScheme))
                .clipShape(Capsule())
                .overlay(Capsule().stroke(AppColors.border(for: colorScheme), lineWidth: 1))
                .padding(.horizontal, 16)
                .padding(.top, 8)
                .padding(.bottom, 10)
                .background(AppColors.cardSurface(for: colorScheme))

                // Horizontal Pill Filter Tabs
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        ForEach(filterTabs, id: \.self) { tab in
                            Button(action: { selectedFilter = tab }) {
                                HStack(spacing: 6) {
                                    Text(tab)
                                        .font(.system(size: 12, weight: selectedFilter == tab ? .bold : .medium))
                                    
                                    if tab == "Unread" {
                                        Text("2")
                                            .font(.system(size: 10, weight: .bold))
                                            .foregroundColor(.white)
                                            .frame(width: 16, height: 16)
                                            .background(selectedFilter == tab ? Color.white.opacity(0.3) : AppColors.corporateBlue)
                                            .clipShape(Circle())
                                    }
                                }
                                .padding(.horizontal, 14)
                                .padding(.vertical, 7)
                                .background(selectedFilter == tab ? AppColors.corporateBlue : AppColors.surfaceVariant(for: colorScheme))
                                .foregroundColor(selectedFilter == tab ? .white : AppColors.textSecondary(for: colorScheme))
                                .clipShape(Capsule())
                            }
                        }
                    }
                    .padding(.horizontal, 16)
                    .padding(.bottom, 10)
                }
                .background(AppColors.cardSurface(for: colorScheme))

                Divider()

                // Chat Cards List
                ScrollView {
                    LazyVStack(spacing: 10) {
                        ForEach(filteredChats) { chat in
                            NavigationLink(destination: ChatDetailView(chat: chat)) {
                                ChatRowView(chat: chat)
                            }
                            .buttonStyle(PlainButtonStyle())
                        }
                    }
                    .padding(16)
                }
                .background(AppColors.background(for: colorScheme))
            }
            .navigationBarHidden(true)
            .sheet(isPresented: $showPresenceSheet) {
                PresenceSelectorSheet(currentStatus: $presenceStatus)
            }
        }
    }
}
