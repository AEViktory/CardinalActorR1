package com.example.core.actions

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.Settings
import com.example.core.accessibility.CardinalAccessibilityService
import com.example.core.accessibility.NodeInteractionResult
import com.example.core.model.ExecutionStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class ActionExecutor(private val context: Context) {

    suspend fun execute(actionId: String, params: Map<String, String>): ActionResult = withContext(Dispatchers.IO) {
        val action = ActionRegistry.getAction(actionId)
            ?: return@withContext ActionResult(
                isSuccess = false,
                executionStatus = ExecutionStatus.EXECUTION_FAILED,
                message = "Unknown tool: $actionId"
            )

        try {
            when (actionId) {
                // System Settings Panels
                "toggle_wifi_settings" -> {
                    val intent = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                    ActionResult(
                        isSuccess = true,
                        executionStatus = ExecutionStatus.NOT_VERIFIED,
                        message = "Launched WiFi settings panel (manual user toggle required by Android OS)"
                    )
                }
                "toggle_bluetooth_settings" -> {
                    val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                    ActionResult(
                        isSuccess = true,
                        executionStatus = ExecutionStatus.NOT_VERIFIED,
                        message = "Launched Bluetooth settings panel (manual user toggle required by Android OS)"
                    )
                }
                "open_sound_settings" -> {
                    val intent = Intent(Settings.ACTION_SOUND_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                    ActionResult(
                        isSuccess = true,
                        executionStatus = ExecutionStatus.NOT_VERIFIED,
                        message = "Launched Sound & Volume configuration"
                    )
                }
                "open_display_settings" -> {
                    val intent = Intent(Settings.ACTION_DISPLAY_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                    ActionResult(
                        isSuccess = true,
                        executionStatus = ExecutionStatus.NOT_VERIFIED,
                        message = "Launched Display & Brightness settings"
                    )
                }
                "open_dnd_settings" -> {
                    val intent = Intent(Settings.ACTION_ZEN_MODE_PRIORITY_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                    ActionResult(
                        isSuccess = true,
                        executionStatus = ExecutionStatus.NOT_VERIFIED,
                        message = "Launched Do Not Disturb settings"
                    )
                }
                "open_battery_settings" -> {
                    val intent = Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                    ActionResult(
                        isSuccess = true,
                        executionStatus = ExecutionStatus.NOT_VERIFIED,
                        message = "Opened Battery Saver settings"
                    )
                }

                // Hardware & Sensors
                "toggle_flashlight" -> {
                    val enabled = params["enabled"]?.toBoolean() ?: true
                    val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
                    val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                        val chars = cameraManager.getCameraCharacteristics(id)
                        chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                    }
                    if (cameraId != null) {
                        cameraManager.setTorchMode(cameraId, enabled)
                        ActionResult(
                            isSuccess = true,
                            executionStatus = ExecutionStatus.VERIFIED,
                            message = "Flashlight torch set to $enabled",
                            evidence = "CameraManager.setTorchMode($cameraId, $enabled) succeeded"
                        )
                    } else {
                        ActionResult(
                            isSuccess = false,
                            executionStatus = ExecutionStatus.EXECUTION_FAILED,
                            message = "No camera with flashlight torch available on this device"
                        )
                    }
                }
                "vibrate_device" -> {
                    val durationMs = params["duration_ms"]?.toLongOrNull() ?: 300L
                    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                        manager?.defaultVibrator
                    } else {
                        @Suppress("DEPRECATION")
                        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                    }
                    if (vibrator != null && vibrator.hasVibrator()) {
                        val effect = VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
                        vibrator.vibrate(effect)
                        ActionResult(
                            isSuccess = true,
                            executionStatus = ExecutionStatus.VERIFIED,
                            message = "Haptic pulse triggered for ${durationMs}ms"
                        )
                    } else {
                        ActionResult(
                            isSuccess = true,
                            executionStatus = ExecutionStatus.NOT_VERIFIED,
                            message = "Haptic vibrator not present on device (simulated)"
                        )
                    }
                }
                "get_battery_status" -> {
                    val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
                    val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                    val isCharging = bm.isCharging
                    ActionResult(
                        isSuccess = true,
                        executionStatus = ExecutionStatus.VERIFIED,
                        message = "Battery level: $level%, Charging: $isCharging",
                        outputData = mapOf("level" to level, "charging" to isCharging),
                        evidence = "BatteryManager reading: level=$level, charging=$isCharging"
                    )
                }

                // Communication (with external effect / sensitive safeguards)
                "send_sms" -> {
                    val phone = params["phone_number"] ?: ""
                    val message = params["message_text"] ?: ""
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("smsto:$phone")
                        putExtra("sms_body", message)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                    ActionResult(
                        isSuccess = true,
                        executionStatus = ExecutionStatus.NOT_VERIFIED,
                        message = "Drafted SMS to '$phone'. User confirmation required in SMS app to dispatch."
                    )
                }
                "dial_phone_number" -> {
                    val phone = params["phone_number"] ?: ""
                    val intent = Intent(Intent.ACTION_DIAL).apply {
                        data = Uri.parse("tel:$phone")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                    ActionResult(
                        isSuccess = true,
                        executionStatus = ExecutionStatus.NOT_VERIFIED,
                        message = "Phone dialer opened for '$phone'"
                    )
                }
                "open_whatsapp" -> {
                    val phone = params["phone_number"]?.replace("+", "")?.replace(" ", "") ?: ""
                    val text = params["text"] ?: ""
                    val uri = if (phone.isNotBlank()) {
                        Uri.parse("https://api.whatsapp.com/send?phone=$phone&text=${Uri.encode(text)}")
                    } else {
                        Uri.parse("https://api.whatsapp.com/send?text=${Uri.encode(text)}")
                    }
                    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                    ActionResult(
                        isSuccess = true,
                        executionStatus = ExecutionStatus.NOT_VERIFIED,
                        message = "Opened WhatsApp message composer draft"
                    )
                }
                "compose_email" -> {
                    val recipient = params["recipient"] ?: ""
                    val subject = params["subject"] ?: ""
                    val body = params["body"] ?: ""
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:$recipient")
                        putExtra(Intent.EXTRA_SUBJECT, subject)
                        putExtra(Intent.EXTRA_TEXT, body)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                    ActionResult(
                        isSuccess = true,
                        executionStatus = ExecutionStatus.NOT_VERIFIED,
                        message = "Opened email composer to '$recipient'"
                    )
                }

                // Productivity & Alarms
                "set_alarm" -> {
                    val hour = params["hour"]?.toIntOrNull() ?: 7
                    val minute = params["minute"]?.toIntOrNull() ?: 30
                    val message = params["message"] ?: "Aethereal Mission"
                    val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                        putExtra(AlarmClock.EXTRA_HOUR, hour)
                        putExtra(AlarmClock.EXTRA_MINUTES, minute)
                        putExtra(AlarmClock.EXTRA_MESSAGE, message)
                        putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                    ActionResult(
                        isSuccess = true,
                        executionStatus = ExecutionStatus.NOT_VERIFIED,
                        message = "Dispatched system alarm intent for %02d:%02d ('$message')".format(hour, minute)
                    )
                }
                "set_timer" -> {
                    val seconds = params["seconds"]?.toIntOrNull() ?: 300
                    val message = params["message"] ?: "Aethereal Timer"
                    val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                        putExtra(AlarmClock.EXTRA_LENGTH, seconds)
                        putExtra(AlarmClock.EXTRA_MESSAGE, message)
                        putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                    ActionResult(
                        isSuccess = true,
                        executionStatus = ExecutionStatus.NOT_VERIFIED,
                        message = "Dispatched timer intent for ${seconds}s ('$message')"
                    )
                }
                "create_calendar_event" -> {
                    val title = params["title"] ?: "Aethereal Event"
                    val desc = params["description"] ?: ""
                    val intent = Intent(Intent.ACTION_INSERT).apply {
                        data = CalendarContract.Events.CONTENT_URI
                        putExtra(CalendarContract.Events.TITLE, title)
                        putExtra(CalendarContract.Events.DESCRIPTION, desc)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                    ActionResult(
                        isSuccess = true,
                        executionStatus = ExecutionStatus.NOT_VERIFIED,
                        message = "Dispatched calendar event intent for '$title'"
                    )
                }
                "launch_app" -> {
                    val appName = params["app_name"] ?: "Chrome"
                    val pm = context.packageManager
                    val launchIntent = resolveAppLaunchIntent(appName, pm)
                    if (launchIntent != null) {
                        context.startActivity(launchIntent)
                        ActionResult(
                            isSuccess = true,
                            executionStatus = ExecutionStatus.EXECUTION_SUCCEEDED,
                            message = "Launched app: $appName"
                        )
                    } else {
                        ActionResult(
                            isSuccess = false,
                            executionStatus = ExecutionStatus.EXECUTION_FAILED,
                            message = "App '$appName' could not be resolved on device"
                        )
                    }
                }
                "search_web" -> {
                    val query = params["query"] ?: "Android"
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}")).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                    ActionResult(
                        isSuccess = true,
                        executionStatus = ExecutionStatus.EXECUTION_SUCCEEDED,
                        message = "Searched web for '$query'"
                    )
                }

                // Automation & Accessibility Gestures
                "click_node_text" -> {
                    val target = params["target_text"] ?: ""
                    val service = CardinalAccessibilityService.instance
                    if (service != null) {
                        when (val clickResult = service.performClickOnNode(target)) {
                            is NodeInteractionResult.Success -> {
                                ActionResult(
                                    isSuccess = true,
                                    executionStatus = ExecutionStatus.EXECUTION_SUCCEEDED,
                                    message = "Successfully performed click on unique element matching '$target'"
                                )
                            }
                            is NodeInteractionResult.AmbiguousTarget -> {
                                ActionResult(
                                    isSuccess = false,
                                    executionStatus = ExecutionStatus.EXECUTION_FAILED,
                                    message = clickResult.description
                                )
                            }
                            is NodeInteractionResult.NodeNotFound -> {
                                ActionResult(
                                    isSuccess = false,
                                    executionStatus = ExecutionStatus.EXECUTION_FAILED,
                                    message = "Element matching '$target' not found in active screen hierarchy"
                                )
                            }
                            is NodeInteractionResult.DisabledOrInvisible -> {
                                ActionResult(
                                    isSuccess = false,
                                    executionStatus = ExecutionStatus.EXECUTION_FAILED,
                                    message = "Element matching '$target' exists but is disabled or invisible"
                                )
                            }
                            is NodeInteractionResult.Error -> {
                                ActionResult(
                                    isSuccess = false,
                                    executionStatus = ExecutionStatus.EXECUTION_FAILED,
                                    message = "Click failed: ${clickResult.reason}"
                                )
                            }
                        }
                    } else {
                        ActionResult(
                            isSuccess = false,
                            executionStatus = ExecutionStatus.EXECUTION_FAILED,
                            message = "Accessibility Service is not bound. Action requires active service in Android Settings."
                        )
                    }
                }
                "scroll_screen" -> {
                    val direction = params["direction"] ?: "forward"
                    val service = CardinalAccessibilityService.instance
                    if (service != null) {
                        when (val scrollRes = service.performScroll(direction == "forward")) {
                            is NodeInteractionResult.Success -> {
                                ActionResult(
                                    isSuccess = true,
                                    executionStatus = ExecutionStatus.EXECUTION_SUCCEEDED,
                                    message = "Scrolled container $direction"
                                )
                            }
                            is NodeInteractionResult.Error -> {
                                ActionResult(
                                    isSuccess = false,
                                    executionStatus = ExecutionStatus.EXECUTION_FAILED,
                                    message = "Scroll failed: ${scrollRes.reason}"
                                )
                            }
                            else -> ActionResult(isSuccess = false, executionStatus = ExecutionStatus.EXECUTION_FAILED, message = "Scroll failed")
                        }
                    } else {
                        ActionResult(
                            isSuccess = false,
                            executionStatus = ExecutionStatus.EXECUTION_FAILED,
                            message = "Accessibility Service required for scroll"
                        )
                    }
                }
                "global_back" -> {
                    val service = CardinalAccessibilityService.instance
                    if (service != null) {
                        service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
                        ActionResult(
                            isSuccess = true,
                            executionStatus = ExecutionStatus.EXECUTION_SUCCEEDED,
                            message = "Triggered global Back navigation"
                        )
                    } else {
                        ActionResult(
                            isSuccess = false,
                            executionStatus = ExecutionStatus.EXECUTION_FAILED,
                            message = "Accessibility Service not active for global Back"
                        )
                    }
                }
                "global_home" -> {
                    val service = CardinalAccessibilityService.instance
                    if (service != null) {
                        service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
                        ActionResult(
                            isSuccess = true,
                            executionStatus = ExecutionStatus.EXECUTION_SUCCEEDED,
                            message = "Navigated to Home screen via Accessibility"
                        )
                    } else {
                        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                            addCategory(Intent.CATEGORY_HOME)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(homeIntent)
                        ActionResult(
                            isSuccess = true,
                            executionStatus = ExecutionStatus.NOT_VERIFIED,
                            message = "Navigated to Home launcher via intent fallback"
                        )
                    }
                }
                "open_notifications" -> {
                    val service = CardinalAccessibilityService.instance
                    if (service != null) {
                        service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS)
                        ActionResult(
                            isSuccess = true,
                            executionStatus = ExecutionStatus.EXECUTION_SUCCEEDED,
                            message = "Opened system notification shade"
                        )
                    } else {
                        ActionResult(
                            isSuccess = false,
                            executionStatus = ExecutionStatus.EXECUTION_FAILED,
                            message = "Accessibility Service required to expand notifications"
                        )
                    }
                }
                "open_quick_settings" -> {
                    val service = CardinalAccessibilityService.instance
                    if (service != null) {
                        service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS)
                        ActionResult(
                            isSuccess = true,
                            executionStatus = ExecutionStatus.EXECUTION_SUCCEEDED,
                            message = "Opened Quick Settings shade"
                        )
                    } else {
                        ActionResult(
                            isSuccess = false,
                            executionStatus = ExecutionStatus.EXECUTION_FAILED,
                            message = "Accessibility Service required for Quick Settings"
                        )
                    }
                }
                "open_recent_apps" -> {
                    val service = CardinalAccessibilityService.instance
                    if (service != null) {
                        service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)
                        ActionResult(
                            isSuccess = true,
                            executionStatus = ExecutionStatus.EXECUTION_SUCCEEDED,
                            message = "Opened recent apps overview"
                        )
                    } else {
                        ActionResult(
                            isSuccess = false,
                            executionStatus = ExecutionStatus.EXECUTION_FAILED,
                            message = "Accessibility Service required for recent apps"
                        )
                    }
                }

                // Utilities
                "wait_delay" -> {
                    val seconds = params["seconds"]?.toIntOrNull() ?: 2
                    delay(seconds * 1000L)
                    ActionResult(
                        isSuccess = true,
                        executionStatus = ExecutionStatus.VERIFIED,
                        message = "Waited for ${seconds}s"
                    )
                }
                "inspect_screen" -> {
                    val service = CardinalAccessibilityService.instance
                    val perception = service?.inspectCurrentWindow()
                    if (perception != null && perception.isAccessibilityActive) {
                        ActionResult(
                            isSuccess = true,
                            executionStatus = ExecutionStatus.VERIFIED,
                            message = "Screen inspected: ${perception.activeAppName} (${perception.interactiveElementsCount} clickable elements)",
                            evidence = "Inspected window '${perception.screenTitle}'"
                        )
                    } else {
                        ActionResult(
                            isSuccess = false,
                            executionStatus = ExecutionStatus.EXECUTION_FAILED,
                            message = "Screen inspection failed: Accessibility service is offline"
                        )
                    }
                }

                else -> ActionResult(
                    isSuccess = false,
                    executionStatus = ExecutionStatus.EXECUTION_FAILED,
                    message = "Tool handler not registered for $actionId"
                )
            }
        } catch (e: Exception) {
            ActionResult(
                isSuccess = false,
                executionStatus = ExecutionStatus.EXECUTION_FAILED,
                message = "Execution failed: ${e.message}",
                errorDetails = e.stackTraceToString()
            )
        }
    }

    private fun resolveAppLaunchIntent(appName: String, pm: android.content.pm.PackageManager): Intent? {
        val lower = appName.lowercase().trim()

        val commonPackages = mapOf(
            "chrome" to "com.android.chrome",
            "browser" to "com.android.chrome",
            "youtube" to "com.google.android.youtube",
            "maps" to "com.google.android.apps.maps",
            "gmail" to "com.google.android.gm",
            "camera" to "com.android.camera",
            "calculator" to "com.google.android.calculator",
            "clock" to "com.google.android.deskclock",
            "whatsapp" to "com.whatsapp",
            "telegram" to "org.telegram.messenger"
        )

        val pkg = commonPackages[lower]
        if (pkg != null) {
            val intent = pm.getLaunchIntentForPackage(pkg)
            if (intent != null) return intent
        }

        val installedApps = pm.getInstalledApplications(0)
        for (app in installedApps) {
            val label = pm.getApplicationLabel(app).toString().lowercase()
            if (label.contains(lower) || lower.contains(label)) {
                val intent = pm.getLaunchIntentForPackage(app.packageName)
                if (intent != null) return intent
            }
        }
        return null
    }
}
