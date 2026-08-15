package de.grocerycompare.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.grocerycompare.app.ui.theme.chainVisual

/** Rounded, brand-colored initials badge for a chain. */
@Composable
fun ChainBadge(chain: String, size: Int = 44) {
    val v = chainVisual(chain)
    Box(
        Modifier
            .size(size.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(v.color),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            v.initials,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = (size / 2.6).sp,
        )
    }
}
