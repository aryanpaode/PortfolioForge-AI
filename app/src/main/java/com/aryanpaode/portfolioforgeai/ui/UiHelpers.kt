@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.aryanpaode.portfolioforgeai.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.aryanpaode.portfolioforgeai.service.UrlPolicy

@Composable
fun SectionCard(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            content()
        }
    }
}

@Composable
fun AppTextField(
    value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier,
    singleLine: Boolean = true, minLines: Int = 1, allowPaste: Boolean = !singleLine,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    val clipboard = LocalClipboardManager.current
    OutlinedTextField(value = value, onValueChange = onValueChange, label = { Text(label) }, modifier = modifier.fillMaxWidth(),
        singleLine = singleLine, minLines = minLines, keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        trailingIcon = if (allowPaste) ({ TextButton(onClick = { clipboard.getText()?.text?.let(onValueChange) }) { Text("Paste") } }) else null)
}

@Composable
fun ChoicePicker(label: String, value: String, options: List<String>, onChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) { Text("$label: $value") }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option -> DropdownMenuItem(text = { Text(option) }, onClick = { onChange(option); expanded = false }) }
        }
    }
}

fun Context.findActivity(): Activity? {
    var value: Context = this
    while (value is ContextWrapper) { if (value is Activity) return value; value = value.baseContext }
    return value as? Activity
}
fun openUrl(context: Context, value: String) {
    val url = UrlPolicy.normalize(value)
    if (url == null) { Toast.makeText(context, "This link is not a valid HTTP(S) URL.", Toast.LENGTH_LONG).show(); return }
    try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    catch (e: Exception) { Toast.makeText(context, "No browser could open this link.", Toast.LENGTH_LONG).show() }
}
