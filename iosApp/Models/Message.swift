import Foundation

struct Message: Identifiable, Codable {
    let id: String
    let chatId: String
    let senderId: String
    let senderName: String
    let content: String
    let timestamp: Date
    let isSelf: BooleanLiteralType
}
