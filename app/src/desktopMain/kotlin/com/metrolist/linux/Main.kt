package com.metrolist.linux

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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.metrolist.shared.domain.MusicStore
import com.metrolist.shared.model.Track
import kotlinx.coroutines.launch

private enum class NavItem(val label: String) {
    HOME("Home"),
    SEARCH("Search"),
    LIBRARY("Library"),
    SETTINGS("Settings")
}

@Composable
fun App() {
    val store = remember { MusicStore() }
    var selected by remember { mutableStateOf(NavItem.HOME) }
    var query by remember { mutableStateOf("") }
    var tracks by remember { mutableStateOf(emptyList<Track>()) }
    var libraryTitle by remember { mutableStateOf("No library loaded") }

    LaunchedEffect(Unit) {
        launch {
            store.loadLibrary()
            libraryTitle = "Library ready"
        }
    }

    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail {
                    NavItem.entries.forEach { item ->
                        NavigationRailItem(
                            selected = item == selected,
                            onClick = { selected = item },
                            icon = { Icon(Icons.Default.Search, contentDescription = item.label) },
                            label = { Text(item.label) },
                        )
                    }
                }

                Divider(modifier = Modifier.fillMaxHeight().padding(vertical = 8.dp))

                Box(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                    when (selected) {
                        NavItem.HOME -> HomeContent()
                        NavItem.SEARCH -> SearchContent(
                            query = query,
                            onQueryChange = {
                                query = it
                                if (it.isBlank()) {
                                    tracks = emptyList()
                                    return@SearchContent
                                }
                                // This remains a lightweight desktop prototype; the actual API layer is being ported next.
                                tracks = listOf(
                                    Track("1", "Demo Search Result", "Metrolist", 214, null),
                                    Track("2", "Second Search Result", "Metrolist", 180, null)
                                ).filter { track ->
                                    track.title.contains(it, ignoreCase = true) || track.artist.contains(it, ignoreCase = true)
                                }
                            },
                            tracks = tracks,
                        )
                        NavItem.LIBRARY -> LibraryContent(title = libraryTitle)
                        NavItem.SETTINGS -> SettingsContent()
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeContent() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Metrolist Linux", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Native Linux desktop foundation for the Metrolist ecosystem.")
        Box(
            modifier = Modifier.fillMaxWidth().background(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                RoundedCornerShape(16.dp)
            ).padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Status", style = MaterialTheme.typography.titleMedium)
                Text("Shared domain layer is ready. The Android repo remains the source of truth for mobile features.")
            }
        }
    }
}

@Composable
private fun SearchContent(
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
            Text("Type to search. This is the desktop port starting point.")
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
private fun LibraryContent(title: String) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Library", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(title)
    }
}

@Composable
private fun SettingsContent() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Linux native settings will be added here as the port matures.")
    }
}

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Metrolist Linux",
    ) {
        App()
    }
}
