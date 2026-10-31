package com.metrolist.linux.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.metrolist.shared.auth.CookieAuth

@Composable
internal fun SettingsScreen(auth: CookieAuth) {
    val signedIn by auth.signedIn.collectAsState()
    var pasted by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("YouTube Music account", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

        if (signedIn) {
            Text("You are signed in. Your playlists and liked songs are in the Library tab.")
            OutlinedButton(onClick = { auth.signOut() }) { Text("Sign out and forget the cookie") }
        } else {
            Text(
                "Paste the Cookie header your browser sends to music.youtube.com. In Firefox or Chrome: open " +
                    "music.youtube.com while signed in, press F12, open the Network tab, reload, click a request " +
                    "to music.youtube.com (for example \"browse\"), and copy the value of the \"Cookie\" request header.",
            )
            Text(
                "It works like a password for your Google account. It is saved only on this computer, in a file " +
                    "only you can read, and is sent only to music.youtube.com. Never share it.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = pasted,
                onValueChange = { pasted = it; error = null },
                label = { Text("Cookie header") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                isError = error != null,
                supportingText = error?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                enabled = pasted.isNotBlank(),
                onClick = {
                    if (auth.signIn(pasted)) pasted = ""
                    else error = "That does not look like a YouTube Music cookie (no SAPISID value found)."
                },
            ) { Text("Sign in") }
        }
    }
}
