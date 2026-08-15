package de.grocerycompare.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.grocerycompare.app.data.local.OfferEntity
import de.grocerycompare.app.ui.theme.DealGreen
import de.grocerycompare.app.ui.theme.chainVisual

@Composable
fun OfferCard(
    offer: OfferEntity,
    isBest: Boolean,
    isWatched: Boolean,
    onWatch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val v = chainVisual(offer.chain)

    val borderColor by animateColorAsState(
        if (isBest) DealGreen else Color.Transparent, label = "border"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable { expanded = !expanded }
            .animateContentSize(spring(stiffness = Spring.StiffnessMediumLow)),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = if (isBest) 4.dp else 1.dp,
        shadowElevation = if (isBest) 8.dp else 2.dp,
        border = androidx.compose.foundation.BorderStroke(if (isBest) 2.dp else 0.dp, borderColor),
    ) {
        Column(Modifier.padding(16.dp)) {
            if (isBest) BestDealRibbon()

            Row(verticalAlignment = Alignment.CenterVertically) {
                ChainBadge(offer.chain)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(v.label, style = MaterialTheme.typography.labelLarge, color = v.color)
                    Text(
                        offer.productName,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = if (expanded) 3 else 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    offer.brand?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    AnimatedPrice(offer.price, style = MaterialTheme.typography.headlineSmall)
                    if (offer.unitPrice != null && offer.unitPriceUnit != null) {
                        Text(
                            "%.2f €/%s".format(offer.unitPrice, offer.unitPriceUnit),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (offer.priceType == "offer") {
                    Pill(text = "Angebot", icon = Icons.Filled.Bolt,
                        bg = MaterialTheme.colorScheme.secondaryContainer,
                        fg = MaterialTheme.colorScheme.onSecondaryContainer)
                } else {
                    Pill(text = "Dauerpreis",
                        bg = MaterialTheme.colorScheme.surfaceVariant,
                        fg = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                offer.distanceKm?.let {
                    Pill(text = "%.1f km".format(it), icon = Icons.Filled.LocationOn,
                        bg = MaterialTheme.colorScheme.surfaceVariant,
                        fg = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                offer.packageSize?.let {
                    Pill(text = it, bg = MaterialTheme.colorScheme.surfaceVariant,
                        fg = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Column {
                    Spacer(Modifier.height(12.dp))
                    offer.validTo?.let {
                        DetailRow("Gültig bis", it)
                    }
                    offer.validFrom?.let { DetailRow("Gültig ab", it) }
                    offer.storeAddress?.let { DetailRow("Filiale", it) }
                    DetailRow("Quelle", offer.source)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onWatch() }
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            if (isWatched) Icons.Filled.Notifications
                            else Icons.Outlined.NotificationsNone,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (isWatched) "Wird beobachtet" else "Preis beobachten",
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BestDealRibbon() {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(DealGreen)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Bolt, null, tint = Color.White, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text("Bester Preis", color = Color.White, style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold)
    }
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun Pill(
    text: String,
    bg: Color,
    fg: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
) {
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(bg).padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = fg, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
        }
        Text(text, color = fg, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text("$label: ", style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
