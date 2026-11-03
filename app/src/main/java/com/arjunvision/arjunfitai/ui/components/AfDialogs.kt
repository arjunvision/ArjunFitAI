package com.arjunvision.arjunfitai.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arjunvision.arjunfitai.ui.theme.AfColors
import com.arjunvision.arjunfitai.ui.theme.AfShapes

/** Dark card dialog with a confirm button (and Cancel unless [showCancel] is false). */
@Composable
fun AfDialog(
    title: String,
    onDismiss: () -> Unit,
    confirm: Pair<String, () -> Unit>,
    confirmEnabled: Boolean = true,
    showCancel: Boolean = true,
    content: @Composable () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = confirm.second, enabled = confirmEnabled) {
                Text(confirm.first, color = if (confirmEnabled) AfColors.AccentSoft else AfColors.TextMuted)
            }
        },
        dismissButton = if (showCancel) ({ TextButton(onClick = onDismiss) { Text("Cancel", color = AfColors.Text) } }) else null,
        title = { Text(title) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { content() } },
        containerColor = AfColors.Card,
        titleContentColor = AfColors.Text,
        textContentColor = AfColors.TextMuted,
        shape = AfShapes.Card,
    )
}

/** Outlined text field in ArjunFit colours. */
@Composable
fun AfTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboard: KeyboardOptions,
    error: String? = null,
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation = androidx.compose.ui.text.input.VisualTransformation.None,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        keyboardOptions = keyboard,
        visualTransformation = visualTransformation,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AfColors.Accent,
            unfocusedBorderColor = AfColors.Border,
            focusedLabelColor = AfColors.AccentSoft,
            cursorColor = AfColors.Accent,
            focusedTextColor = AfColors.Text,
            unfocusedTextColor = AfColors.Text,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

