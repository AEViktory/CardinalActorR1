package com.example.core.actions

object ActionRegistry {

    val allActions: List<CardinalAction> = listOf(
        // System & Settings
        CardinalAction(
            id = "toggle_wifi_settings",
            name = "WiFi Settings",
            category = ActionCategory.SYSTEM,
            riskLevel = RiskLevel.REVERSIBLE,
            description = "Launch Android WiFi and network configuration panel.",
            expectedPostcondition = "WiFi settings activity presented to user"
        ),
        CardinalAction(
            id = "toggle_bluetooth_settings",
            name = "Bluetooth Settings",
            category = ActionCategory.SYSTEM,
            riskLevel = RiskLevel.REVERSIBLE,
            description = "Launch Android Bluetooth pairing and device settings.",
            expectedPostcondition = "Bluetooth settings activity presented to user"
        ),
        CardinalAction(
            id = "open_sound_settings",
            name = "Sound & Volume Settings",
            category = ActionCategory.SYSTEM,
            riskLevel = RiskLevel.REVERSIBLE,
            description = "Open device audio and volume control settings.",
            expectedPostcondition = "Sound settings panel launched"
        ),
        CardinalAction(
            id = "open_display_settings",
            name = "Display & Brightness",
            category = ActionCategory.SYSTEM,
            riskLevel = RiskLevel.REVERSIBLE,
            description = "Open screen brightness and dark mode display settings.",
            expectedPostcondition = "Display settings panel launched"
        ),
        CardinalAction(
            id = "open_dnd_settings",
            name = "Do Not Disturb Settings",
            category = ActionCategory.SYSTEM,
            riskLevel = RiskLevel.REVERSIBLE,
            description = "Open Zen mode and Do Not Disturb notification priority rules.",
            expectedPostcondition = "Zen mode / DND settings launched"
        ),
        CardinalAction(
            id = "open_battery_settings",
            name = "Battery & Power Saver",
            category = ActionCategory.SYSTEM,
            riskLevel = RiskLevel.READ_ONLY,
            description = "Inspect battery health and toggle battery saver mode.",
            expectedPostcondition = "Battery saver screen displayed"
        ),

        // Hardware & Sensors
        CardinalAction(
            id = "toggle_flashlight",
            name = "Torch / Flashlight",
            category = ActionCategory.HARDWARE,
            riskLevel = RiskLevel.REVERSIBLE,
            description = "Turn on or off the device camera LED flashlight torch.",
            parameters = listOf(
                ActionParameter(
                    name = "enabled",
                    type = "boolean",
                    description = "True to turn on, False to turn off",
                    defaultValue = "true",
                    allowedValues = listOf("true", "false")
                )
            ),
            expectedPostcondition = "CameraManager torch mode updated"
        ),
        CardinalAction(
            id = "vibrate_device",
            name = "Haptic Vibration Pulse",
            category = ActionCategory.HARDWARE,
            riskLevel = RiskLevel.REVERSIBLE,
            description = "Trigger a haptic vibration pulse pattern on device.",
            parameters = listOf(
                ActionParameter("duration_ms", "number", "Duration in milliseconds", defaultValue = "300")
            ),
            expectedPostcondition = "Haptic actuator triggered"
        ),
        CardinalAction(
            id = "get_battery_status",
            name = "Battery Telemetry",
            category = ActionCategory.HARDWARE,
            riskLevel = RiskLevel.READ_ONLY,
            description = "Query current battery percentage, temperature, and charging state.",
            expectedPostcondition = "Battery percentage and charging status read"
        ),

        // Communication
        CardinalAction(
            id = "send_sms",
            name = "Draft / Send SMS",
            category = ActionCategory.COMMUNICATION,
            riskLevel = RiskLevel.SENSITIVE,
            description = "Compose an SMS message to a phone number.",
            parameters = listOf(
                ActionParameter("phone_number", "string", "Recipient phone number", maxLength = 30),
                ActionParameter("message_text", "string", "Content of the message", maxLength = 500)
            ),
            expectedPostcondition = "SMS compose intent launched for user review"
        ),
        CardinalAction(
            id = "dial_phone_number",
            name = "Dial Phone Number",
            category = ActionCategory.COMMUNICATION,
            riskLevel = RiskLevel.SENSITIVE,
            description = "Open phone dialer with specified number.",
            parameters = listOf(
                ActionParameter("phone_number", "string", "Phone number to dial", maxLength = 30)
            ),
            expectedPostcondition = "System dialer presented"
        ),
        CardinalAction(
            id = "open_whatsapp",
            name = "WhatsApp Message",
            category = ActionCategory.COMMUNICATION,
            riskLevel = RiskLevel.SENSITIVE,
            description = "Open WhatsApp chat draft to specified phone number or contact.",
            parameters = listOf(
                ActionParameter("phone_number", "string", "Phone number with country code", maxLength = 30),
                ActionParameter("text", "string", "Message to prefill", isRequired = false, maxLength = 500)
            ),
            expectedPostcondition = "WhatsApp conversation draft opened"
        ),
        CardinalAction(
            id = "compose_email",
            name = "Compose Email",
            category = ActionCategory.COMMUNICATION,
            riskLevel = RiskLevel.SENSITIVE,
            description = "Open email client with prefilled recipient and subject.",
            parameters = listOf(
                ActionParameter("recipient", "string", "Recipient email address", maxLength = 100),
                ActionParameter("subject", "string", "Email subject line", maxLength = 200),
                ActionParameter("body", "string", "Email body content", isRequired = false, maxLength = 1000)
            ),
            expectedPostcondition = "Email client opened with draft"
        ),

        // Productivity & Alarms
        CardinalAction(
            id = "set_alarm",
            name = "Set System Alarm",
            category = ActionCategory.PRODUCTIVITY,
            riskLevel = RiskLevel.EXTERNAL_EFFECT,
            description = "Schedule a device alarm clock at designated hour and minute.",
            parameters = listOf(
                ActionParameter("hour", "number", "Hour (0-23)"),
                ActionParameter("minute", "number", "Minute (0-59)"),
                ActionParameter("message", "string", "Alarm label/title", isRequired = false, defaultValue = "Aethereal Mission")
            ),
            expectedPostcondition = "Alarm intent dispatched to Android Clock"
        ),
        CardinalAction(
            id = "set_timer",
            name = "Set Countdown Timer",
            category = ActionCategory.PRODUCTIVITY,
            riskLevel = RiskLevel.EXTERNAL_EFFECT,
            description = "Start a countdown timer for designated seconds.",
            parameters = listOf(
                ActionParameter("seconds", "number", "Duration in seconds", defaultValue = "300"),
                ActionParameter("message", "string", "Timer label", isRequired = false, defaultValue = "Aethereal Timer")
            ),
            expectedPostcondition = "Timer intent dispatched to Android Clock"
        ),
        CardinalAction(
            id = "create_calendar_event",
            name = "Calendar Event",
            category = ActionCategory.PRODUCTIVITY,
            riskLevel = RiskLevel.EXTERNAL_EFFECT,
            description = "Add an event or reminder to the system calendar.",
            parameters = listOf(
                ActionParameter("title", "string", "Event title", maxLength = 200),
                ActionParameter("description", "string", "Event description", isRequired = false, maxLength = 500)
            ),
            expectedPostcondition = "Calendar event intent dispatched"
        ),
        CardinalAction(
            id = "launch_app",
            name = "Launch App",
            category = ActionCategory.PRODUCTIVITY,
            riskLevel = RiskLevel.REVERSIBLE,
            description = "Open an installed application by package name or common app keyword.",
            parameters = listOf(
                ActionParameter("app_name", "string", "Application name (e.g. YouTube, Maps, Chrome, Camera)", maxLength = 100)
            ),
            expectedPostcondition = "Foreground activity transferred to target application"
        ),
        CardinalAction(
            id = "search_web",
            name = "Web Search",
            category = ActionCategory.PRODUCTIVITY,
            riskLevel = RiskLevel.READ_ONLY,
            description = "Launch browser with targeted web search query.",
            parameters = listOf(
                ActionParameter("query", "string", "Search keywords or question", maxLength = 300)
            ),
            expectedPostcondition = "Browser query activity displayed"
        ),

        // Automation & Accessibility Gestures
        CardinalAction(
            id = "click_node_text",
            name = "Click Element by Text or ID",
            category = ActionCategory.AUTOMATION,
            riskLevel = RiskLevel.REVERSIBLE,
            description = "Perform a verified accessibility click on a unique, visible, and enabled UI element.",
            parameters = listOf(
                ActionParameter("target_text", "string", "Exact text label or View Resource ID to click", maxLength = 150)
            ),
            expectedPostcondition = "UI state transition observed on screen"
        ),
        CardinalAction(
            id = "scroll_screen",
            name = "Scroll Screen Container",
            category = ActionCategory.AUTOMATION,
            riskLevel = RiskLevel.REVERSIBLE,
            description = "Scroll the active scrollable container forward or backward.",
            parameters = listOf(
                ActionParameter(
                    name = "direction",
                    type = "string",
                    description = "forward or backward",
                    defaultValue = "forward",
                    allowedValues = listOf("forward", "backward")
                )
            ),
            expectedPostcondition = "Scroll container offset updated"
        ),
        CardinalAction(
            id = "global_back",
            name = "System Back Button",
            category = ActionCategory.AUTOMATION,
            riskLevel = RiskLevel.REVERSIBLE,
            description = "Trigger the global Android Back navigation action.",
            expectedPostcondition = "Back key event handled"
        ),
        CardinalAction(
            id = "global_home",
            name = "System Home Button",
            category = ActionCategory.AUTOMATION,
            riskLevel = RiskLevel.REVERSIBLE,
            description = "Navigate immediately to Android Home launcher.",
            expectedPostcondition = "Home launcher activity active"
        ),
        CardinalAction(
            id = "open_notifications",
            name = "Expand Notifications",
            category = ActionCategory.AUTOMATION,
            riskLevel = RiskLevel.REVERSIBLE,
            description = "Pull down the system notification shade.",
            expectedPostcondition = "Notification shade opened"
        ),
        CardinalAction(
            id = "open_quick_settings",
            name = "Quick Settings Shade",
            category = ActionCategory.AUTOMATION,
            riskLevel = RiskLevel.REVERSIBLE,
            description = "Pull down the system Quick Settings toggles panel.",
            expectedPostcondition = "Quick settings opened"
        ),
        CardinalAction(
            id = "open_recent_apps",
            name = "Recent Apps Overview",
            category = ActionCategory.AUTOMATION,
            riskLevel = RiskLevel.REVERSIBLE,
            description = "Open the system multitasking overview switcher.",
            expectedPostcondition = "Recents overview active"
        ),

        // Utilities
        CardinalAction(
            id = "wait_delay",
            name = "Execution Wait Delay",
            category = ActionCategory.UTILITY,
            riskLevel = RiskLevel.READ_ONLY,
            description = "Pause execution for a brief duration to let screens settle.",
            parameters = listOf(
                ActionParameter("seconds", "number", "Delay duration in seconds (1-10)", defaultValue = "2")
            ),
            expectedPostcondition = "Settling delay elapsed"
        ),
        CardinalAction(
            id = "inspect_screen",
            name = "Perceive Screen State",
            category = ActionCategory.UTILITY,
            riskLevel = RiskLevel.READ_ONLY,
            description = "Capture and analyze current active window hierarchy.",
            expectedPostcondition = "Active window elements inspected"
        )
    )

    fun getAction(id: String): CardinalAction? = allActions.find { it.id == id }
}
