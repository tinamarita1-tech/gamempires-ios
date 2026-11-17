import Foundation

enum PresenceStatus: String, Codable, CaseIterable {
    case online = "Available"
    case away = "Stepping away"
    case busy = "In a meeting"
    case focus = "Focus time"
    case offline = "Offline"
}

struct User: Identifiable, Codable {
    let id: String
    let name: String
    let email: String?
    let avatarUrl: String?
    var status: PresenceStatus
}
