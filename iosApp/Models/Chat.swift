import Foundation

enum ChatType: String, Codable {
    case direct
    case group
}

struct Chat: Identifiable, Codable {
    let id: String
    let title: String
    let type: ChatType
    let avatarInitials: String
    let lastMessage: String
    let timestamp: String
    let unreadCount: Int
    let isPinned: Bool
    var presenceStatus: PresenceStatus
}
