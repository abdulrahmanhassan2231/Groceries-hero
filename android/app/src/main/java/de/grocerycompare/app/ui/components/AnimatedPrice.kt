package de.grocerycompare.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import java.util.Locale

/** A price that smoothly counts up to its value when it first appears / changes. */
@Composable
fun AnimatedPrice(
    euros: Double,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    suffix: String = " €",
    weight: FontWeight = FontWeight.Bold,
) {
    val animated by animateFloatAsState(
        targetValue = euros.toFloat(),
        animationSpec = tween(650),
        label = "price",
    )
    Text(
        text = String.format(Locale.GERMANY, "%.2f", animated) + suffix,
        style = style.copy(fontWeight = weight),
        modifier = modifier,
    )
}
