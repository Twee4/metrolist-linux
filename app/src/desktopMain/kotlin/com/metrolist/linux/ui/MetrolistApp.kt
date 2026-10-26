package com.metrolist.linux.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.metrolist.shared.model.Track
import com.metrolist.shared.state.AppState

private enum class NavItem(val label: String) {
    HOME("Home"),
    SEARCH("Search"),
    LIBRARY("Library"),
    SETTINGS("Settings")
}

@Composable
fun MetrolistApp() {
    val appState = remember { AppState() }
    var selected by remember { mutableStateOf(NavItem.HOME) }
    var query by remember { mutableStateOf("") }
    val results = appState.results

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail {
                    NavItem.entries.forEach { item ->
                        val icon = when (item) {
                            NavItem.HOME -> Icons.Default.Home
                            NavItem.SEARCH -> Icons.Default.Search
                            NavItem.LIBRARY -> Icons.Default.LibraryMusic
                            NavItem.SETTINGS -> Icons.Default.Settings
                        }

                        NavigationRailItem(
                            selected = item == selected,
                            onClick = { selected = item },
                            icon = { Icon(icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                        )
                    }
                }

                Divider(modifier = Modifier.fillMaxHeight().padding(vertical = 8.dp))

                Box(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                    when (selected) {
                        NavItem.HOME -> HomeScreen()
                        NavItem.SEARCH -> SearchScreen(
                            query = query,
                            onQueryChange = {
                                query = it
                                appState.updateQuery(it)
                            },
                            tracks = results.value,
                        )
                        NavItem.LIBRARY -> LibraryScreen()
                        NavItem.SETTINGS -> SettingsScreen()
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeScreen() {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Metrolist Linux", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("A native desktop port foundation for the Metrolist project.")

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Status", style = MaterialTheme.typography.titleMedium)
                Text("Shared state and desktop shell are ready. Music logic is still being ported from the Android codebase.")
            }
        }
    }
}

@Composable
private fun SearchScreen(
    query: String,
    onQueryChange: (String) -> Unit,
    tracks: List<Track>,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            label = { Text("Search music") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        if (tracks.isEmpty()) {
            Text("Type to search. This is the Linux desktop starter flow.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(tracks) { track ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column {
                                Text(track.title, fontWeight = FontWeight.SemiBold)
                                Text(track.artist)
                            }
                            Text("${track.durationSeconds / 60}:${String.format("%02d", track.durationSeconds % 60)}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryScreen() {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Library", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Library management will be added as the desktop port matures.")
    }
}

@Composable
private fun SettingsScreen() {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Linux app settings and system integration are planned next.")
    }
}
