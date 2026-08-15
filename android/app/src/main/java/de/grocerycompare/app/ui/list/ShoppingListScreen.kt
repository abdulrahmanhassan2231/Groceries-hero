package de.grocerycompare.app.ui.list

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import de.grocerycompare.app.data.remote.dto.OfferDto
import de.grocerycompare.app.ui.components.AnimatedPrice
import de.grocerycompare.app.ui.theme.chainVisual

@Composable
fun ShoppingListScreen(
    modifier: Modifier = Modifier,
    vm: ShoppingListViewModel = hiltViewModel(),
) {
    val ui by vm.ui.collectAsStateWithLifecycle()

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text("Einkaufsliste", style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary)
        Text("Mehrere Artikel → bester Markt oder optimale Aufteilung.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = ui.plz,
            onValueChange = vm::onPlzChange,
            label = { Text("PLZ") },
            leadingIcon = { Icon(androidx.compose.material.icons.Icons.Filled.LocationOn, null) },
            singleLine = true,
            shape = MaterialTheme.shapes.large,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = ui.draft,
                onValueChange = vm::onDraftChange,
                placeholder = { Text("Artikel hinzufügen") },
                singleLine = true,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = vm::addItem) {
                Icon(Icons.Filled.Add, "Hinzufügen",
                    tint = MaterialTheme.colorScheme.primary)
            }
        }

        FlowRow(
            Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ui.items.forEach { item ->
                AssistChip(
                    onClick = { vm.removeItem(item) },
                    label = { Text(item) },
                    trailingIcon = { Icon(Icons.Filled.Close, "Entfernen",
                        Modifier.height(16.dp)) },
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = vm::compare,
            enabled = ui.items.isNotEmpty() && !ui.loading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (ui.loading) {
                CircularProgressIndicator(Modifier.height(18.dp), strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text("Preise vergleichen", fontWeight = FontWeight.SemiBold)
            }
        }

        AnimatedVisibility(
            visible = ui.result != null,
            enter = fadeIn() + slideInVertically { it / 6 },
            exit = fadeOut(),
        ) {
            ui.result?.let { r ->
                Column(Modifier.padding(top = 20.dp)) {
                    r.singleStore?.let { single ->
                        val v = chainVisual(single.chain)
                        Surface(
                            Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shadowElevation = 6.dp,
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Storefront, null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Bester Einzel-Markt",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(v.label, style = MaterialTheme.typography.headlineSmall,
                                    color = v.color)
                                AnimatedPrice(single.total,
                                    style = MaterialTheme.typography.displaySmall)
                                Text("${single.count} von ${ui.items.size} Artikeln verfügbar",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Text("Optimale Aufteilung",
                        style = MaterialTheme.typography.titleMedium)
                    Text("Jeder Artikel im jeweils günstigsten Markt",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    r.optimalSplit.items.forEach { (item, offer) ->
                        SplitRow(item, offer)
                        Spacer(Modifier.height(8.dp))
                    }
                    Row(
                        Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Gesamt (Split)", fontWeight = FontWeight.SemiBold)
                        AnimatedPrice(r.optimalSplit.total,
                            style = MaterialTheme.typography.titleLarge)
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(r.disclaimer, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun SplitRow(item: String, offer: OfferDto) {
    val v = chainVisual(offer.chain)
    Surface(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(item.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleMedium)
                Text(offer.productName, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(v.label, style = MaterialTheme.typography.labelSmall, color = v.color)
            }
            Text("%.2f €".format(offer.price),
                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}
