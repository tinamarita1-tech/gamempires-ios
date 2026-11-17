import SwiftUI

struct PresenceSelectorSheet: View {
    @Binding var currentStatus: PresenceStatus
    @Environment(\.dismiss) var dismiss
    @Environment(\.colorScheme) var colorScheme

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack {
                Text("Set Availability")
                    .font(.system(size: 16, weight: .bold))
                    .foregroundColor(AppColors.textPrimary(for: colorScheme))
                Spacer()
                Button("Done") {
                    dismiss()
                }
                .font(.system(size: 14, weight: .bold))
                .foregroundColor(AppColors.corporateBlue)
            }
            .padding(.top, 8)

            Divider()

            ForEach(PresenceStatus.allCases, id: \.self) { status in
                Button(action: {
                    currentStatus = status
                    dismiss()
                }) {
                    HStack(spacing: 12) {
                        Circle()
                            .fill(colorForStatus(status))
                            .frame(width: 10, height: 10)
                        
                        Text(status.rawValue)
                            .font(.system(size: 14, weight: .medium))
                            .foregroundColor(AppColors.textPrimary(for: colorScheme))
                        
                        Spacer()
                        
                        if currentStatus == status {
                            Image(systemName: "checkmark")
                                .font(.system(size: 12, weight: .bold))
                                .foregroundColor(AppColors.corporateBlue)
                        }
                    }
                    .padding(.vertical, 8)
                }
            }
            
            Spacer()
        }
        .padding(20)
        .presentationDetents([.fraction(0.4)])
        .background(AppColors.cardSurface(for: colorScheme))
    }

    private func colorForStatus(_ status: PresenceStatus) -> Color {
        switch status {
        case .online: return AppColors.statusOnline
        case .busy: return AppColors.statusAway
        case .focus: return AppColors.statusFocus
        case .away, .offline: return AppColors.statusOffline
        }
    }
}
