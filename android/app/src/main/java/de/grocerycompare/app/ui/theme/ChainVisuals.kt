package de.grocerycompare.app.ui.theme

import androidx.compose.ui.graphics.Color

/** Brand color + short label + initials badge for each chain, for consistent visuals. */
data class ChainVisual(val label: String, val color: Color, val initials: String)

private val VISUALS = mapOf(
    "lidl" to ChainVisual("Lidl", Color(0xFF0050AA), "L"),
    "aldi_sued" to ChainVisual("ALDI SÜD", Color(0xFF009FE3), "AS"),
    "aldi_nord" to ChainVisual("ALDI Nord", Color(0xFF002F6C), "AN"),
    "netto" to ChainVisual("Netto", Color(0xFFE30613), "N"),
    "kaufland" to ChainVisual("Kaufland", Color(0xFFE10915), "K"),
)

fun chainVisual(chain: String): ChainVisual =
    VISUALS[chain] ?: ChainVisual(chain.replaceFirstChar { it.uppercase() }, Color(0xFF6E6E6E), "?")
