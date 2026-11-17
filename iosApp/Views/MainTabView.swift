import SwiftUI

struct MainTabView: View {
    @State private var selectedTab = 0
    @Environment(\.colorScheme) var colorScheme

    var body: some View {
        TabView(selection: $selectedTab) {
            ChatListView()
                .tabItem {
                    Label("Chats", systemImage: "message.fill")
                }
                .tag(0)

            HomeTabView()
                .tabItem {
                    Label("Home", systemImage: "house.fill")
                }
                .tag(1)

            Text("Contacts")
                .foregroundColor(AppColors.textSecondary(for: colorScheme))
                .tabItem {
                    Label("Contacts", systemImage: "person.2.fill")
                }
                .tag(2)

            Text("Settings")
                .foregroundColor(AppColors.textSecondary(for: colorScheme))
                .tabItem {
                    Label("Settings", systemImage: "gearshape.fill")
                }
                .tag(3)
        }
        .accentColor(AppColors.corporateBlue)
    }
}
