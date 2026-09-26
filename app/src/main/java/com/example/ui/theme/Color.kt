package com.example.ui.theme

import androidx.compose.ui.graphics.Color

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

// Geometric Balance Theme Base Colors
val GeoBackground = Color(0xFFF2F4F3)
val GeoTextPrimary: Color
    @Composable
    get() = MaterialTheme.colorScheme.onSurface

val GeoTextSecondary: Color
    @Composable
    get() = MaterialTheme.colorScheme.onSurfaceVariant
val GeoPrimaryTeal = Color(0xFF005F5F)
val GeoPrimaryTealContainer = Color(0xFFC7E6E6)
val GeoOnPrimaryTealContainer = Color(0xFF001F20)

// Emergency / Red accents
val GeoEmergencyBg = Color(0xFFFFDAD6)
val GeoEmergencyBorder = Color(0xFFFFB4AB)
val GeoEmergencyAccent = Color(0xFFBA1A1A)
val GeoEmergencyText = Color(0xFF3B0001) // Even darker red-black for better contrast
val GeoEmergencySubtext = Color(0xFF820007) // Darker subtext red

// Card-specific themes
val GeoOpdCardBg = Color(0xFFD6ECEB)     // Slightly deeper teal-grey for robust contrast
val GeoOpdCardBorder = Color(0xFFB5DAD8) // Darker border
val GeoOpdText = Color(0xFF001A1A)
val GeoOpdSubtext = Color(0xFF004343)

val GeoLockerCardBg = Color(0xFFE5ECE9)   // Rich off-white grey for robust contrast
val GeoLockerCardBorder = Color(0xFFC2D0CA) // Strongly visible border

val GeoBloodCardBg = Color(0xFFFBEBE6)    // Slightly deeper peach-grey
val GeoBloodCardBorder = Color(0xFFF3C7BE)
val GeoBloodText = Color(0xFF330C04)
val GeoBloodAccent = Color(0xFF8A3420)   // Darker blood accent

val GeoTriageCardBg = Color(0xFFE4EEF5)   // Deeper medical-blue
val GeoTriageCardBorder = Color(0xFFC0D5E5)
val GeoTriageText = Color(0xFF0A3C8B)
val GeoTriageAccent = Color(0xFF1565C0)

// Navigation elements
val GeoNavBg = Color(0xFFE2EBEA)         // Deeper clinical navigation background
val GeoNavIndicator = Color(0xFFC9D9D7)
val GeoNavTextActive = Color(0xFF001A1A)
val GeoNavTextInactive = Color(0xFF384745) // Prominent dark teal-grey for inactive tabs
