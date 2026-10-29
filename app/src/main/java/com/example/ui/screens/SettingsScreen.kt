package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.core.model.LLMConfig
import com.example.core.model.LLMProviderType
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentConfig: LLMConfig,
    onSaveConfig: (LLMConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedProvider by remember(currentConfig) { mutableStateOf(currentConfig.provider) }
    var apiKey by remember(currentConfig) { mutableStateOf(currentConfig.apiKey) }
    var modelName by remember(currentConfig) { mutableStateOf(currentConfig.modelName) }
    var temperature by remember(currentConfig) { mutableFloatStateOf(currentConfig.temperature) }
    var maxRetries by remember(currentConfig) { mutableIntStateOf(currentConfig.maxRetries) }
    var showApiKey by remember { mutableStateOf(false) }
    var isProviderDropdownExpanded by remember { mutableStateOf(false) }
    var saveSuccessMessage by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "ENGINE & PROVIDER SETTINGS",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = "Configure autonomous intelligence backends, models, and reasoning parameters.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }

        // Provider Configuration Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(CardinalCrimsonDark, ObsidianBorder)))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "INTELLIGENCE BACKEND",
                        style = MaterialTheme.typography.labelSmall,
                        color = CardinalScarlet
                    )

                    // Provider Dropdown
                    ExposedDropdownMenuBox(
                        expanded = isProviderDropdownExpanded,
                        onExpandedChange = { isProviderDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedProvider.displayName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Active LLM Provider") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isProviderDropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CardinalScarlet,
                                unfocusedBorderColor = ObsidianBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedContainerColor = ObsidianDeep,
                                unfocusedContainerColor = ObsidianDeep
                            )
                        )

                        ExposedDropdownMenu(
                            expanded = isProviderDropdownExpanded,
                            onDismissRequest = { isProviderDropdownExpanded = false },
                            modifier = Modifier.background(ObsidianSurfaceElevated)
                        ) {
                            LLMProviderType.values().forEach { provider ->
                                DropdownMenuItem(
                                    text = { Text(provider.displayName, color = TextPrimary) },
                                    onClick = {
                                        selectedProvider = provider
                                        isProviderDropdownExpanded = false
                                        if (provider == LLMProviderType.GEMINI) {
                                            modelName = "gemini-2.0-flash"
                                        } else if (provider == LLMProviderType.OPENAI) {
                                            modelName = "gpt-4o-mini"
                                        } else if (provider == LLMProviderType.GROQ) {
                                            modelName = "llama-3.3-70b-versatile"
                                        } else if (provider == LLMProviderType.OLLAMA) {
                                            modelName = "llama3.2"
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // Model Name Input
                    OutlinedTextField(
                        value = modelName,
                        onValueChange = { modelName = it },
                        label = { Text("Model Identifier") },
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

                    // API Key Input
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        label = { Text("Provider API Key (Optional if injected via Secrets)") },
                        visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showApiKey = !showApiKey }) {
                                Icon(
                                    imageVector = if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle API Key visibility",
                                    tint = TextSecondary
                                )
                            }
                        },
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

                    // Temperature Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Temperature (Creativity vs Determinism)", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                            Text(text = "%.2f".format(temperature), style = MaterialTheme.typography.labelSmall, color = CardinalScarlet)
                        }
                        Slider(
                            value = temperature,
                            onValueChange = { temperature = it },
                            valueRange = 0.0f..1.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = CardinalScarlet,
                                activeTrackColor = CardinalCrimson,
                                inactiveTrackColor = ObsidianDeep
                            )
                        )
                    }

                    // Save Button
                    Button(
                        onClick = {
                            val newConfig = currentConfig.copy(
                                provider = selectedProvider,
                                apiKey = apiKey.trim(),
                                modelName = modelName.trim(),
                                temperature = temperature,
                                maxRetries = maxRetries
                            )
                            onSaveConfig(newConfig)
                            saveSuccessMessage = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CardinalCrimson),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("SAVE CONFIGURATION")
                    }

                    if (saveSuccessMessage) {
                        Text(
                            text = "✓ Configuration saved successfully.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = CardinalEmerald
                        )
                    }
                }
            }
        }

        // Upstream Attribution & Open-Source Heritage Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = ObsidianSurfaceElevated),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(ObsidianBorder, CardinalCrimsonDark)))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(ObsidianDeep),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Code, contentDescription = null, tint = CardinalScarlet)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "UPSTREAM HERITAGE & ATTRIBUTION",
                                style = MaterialTheme.typography.labelSmall,
                                color = CardinalScarlet
                            )
                            Text(
                                text = "Derived from OpenDroid Project",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                    }

                    Text(
                        text = "AETHEREAL CARDINAL is an independently branded, enhanced fork of OpenDroid, originally authored by Yashab Alam (yashab-cyber). It replicates and refines the core autonomous agent loop, accessibility perception architecture, and multi-tool action registry under an obsidian and cardinal crimson identity.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(ObsidianDeep)
                            .clickable {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/yashab-cyber/opendroid")).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "github.com/yashab-cyber/opendroid",
                            style = MaterialTheme.typography.bodyMedium,
                            color = CardinalGlow
                        )
                        Icon(Icons.Default.OpenInNew, contentDescription = null, tint = CardinalGlow, modifier = Modifier.size(16.dp))
                    }

                    Text(
                        text = "Licensed under the Apache License, Version 2.0. Third-party attributions and open-source licenses are fully preserved.",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }
            }
        }
    }
}
