package de.grocerycompare.app.ui.search

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.grocerycompare.app.R
import de.grocerycompare.app.ui.components.EmptyState
import de.grocerycompare.app.ui.components.OfferCard
import de.grocerycompare.app.ui.components.OfferCardSkeleton
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    modifier: Modifier = Modifier,
    vm: SearchViewModel = hiltViewModel(),
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val results by vm.results.collectAsStateWithLifecycle()
    val watched by vm.watchedQueries.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    val voiceLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()?.let { spoken ->
                    vm.onQueryChange(spoken)
                    vm.submit()
                }
        }
    }

    Column(modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(8.dp))
        Text(
            "GroceryCompare",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            "Wo ist es diese Woche am günstigsten?",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = ui.plz,
            onValueChange = vm::onPlzChange,
            label = { Text("PLZ") },
            leadingIcon = {
                Icon(androidx.compose.material.icons.Icons.Filled.LocationOn, null)
            },
            singleLine = true,
            shape = MaterialTheme.shapes.large,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = ui.query,
            onValueChange = vm::onQueryChange,
            placeholder = { Text(stringResource(R.string.search_hint)) },
            leadingIcon = { Icon(Icons.Filled.Search, null) },
            trailingIcon = {
                IconButton(onClick = {
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.GERMANY.toString())
                    }
                    voiceLauncher.launch(intent)
                }) { Icon(Icons.Filled.Mic, stringResource(R.string.voice_search)) }
            },
            singleLine = true,
            shape = MaterialTheme.shapes.extraLarge,
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                onSearch = { vm.submit() }
            ),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                imeAction = ImeAction.Search
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        AnimatedVisibility(visible = ui.suggestions.isNotEmpty()) {
            androidx.compose.foundation.layout.FlowRow(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ui.suggestions.take(4).forEach { s ->
                    SuggestionChip(
                        onClick = { vm.onQueryChange(s); vm.submit() },
                        label = { Text(s, maxLines = 1) },
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        PullToRefreshBox(
            isRefreshing = ui.loading,
            onRefresh = { vm.submit() },
            modifier = Modifier.fillMaxSize(),
        ) {
            when {
                ui.loading && results.isEmpty() -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(5) { OfferCardSkeleton() }
                    }
                }
                results.isEmpty() -> {
                    EmptyState(
                        icon = Icons.Filled.ShoppingCart,
                        title = if (ui.query.isBlank()) "Wonach suchst du?" else "Keine Angebote gefunden",
                        subtitle = if (ui.query.isBlank())
                            "Tippe oder sprich einen Artikel wie \"Kartoffeln\"."
                        else "Für diese Woche gibt es hier keine Treffer.",
                    )
                }
                else -> {
                    LazyColumn(
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp),
                    ) {
                        if (ui.offline) item { OfflineBanner() }
                        androidx.compose.foundation.lazy.itemsIndexed(
                            results, key = { _, o -> o.id }
                        ) { index, offer ->
                            OfferCard(
                                offer = offer,
                                isBest = index == 0,
                                isWatched = watched.contains(offer.query),
                                onWatch = { vm.watch(offer.query) },
                                modifier = Modifier.animateItem(),
                            )
                        }
                        item {
                            Text(
                                stringResource(R.string.disclaimer),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OfflineBanner() {
    Text(
        stringResource(R.string.offline_notice),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    )
}
