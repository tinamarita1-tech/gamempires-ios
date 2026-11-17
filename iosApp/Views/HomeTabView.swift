import SwiftUI

struct HomeTabView: View {
    @Environment(\.colorScheme) var colorScheme

    var greeting: String {
        let hour = Calendar.current.component(.hour, from: Date())
        switch hour {
        case 5..<12: return "Good morning"
        case 12..<17: return "Good afternoon"
        default: return "Good evening"
        }
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 18) {
                // Greeting Header
                VStack(alignment: .leading, spacing: 4) {
                    Text("\(greeting), Teammate 👋")
                        .font(.system(size: 22, weight: .bold))
                        .foregroundColor(AppColors.textPrimary(for: colorScheme))
                    
                    Text("GamEmpires Studio • Here's what's happening today")
                        .font(.system(size: 13))
                        .foregroundColor(AppColors.textSecondary(for: colorScheme))
                }
                .padding(.top, 10)

                // 1. Team Announcements Card (24pt rounded)
                VStack(alignment: .leading, spacing: 12) {
                    HStack {
                        HStack(spacing: 6) {
                            Circle()
                                .fill(AppColors.corporateBlue)
                                .frame(width: 8, height: 8)
                            
                            Text("Team Announcements")
                                .font(.system(size: 14, weight: .bold))
                                .foregroundColor(AppColors.textPrimary(for: colorScheme))
                        }
                        
                        Spacer()
                        
                        Text("People Team")
                            .font(.system(size: 10, weight: .semibold))
                            .foregroundColor(AppColors.corporateBlue)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 3)
                            .background(AppColors.corporateBlue.opacity(0.1))
                            .clipShape(Capsule())
                    }
                    
                    Text("Quarterly all-hands briefing and workspace architecture session scheduled for Friday.")
                        .font(.system(size: 13))
                        .foregroundColor(AppColors.textSecondary(for: colorScheme))
                }
                .padding(18)
                .background(AppColors.cardSurface(for: colorScheme))
                .cornerRadius(24)
                .overlay(
                    RoundedRectangle(cornerRadius: 24)
                        .stroke(AppColors.border(for: colorScheme), lineWidth: 1)
                )

                // 2. Upcoming Events & Meetings Card
                VStack(alignment: .leading, spacing: 12) {
                    HStack(spacing: 6) {
                        Circle()
                            .fill(Color(hex: 0x0EA5E9))
                            .frame(width: 8, height: 8)
                        
                        Text("Upcoming Events")
                            .font(.system(size: 14, weight: .bold))
                            .foregroundColor(AppColors.textPrimary(for: colorScheme))
                    }

                    HStack {
                        VStack(alignment: .leading, spacing: 2) {
                            Text("iOS Cloud Deployment Sync")
                                .font(.system(size: 13, weight: .semibold))
                                .foregroundColor(AppColors.textPrimary(for: colorScheme))
                            
                            Text("Today • 4:00 PM (Virtual)")
                                .font(.system(size: 11))
                                .foregroundColor(AppColors.textSecondary(for: colorScheme))
                        }
                        
                        Spacer()
                        
                        Text("Online")
                            .font(.system(size: 11, weight: .medium))
                            .foregroundColor(AppColors.textSecondary(for: colorScheme))
                            .padding(.horizontal, 10)
                            .padding(.vertical, 4)
                            .background(AppColors.surfaceVariant(for: colorScheme))
                            .clipShape(Capsule())
                    }
                    .padding(12)
                    .background(AppColors.surfaceVariant(for: colorScheme).opacity(0.6))
                    .cornerRadius(16)
                }
                .padding(18)
                .background(AppColors.cardSurface(for: colorScheme))
                .cornerRadius(24)
                .overlay(
                    RoundedRectangle(cornerRadius: 24)
                        .stroke(AppColors.border(for: colorScheme), lineWidth: 1)
                )

                // 3. Saved & Message Reminders Card
                HStack(spacing: 14) {
                    ZStack {
                        RoundedRectangle(cornerRadius: 14)
                            .fill(Color(hex: 0xCCFBF1))
                            .frame(width: 44, height: 44)
                        
                        Image(systemName: "bell.badge.fill")
                            .font(.system(size: 18))
                            .foregroundColor(Color(hex: 0x0D9488))
                    }

                    VStack(alignment: .leading, spacing: 2) {
                        Text("Saved & Message Reminders")
                            .font(.system(size: 14, weight: .bold))
                            .foregroundColor(AppColors.textPrimary(for: colorScheme))
                        
                        Text("Follow up on important items later")
                            .font(.system(size: 12))
                            .foregroundColor(AppColors.textSecondary(for: colorScheme))
                    }
                }
                .padding(18)
                .background(AppColors.cardSurface(for: colorScheme))
                .cornerRadius(24)
                .overlay(
                    RoundedRectangle(cornerRadius: 24)
                        .stroke(AppColors.border(for: colorScheme), lineWidth: 1)
                )
            }
            .padding(16)
        }
        .background(AppColors.background(for: colorScheme))
    }
}
