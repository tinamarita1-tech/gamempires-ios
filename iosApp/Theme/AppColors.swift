import SwiftUI

struct AppColors {
    // Primary Corporate Blue Palette (#2563EB)
    static let corporateBlue = Color(hex: 0x2563EB)
    static let corporateBlueDark = Color(hex: 0x1D4ED8)
    static let corporateBlueLight = Color(hex: 0x3B82F6)
    static let corporateBlue50 = Color(hex: 0xEFF6FF)
    
    // Status Indicators
    static let statusOnline = Color(hex: 0x10B981)
    static let statusAway = Color(hex: 0xF59E0B)
    static let statusFocus = Color(hex: 0x6366F1)
    static let statusOffline = Color(hex: 0x94A3B8)
    
    // Semantic Theme Colors (Automatic Light / Dark adaptation)
    static func background(for scheme: ColorScheme) -> Color {
        scheme == .dark ? Color(hex: 0x020617) : Color(hex: 0xFAFAF9)
    }
    
    static func cardSurface(for scheme: ColorScheme) -> Color {
        scheme == .dark ? Color(hex: 0x0F172A) : Color(hex: 0xFFFFFF)
    }
    
    static func surfaceVariant(for scheme: ColorScheme) -> Color {
        scheme == .dark ? Color(hex: 0x1E293B) : Color(hex: 0xF1F5F9)
    }
    
    static func border(for scheme: ColorScheme) -> Color {
        scheme == .dark ? Color(hex: 0x1E293B) : Color(hex: 0xE2E8F0)
    }
    
    static func textPrimary(for scheme: ColorScheme) -> Color {
        scheme == .dark ? Color(hex: 0xF8FAFC) : Color(hex: 0x0F172A)
    }
    
    static func textSecondary(for scheme: ColorScheme) -> Color {
        scheme == .dark ? Color(hex: 0x94A3B8) : Color(hex: 0x64748B)
    }
}

extension Color {
    init(hex: UInt, alpha: Double = 1.0) {
        self.init(
            .sRGB,
            red: Double((hex >> 16) & 0xff) / 255,
            green: Double((hex >> 08) & 0xff) / 255,
            blue: Double((hex >> 00) & 0xff) / 255,
            opacity: alpha
        )
    }
}
