package de.grocerycompare.app.ui.watched

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.grocerycompare.app.data.local.WatchedItemEntity
import de.grocerycompare.app.ui.components.EmptyState
import de.grocerycompare.app.ui.theme.chainVisual

@Composable
fun WatchedScreen(
    modifier: Modifier = Modifier,
    vm: WatchedViewModel = hiltViewModel(),
) {
    val watched by vm.watched.collectAsStateWithLifecycle()

    Column(modifier.fillMaxSize().padding(16.dp)) {
        Text("Beobachtet", style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary)
        Text("Wir benachrichtigen dich bei Preissenkungen.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))

        if (watched.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.FavoriteBorder,
                title = "Noch nichts beobachtet",
                subtitle = "Tippe bei einem Angebot auf die Glocke, um seinen Preis zu verfolgen.",
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(watched, key = { it.query }) { item ->
                    WatchedRow(item, onDelete = { vm.unwatch(item.query) },
                        modifier = Modifier.animateItem())
                }
            }
        }
    }
}

@Composable
private fun WatchedRow(
    item: WatchedItemEntity,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(item.query.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleMedium)
                if (item.lastBestUnitPrice != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.TrendingDown, null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.height(16.dp))
                        Spacer(Modifier.height(0.dp))
                        Text(
                            "  bester Preis: %.2f €%s".format(
                                item.lastBestUnitPrice,
                                item.lastBestChain?.let { " · " + chainVisual(it).label } ?: "",
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    Text("PLZ ${item.plz} · warte auf nächste Preisprüfung",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, "Entfernen",
                    tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
