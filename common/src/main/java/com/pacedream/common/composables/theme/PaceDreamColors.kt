package com.pacedream.common.composables.theme

import androidx.compose.ui.graphics.Color

/**
 * PaceDream color system matching iOS design
 * Object-based access for easier use in Compose
 */
object PaceDreamColors {
    // Brand Primary Colors
    val Primary = Color(0xFF5527D7)
    val PrimaryLight = Color(0xFF5527D7).copy(alpha = 0.1f)
    val PrimaryDark = Color(0xFF5527D7).copy(alpha = 0.8f)
    
    // Secondary Colors
    val Secondary = Color(0xFF3B82F6)
    val Accent = Color(0xFF7B4DFF)
    
    // Background & Surface
    val Background = Color(0xFFFFFFFF)
    val Surface = Color(0xFFF8F9FA)
    val Card = Color(0xFFFFFFFF)
    
    // Text Colors
    val TextPrimary = Color(0xFF1A1A1A)
    val TextSecondary = Color(0xFF6B7280)
    val TextTertiary = Color(0xFF9CA3AF)
    
    // Semantic Colors
    val Success = Color(0xFF10B981)
    val Warning = Color(0xFFF59E0B)
    val Error = Color(0xFFEF4444)
    val Info = Color(0xFF3B82F6)
    
    // Neutral Grays
    val Gray50 = Color(0xFFF9FAFB)
    val Gray100 = Color(0xFFF3F4F6)
    val Gray200 = Color(0xFFE5E7EB)
    val Gray300 = Color(0xFFD1D5DB)
    val Gray400 = Color(0xFF9CA3AF)
    val Gray500 = Color(0xFF6B7280)
    val Gray600 = Color(0xFF4B5563)
    val Gray700 = Color(0xFF374151)
    val Gray800 = Color(0xFF1F2937)
    val Gray900 = Color(0xFF111827)
    
    // Transparent
    val Transparent = Color.Transparent
    
    // Common UI Colors
    val Divider = Gray200
    val Border = Gray300
    val Disabled = Gray400
    val Overlay = Color.Black.copy(alpha = 0.5f)
    
    // Status Colors
    val StatusActive = Success
    val StatusInactive = Gray400
    val StatusPending = Warning
    val StatusError = Error
}

