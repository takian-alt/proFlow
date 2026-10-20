package com.neuroflow.app.presentation.notes

import androidx.compose.ui.graphics.Color

data class NoteColorItem(
    val id: String,
    val name: String,
    val lightColor: Color,
    val darkColor: Color
)

object NoteColors {
    val items = listOf(
        NoteColorItem(
            id = "DEFAULT",
            name = "Default",
            lightColor = Color(0xFFF7F7F9),
            darkColor = Color(0xFF1E1E24)
        ),
        NoteColorItem(
            id = "BUTTER",
            name = "Butter",
            lightColor = Color(0xFFFFF9C4),
            darkColor = Color(0xFF383318)
        ),
        NoteColorItem(
            id = "PEACH",
            name = "Peach",
            lightColor = Color(0xFFFFE0B2),
            darkColor = Color(0xFF3A2818)
        ),
        NoteColorItem(
            id = "CORAL",
            name = "Coral",
            lightColor = Color(0xFFFFCDD2),
            darkColor = Color(0xFF3B1E22)
        ),
        NoteColorItem(
            id = "MINT",
            name = "Mint",
            lightColor = Color(0xFFC8E6C9),
            darkColor = Color(0xFF18331E)
        ),
        NoteColorItem(
            id = "TEAL",
            name = "Teal",
            lightColor = Color(0xFFB2DFDB),
            darkColor = Color(0xFF143230)
        ),
        NoteColorItem(
            id = "SKY",
            name = "Sky",
            lightColor = Color(0xFFBBDEFB),
            darkColor = Color(0xFF182A3E)
        ),
        NoteColorItem(
            id = "LAVENDER",
            name = "Lavender",
            lightColor = Color(0xFFE1BEE7),
            darkColor = Color(0xFF32193A)
        ),
        NoteColorItem(
            id = "ROSE",
            name = "Rose",
            lightColor = Color(0xFFF8BBD0),
            darkColor = Color(0xFF381928)
        )
    )

    fun getColor(colorId: String?, isDark: Boolean): Color {
        val found = items.find { it.id.equals(colorId, ignoreCase = true) } ?: items.first()
        return if (isDark) found.darkColor else found.lightColor
    }
}
