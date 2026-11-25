package com.example.sportstats.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.example.sportstats.R   // adapte avec le nom de ton package

val SNPro = FontFamily(
    Font(R.font.sn_pro_regular, FontWeight.Normal),
    Font(R.font.sn_pro_medium, FontWeight.Medium),
    Font(R.font.sn_pro_semibold, FontWeight.SemiBold),
    Font(R.font.sn_pro_bold, FontWeight.Bold)
)

private val base = Typography()

val Typography = Typography(
    displayLarge = base.displayLarge.copy(fontFamily = SNPro),
    displayMedium = base.displayMedium.copy(fontFamily = SNPro),
    displaySmall = base.displaySmall.copy(fontFamily = SNPro),
    headlineLarge = base.headlineLarge.copy(fontFamily = SNPro),
    headlineMedium = base.headlineMedium.copy(fontFamily = SNPro),
    headlineSmall = base.headlineSmall.copy(fontFamily = SNPro),
    titleLarge = base.titleLarge.copy(fontFamily = SNPro),
    titleMedium = base.titleMedium.copy(fontFamily = SNPro),
    titleSmall = base.titleSmall.copy(fontFamily = SNPro),
    bodyLarge = base.bodyLarge.copy(fontFamily = SNPro),
    bodyMedium = base.bodyMedium.copy(fontFamily = SNPro),
    bodySmall = base.bodySmall.copy(fontFamily = SNPro),
    labelLarge = base.labelLarge.copy(fontFamily = SNPro),
    labelMedium = base.labelMedium.copy(fontFamily = SNPro),
    labelSmall = base.labelSmall.copy(fontFamily = SNPro)
)