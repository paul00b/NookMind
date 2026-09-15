package fr.paulbr.nookmind.core.designsystem

import androidx.compose.ui.graphics.Color

/** Tailwind CSS v4 default palette — the exact hex values the web app renders. */
object Palette {
    val White = Color(0xFFFFFFFF)
    val Black = Color(0xFF000000)

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
    val Gray950 = Color(0xFF030712)

    val Amber50 = Color(0xFFFFFBEB)
    val Amber100 = Color(0xFFFEF3C7)
    val Amber200 = Color(0xFFFDE68A)
    val Amber300 = Color(0xFFFCD34D)
    val Amber400 = Color(0xFFFBBF24)
    val Amber500 = Color(0xFFF59E0B)
    val Amber600 = Color(0xFFD97706)
    val Amber700 = Color(0xFFB45309)
    val Amber900 = Color(0xFF78350F)

    val Indigo400 = Color(0xFF818CF8)
    val Indigo500 = Color(0xFF6366F1)
    val Indigo600 = Color(0xFF4F46E5)

    val Teal300 = Color(0xFF5EEAD4)
    val Teal400 = Color(0xFF2DD4BF)
    val Teal500 = Color(0xFF14B8A6)
    val Teal600 = Color(0xFF0D9488)
    val Teal700 = Color(0xFF0F766E)

    val Emerald50 = Color(0xFFECFDF5)
    val Emerald100 = Color(0xFFD1FAE5)
    val Emerald200 = Color(0xFFA7F3D0)
    val Emerald300 = Color(0xFF6EE7B7)
    val Emerald400 = Color(0xFF34D399)
    val Emerald500 = Color(0xFF10B981)
    val Emerald600 = Color(0xFF059669)
    val Emerald700 = Color(0xFF047857)
    val Emerald900 = Color(0xFF064E3B)

    val Blue400 = Color(0xFF60A5FA)
    val Blue500 = Color(0xFF3B82F6)
    val Blue600 = Color(0xFF2563EB)

    val Purple400 = Color(0xFFC084FC)
    val Purple500 = Color(0xFFA855F7)

    val Sky500 = Color(0xFF0EA5E9)

    val Red50 = Color(0xFFFEF2F2)
    val Red200 = Color(0xFFFECACA)
    val Red300 = Color(0xFFFCA5A5)
    val Red400 = Color(0xFFF87171)
    val Red500 = Color(0xFFEF4444)
    val Red600 = Color(0xFFDC2626)
    val Red700 = Color(0xFFB91C1C)
    val Red800 = Color(0xFF991B1B)
    val Red900 = Color(0xFF7F1D1D)

    val Violet500 = Color(0xFF8B5CF6)
    val Rose500 = Color(0xFFF43F5E)

    val Green400 = Color(0xFF4ADE80)
    val Green600 = Color(0xFF16A34A)
    val Green900 = Color(0xFF14532D)
    val Yellow400 = Color(0xFFFACC15)
    val Yellow900 = Color(0xFF713F12)
    val Orange500 = Color(0xFFF97316)

    /** App-specific brand backgrounds. */
    val Cream = Color(0xFFF8F6F1)
    val Night = Color(0xFF0F1117)
    val NightCard = Color(0xFF1A1F2E)
}

fun Color.alpha(alpha: Float): Color = copy(alpha = alpha)
