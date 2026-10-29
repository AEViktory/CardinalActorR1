package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.core.model.LogLevel
import com.example.core.model.LogEntry
import com.example.core.model.Mission
import com.example.core.model.MissionStatus
import com.example.core.model.PlanStep
import com.example.core.model.StepStatus
import com.example.ui.theme.CardinalAmber
import com.example.ui.theme.CardinalCrimson
import com.example.ui.theme.CardinalCrimsonDark
import com.example.ui.theme.CardinalCyan
import com.example.ui.theme.CardinalEmerald
import com.example.ui.theme.CardinalGlow
import com.example.ui.theme.CardinalPurple
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
fun MissionStudioScreen(
    activeMission: Mission?,
    logs: List<LogEntry>,
    isAccessibilityActive: Boolean,
    onDispatch: (String) -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onAbort: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var promptInput by remember { mutableStateOf("") }
    val terminalListState = rememberLazyListState()

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            terminalListState.animateScrollToItem(logs.size - 1)
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero HUD Banner
        item {
            AgentHudHeader(
                activeMission = activeMission,
                isAccessibilityActive = isAccessibilityActive,
                onOpenAccessibilitySettings = onOpenAccessibilitySettings
            )
        }

        // Mission Prompt Input Box
        item {
            MissionInputCard(
                prompt = promptInput,
                onPromptChange = { promptInput = it },
                onDispatch = {
                    if (promptInput.isNotBlank()) {
                        onDispatch(promptInput)
                        promptInput = ""
                    }
                },
                isBusy = activeMission?.status == MissionStatus.PLANNING || activeMission?.status == MissionStatus.RUNNING
            )
        }

        // Preset Prompt Chips
        item {
            PresetRoutineChips(
                onSelectPreset = { preset ->
                    promptInput = preset
                    onDispatch(preset)
                }
            )
        }

        // Active Mission Execution Card
        if (activeMission != null) {
            item {
                ActiveMissionCard(
                    mission = activeMission,
                    onPause = onPause,
                    onResume = onResume,
                    onAbort = onAbort
                )
            }

            // Steps Tree
            if (activeMission.steps.isNotEmpty()) {
                item {
                    Text(
                        text = "TASK TREE DECOMPOSITION (${activeMission.completedSteps}/${activeMission.totalSteps})",
                        style = MaterialTheme.typography.labelSmall,
                        color = CardinalScarlet,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }

                items(activeMission.steps, key = { it.id }) { step ->
                    PlanStepCard(step = step)
                }
            }
        }

        // Reasoning & Telemetry Terminal
        item {
            Text(
                text = "REASONING & TELEMETRY TERMINAL",
                style = MaterialTheme.typography.labelSmall,
                color = CardinalScarlet,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )
            TelemetryTerminalCard(
                logs = logs,
                listState = terminalListState
            )
        }
    }
}

@Composable
private fun AgentHudHeader(
    activeMission: Mission?,
    isAccessibilityActive: Boolean,
    onOpenAccessibilitySettings: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(CardinalCrimsonDark, ObsidianBorder)))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Crest Icon with glowing ring
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(ObsidianDeep)
                        .border(1.5.dp, CardinalCrimson, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_cardinal_logo),
                        contentDescription = "Aethereal Cardinal Crest",
                        modifier = Modifier.size(42.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "AETHEREAL CARDINAL",
                        style = MaterialTheme.typography.titleLarge.copy(letterSpacing = 1.sp),
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Autonomous On-Device Agent",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }

                // Status Badge
                StatusPill(status = activeMission?.status ?: MissionStatus.IDLE)
            }

            // Accessibility Warning if inactive
            if (!isAccessibilityActive) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(CardinalCrimsonDark.copy(alpha = 0.35f))
                        .border(1.dp, CardinalScarlet.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .clickable { onOpenAccessibilitySettings() }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Warning",
                        tint = CardinalAmber,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Accessibility Service is offline. Tap to activate screen perception.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusPill(status: MissionStatus) {
    val (color, text) = when (status) {
        MissionStatus.IDLE -> ObsidianBorder to "STANDBY"
        MissionStatus.PLANNING -> CardinalAmber to "PLANNING"
        MissionStatus.RUNNING -> CardinalScarlet to "EXECUTING"
        MissionStatus.PAUSED -> CardinalAmber to "PAUSED"
        MissionStatus.COMPLETED -> CardinalEmerald to "COMPLETED"
        MissionStatus.FAILED -> CardinalCrimson to "FAILED"
        MissionStatus.ABORTED -> TextMuted to "ABORTED"
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.2f))
            .border(1.dp, color, RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun MissionInputCard(
    prompt: String,
    onPromptChange: (String) -> Unit,
    onDispatch: () -> Unit,
    isBusy: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurfaceElevated),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(ObsidianBorder, CardinalCrimsonDark)))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "MISSION OBJECTIVE",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = prompt,
                onValueChange = onPromptChange,
                placeholder = {
                    Text(
                        text = "e.g., Turn on Do Not Disturb, set morning alarm for 7:30, and check battery status...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("mission_prompt_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CardinalScarlet,
                    unfocusedBorderColor = ObsidianBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedContainerColor = ObsidianDeep,
                    unfocusedContainerColor = ObsidianDeep
                ),
                shape = RoundedCornerShape(12.dp),
                maxLines = 4
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onDispatch,
                enabled = prompt.isNotBlank() && !isBusy,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("dispatch_mission_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CardinalCrimson,
                    disabledContainerColor = ObsidianSurfaceHover
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isBusy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = TextPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Agent Processing...")
                } else {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Dispatch",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "DISPATCH MISSION",
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun PresetRoutineChips(
    onSelectPreset: (String) -> Unit
) {
    val presets = listOf(
        "Night Routine: DND on, set alarm for 07:30, torch off",
        "Deep Focus: Enable DND and start 45 min focus timer",
        "Battery Check: Query device power status and battery saver",
        "Open WhatsApp to message",
        "Launch YouTube and search Android news"
    )

    Column {
        Text(
            text = "QUICK MISSION DISPATCH",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            presets.forEach { preset ->
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = ObsidianSurfaceHover,
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(ObsidianBorder, CardinalCrimsonDark.copy(alpha = 0.5f)))),
                    modifier = Modifier
                        .clickable { onSelectPreset(preset) }
                        .padding(vertical = 2.dp)
                ) {
                    Text(
                        text = preset,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ActiveMissionCard(
    mission: Mission,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onAbort: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurfaceElevated),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(CardinalCrimson, CardinalGlow)))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "ACTIVE MISSION",
                        style = MaterialTheme.typography.labelSmall,
                        color = CardinalScarlet
                    )
                    Text(
                        text = mission.goalPrompt,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        maxLines = 2
                    )
                }

                // Control Buttons
                Row {
                    if (mission.status == MissionStatus.RUNNING) {
                        IconButton(
                            onClick = onPause,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(ObsidianDeep)
                        ) {
                            Icon(Icons.Default.Pause, contentDescription = "Pause", tint = CardinalAmber)
                        }
                    } else if (mission.status == MissionStatus.PAUSED) {
                        IconButton(
                            onClick = onResume,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(ObsidianDeep)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Resume", tint = CardinalEmerald)
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    if (mission.status == MissionStatus.RUNNING || mission.status == MissionStatus.PAUSED || mission.status == MissionStatus.PLANNING) {
                        IconButton(
                            onClick = onAbort,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(ObsidianDeep)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = "Abort", tint = CardinalCrimson)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress Bar
            LinearProgressIndicator(
                progress = { mission.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = CardinalScarlet,
                trackColor = ObsidianDeep
            )

            if (mission.summary != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = mission.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = CardinalEmerald
                )
            }
        }
    }
}

@Composable
private fun PlanStepCard(step: PlanStep) {
    val (statusColor, statusIcon) = when (step.status) {
        StepStatus.PENDING -> TextMuted to Icons.Default.HourglassTop
        StepStatus.VALIDATING -> CardinalCyan to Icons.Default.HourglassTop
        StepStatus.RUNNING -> CardinalAmber to Icons.Default.Refresh
        StepStatus.VERIFYING -> CardinalAmber to Icons.Default.HourglassTop
        StepStatus.SUCCESS -> {
            if (step.executionStatus == com.example.core.model.ExecutionStatus.NOT_VERIFIED) {
                CardinalAmber to Icons.Default.CheckCircle
            } else {
                CardinalEmerald to Icons.Default.CheckCircle
            }
        }
        StepStatus.FAILED -> CardinalCrimson to Icons.Default.Close
        StepStatus.REPLANNED -> CardinalCyan to Icons.Default.Refresh
        StepStatus.SKIPPED -> TextMuted to Icons.Default.Close
        StepStatus.REJECTED -> CardinalCrimson to Icons.Default.Close
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        shape = RoundedCornerShape(12.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(ObsidianBorder, statusColor.copy(alpha = 0.5f))))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Step Number Badge
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(statusColor.copy(alpha = 0.15f))
                    .border(1.dp, statusColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${step.stepIndex}",
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = step.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "[${step.toolName}]",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }

                if (step.description.isNotBlank()) {
                    Text(
                        text = step.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                if (step.executionStatus != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Status: ${step.executionStatus.displayName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (step.executionStatus == com.example.core.model.ExecutionStatus.VERIFIED) CardinalEmerald
                                else if (step.executionStatus == com.example.core.model.ExecutionStatus.NOT_VERIFIED) CardinalAmber
                                else CardinalCrimson,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (step.result != null) {
                    Text(
                        text = "Result: ${step.result}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (step.status == StepStatus.SUCCESS) CardinalEmerald else CardinalCrimson,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            Icon(
                imageVector = statusIcon,
                contentDescription = null,
                tint = statusColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun TelemetryTerminalCard(
    logs: List<LogEntry>,
    listState: androidx.compose.foundation.lazy.LazyListState
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianDeep),
        shape = RoundedCornerShape(14.dp),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(ObsidianBorder, CardinalCrimsonDark.copy(alpha = 0.4f))))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CONSOLE STREAM (${logs.size} EVENTS)",
                    style = MaterialTheme.typography.labelSmall,
                    color = CardinalScarlet
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(CardinalEmerald)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (logs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Awaiting mission execution...\nDispatch a prompt above to view telemetry stream.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(logs) { entry ->
                        val tagColor = when (entry.level) {
                            LogLevel.INFO -> CardinalCyan
                            LogLevel.PLAN -> CardinalAmber
                            LogLevel.EXEC -> CardinalGlow
                            LogLevel.PERCEIVE -> CardinalPurple
                            LogLevel.WARN -> CardinalAmber
                            LogLevel.ERROR -> CardinalCrimson
                        }

                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "[${entry.tag}]",
                                style = MaterialTheme.typography.labelSmall,
                                color = tagColor,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(72.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = entry.message,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextPrimary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}
