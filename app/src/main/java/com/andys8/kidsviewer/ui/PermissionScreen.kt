package com.andys8.kidsviewer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.andys8.kidsviewer.R

@Composable
fun PermissionScreen(onGrantClick: () -> Unit, onOpenSettingsClick: () -> Unit) {
    MessageScreen(
        title = stringResource(R.string.permission_title),
        body = stringResource(R.string.permission_body)
    ) {
        Button(onClick = onGrantClick) {
            Text(stringResource(R.string.permission_grant_button))
        }
        TextButton(onClick = onOpenSettingsClick) {
            Text(stringResource(R.string.permission_open_settings_button), color = Color.White)
        }
    }
}

@Composable
fun EmptyLibraryScreen() {
    MessageScreen(
        title = stringResource(R.string.empty_title),
        body = stringResource(R.string.empty_body)
    )
}

@Composable
private fun MessageScreen(
    title: String,
    body: String,
    actions: @Composable () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp, bottom = 24.dp)
        )
        actions()
    }
}
