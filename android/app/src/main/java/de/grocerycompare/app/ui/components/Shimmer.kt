package de.grocerycompare.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** A moving-gradient shimmer brush for skeleton loading placeholders. */
@Composable
fun rememberShimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val x by transition.animateFloat(
        initialValue = -600f,
        targetValue = 600f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer-x",
    )
    val colors = listOf(
        Color.Gray.copy(alpha = 0.10f),
        Color.Gray.copy(alpha = 0.22f),
        Color.Gray.copy(alpha = 0.10f),
    )
    return Brush.linearGradient(colors, start = Offset(x, 0f), end = Offset(x + 300f, 300f))
}

@Composable
private fun ShimmerBox(modifier: Modifier, brush: Brush) {
    androidx.compose.foundation.layout.Box(
        modifier.clip(RoundedCornerShape(8.dp)).background(brush)
    )
}

/** Skeleton placeholder that mimics an offer card while data loads. */
@Composable
fun OfferCardSkeleton() {
    val brush = rememberShimmerBrush()
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Gray.copy(alpha = 0.06f))
            .padding(16.dp),
    ) {
        ShimmerBox(Modifier.width(120.dp).height(18.dp), brush)
        androidx.compose.foundation.layout.Spacer(Modifier.height(10.dp))
        ShimmerBox(Modifier.fillMaxWidth().height(14.dp), brush)
        androidx.compose.foundation.layout.Spacer(Modifier.height(8.dp))
        ShimmerBox(Modifier.width(180.dp).height(14.dp), brush)
        androidx.compose.foundation.layout.Spacer(Modifier.height(14.dp))
        ShimmerBox(Modifier.width(90.dp).height(24.dp), brush)
    }
}
