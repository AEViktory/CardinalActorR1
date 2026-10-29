package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.actions.ActionCategory
import com.example.core.actions.ActionResult
import com.example.core.actions.CardinalAction
import com.example.ui.theme.CardinalCrimson
import com.example.ui.theme.CardinalCrimsonDark
import com.example.ui.theme.CardinalEmerald
import com.example.ui.theme.CardinalGlow
import com.example.ui.theme.CardinalScarlet
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianDeep
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.ObsidianSurfaceHover
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ToolsCatalogScreen(
    tools: List<CardinalAction>,
    isAccessibilityActive: Boolean,
    executionResult: Pair<String, ActionResult>?,
    onExecuteTool: (String, Map<String, String>) -> Unit,
    onClearResult: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf<ActionCategory?>(null) }
    var actionToConfigure by remember { mutableStateOf<CardinalAction?>(null) }

    val filteredTools = if (selectedCategory == null) {
        tools
    } else {
        tools.filter { it.category == selectedCategory }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "ACTION CAPABILITIES CATALOG",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = "${tools.size} registered device actions spanning system, communication, gestures, and automation.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }

        // Accessibility Service Toggle Banner
        item {
            AccessibilityBanner(
                isAccessibilityActive = isAccessibilityActive,
                onOpenSettings = onOpenAccessibilitySettings
            )
        }

        // Category Filter Chips
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CategoryChip(
                    label = "All (${tools.size})",
                    isSelected = selectedCategory == null,
                    onClick = { selectedCategory = null }
                )
                ActionCategory.values().forEach { cat ->
                    val count = tools.count { it.category == cat }
                    CategoryChip(
                        label = "${cat.displayName} ($count)",
                        isSelected = selectedCategory == cat,
                        onClick = { selectedCategory = cat }
                    )
                }
            }
        }

        // Tool Cards
        items(filteredTools, key = { it.id }) { action ->
            ToolItemCard(
                action = action,
                onRunClicked = { actionToConfigure = action }
            )
        }
    }

    // Configure & Run Dialog
    if (actionToConfigure != null) {
        ToolExecutionDialog(
            action = actionToConfigure!!,
            onDismiss = { actionToConfigure = null },
            onConfirmRun = { params ->
                val id = actionToConfigure!!.id
                actionToConfigure = null
                onExecuteTool(id, params)
            }
        )
    }

    // Execution Result Alert
    if (executionResult != null) {
        val (toolId, result) = executionResult
        AlertDialog(
            onDismissRequest = onClearResult,
            containerColor = ObsidianSurfaceElevated,
            title = {
                Text(
                    text = if (result.isSuccess) "Execution Succeeded" else "Execution Failed",
                    color = if (result.isSuccess) CardinalEmerald else CardinalCrimson,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Tool: $toolId",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = result.message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextPrimary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = onClearResult,
                    colors = ButtonDefaults.buttonColors(containerColor = CardinalCrimson)
                ) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
private fun AccessibilityBanner(
    isAccessibilityActive: Boolean,
    onOpenSettings: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        shape = RoundedCornerShape(14.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.horizontalGradient(
                listOf(
                    if (isAccessibilityActive) CardinalEmerald else CardinalCrimsonDark,
                    ObsidianBorder
                )
            )
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isAccessibilityActive) CardinalEmerald.copy(alpha = 0.2f) else CardinalCrimsonDark.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isAccessibilityActive) Icons.Default.Check else Icons.Default.Settings,
                    contentDescription = null,
                    tint = if (isAccessibilityActive) CardinalEmerald else CardinalScarlet
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isAccessibilityActive) "Accessibility Engine Active" else "Accessibility Service Disabled",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Text(
                    text = if (isAccessibilityActive) "Full gesture & screen inspection enabled." else "Required for automated UI clicks and screen node scraping.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }

            if (!isAccessibilityActive) {
                OutlinedButton(
                    onClick = onOpenSettings,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Enable", color = CardinalScarlet)
                }
            }
        }
    }
}

@Composable
private fun CategoryChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) CardinalCrimson else ObsidianSurfaceHover,
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.horizontalGradient(
                listOf(
                    if (isSelected) CardinalScarlet else ObsidianBorder,
                    ObsidianBorder
                )
            )
        ),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) TextPrimary else TextSecondary,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun ToolItemCard(
    action: CardinalAction,
    onRunClicked: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(ObsidianBorder, CardinalCrimsonDark.copy(alpha = 0.3f))))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = action.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(ObsidianDeep)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = action.id,
                            style = MaterialTheme.typography.labelSmall,
                            color = CardinalGlow
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = action.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

                if (action.parameters.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Params: " + action.parameters.joinToString(", ") { "${it.name} (${it.type})" },
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onRunClicked,
                colors = ButtonDefaults.buttonColors(containerColor = ObsidianSurfaceHover),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Run", tint = CardinalScarlet, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Test", color = TextPrimary)
            }
        }
    }
}

@Composable
private fun ToolExecutionDialog(
    action: CardinalAction,
    onDismiss: () -> Unit,
    onConfirmRun: (Map<String, String>) -> Unit
) {
    val paramValues = remember {
        mutableStateMapOf<String, String>().apply {
            action.parameters.forEach { p ->
                put(p.name, p.defaultValue ?: "")
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianSurfaceElevated,
        title = {
            Text(
                text = "Run Tool: ${action.name}",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = action.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )

                if (action.parameters.isNotEmpty()) {
                    Text(
                        text = "PARAMETERS",
                        style = MaterialTheme.typography.labelSmall,
                        color = CardinalScarlet
                    )
                    action.parameters.forEach { param ->
                        OutlinedTextField(
                            value = paramValues[param.name] ?: "",
                            onValueChange = { paramValues[param.name] = it },
                            label = { Text("${param.name} (${param.description})") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CardinalScarlet,
                                unfocusedBorderColor = ObsidianBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedContainerColor = ObsidianDeep,
                                unfocusedContainerColor = ObsidianDeep
                            )
                        )
                    }
                } else {
                    Text(
                        text = "This tool executes with zero required parameters.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmRun(paramValues.toMap()) },
                colors = ButtonDefaults.buttonColors(containerColor = CardinalCrimson)
            ) {
                Text("Execute Now")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}
