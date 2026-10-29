# AETHEREAL CARDINAL - Tool & Action Catalog

Every tool registered in `ActionRegistry.kt` undergoes parameter validation, execution timeout handling, and postcondition verification. The registry declares **25 distinct, production-grade tools** across 6 operational categories.

---

## 1. System & Settings (6 Tools)

| Tool ID | Name | Risk Level | Description | Parameters | Expected Postcondition |
|---|---|---|---|---|---|
| `toggle_wifi_settings` | WiFi Settings | `REVERSIBLE` | Launches Android WiFi and network configuration panel. | None | WiFi settings activity presented to user |
| `toggle_bluetooth_settings` | Bluetooth Settings | `REVERSIBLE` | Launches Android Bluetooth pairing and device settings. | None | Bluetooth settings activity presented to user |
| `open_sound_settings` | Sound & Volume Settings | `REVERSIBLE` | Opens device audio and volume control settings. | None | Sound settings panel launched |
| `open_display_settings` | Display & Brightness | `REVERSIBLE` | Opens screen brightness and dark mode display settings. | None | Display settings panel launched |
| `open_dnd_settings` | Do Not Disturb Settings | `REVERSIBLE` | Opens Zen mode and Do Not Disturb notification priority rules. | None | Zen mode / DND settings launched |
| `open_battery_settings` | Battery & Power Saver | `READ_ONLY` | Inspects battery health and opens battery saver mode. | None | Battery saver screen displayed |

*Notice: Android OS security sandboxing restricts non-system third-party apps from silently toggling system radios. AETHEREAL CARDINAL faithfully launches the system settings intent and marks the result `NOT_VERIFIED` to transparently reflect sandbox boundaries.*

---

## 2. Hardware & Sensors (3 Tools)

| Tool ID | Name | Risk Level | Description | Parameters | Expected Postcondition |
|---|---|---|---|---|---|
| `toggle_flashlight` | Torch / Flashlight | `REVERSIBLE` | Toggles rear camera LED torch via CameraManager. | `enabled` (boolean, default: true) | Flashlight state updated |
| `get_battery_status` | Battery Telemetry | `READ_ONLY` | Reads current battery percentage, charging state, and temperature. | None | Battery telemetry read |
| `vibrate_device` | Haptic Pulse | `REVERSIBLE` | Triggers device haptic feedback pulse via Vibrator. | `duration_ms` (number, default: 300) | Haptic vibration executed |

---

## 3. Communication & Messaging (4 Tools)

| Tool ID | Name | Risk Level | Description | Parameters | Expected Postcondition |
|---|---|---|---|---|---|
| `send_sms` | Send SMS Text | `SENSITIVE` | Prepares and drafts SMS message. Non-idempotent. | `phone_number` (string, req), `message` (string, req) | SMS draft intent dispatched |
| `dial_phone_number` | Dial Phone Number | `SENSITIVE` | Pre-fills phone dialer with telephone number. Non-idempotent. | `phone_number` (string, req) | Dialer intent dispatched |
| `open_whatsapp` | WhatsApp Chat | `SENSITIVE` | Opens WhatsApp conversation with pre-filled text. Non-idempotent. | `phone_number` (string, req), `message` (string, opt) | WhatsApp conversation opened |
| `open_url` | Open Web URL | `READ_ONLY` | Opens specified HTTP/HTTPS web link in default browser. | `url` (string, req) | Web browser opened |

---

## 4. Productivity & Applications (5 Tools)

| Tool ID | Name | Risk Level | Description | Parameters | Expected Postcondition |
|---|---|---|---|---|---|
| `set_alarm` | Create System Alarm | `EXTERNAL_EFFECT` | Sets a device alarm clock. Non-idempotent. | `hour` (number, req), `minute` (number, req), `message` (string, opt) | Alarm scheduled in Clock app |
| `set_timer` | Start Countdown Timer | `EXTERNAL_EFFECT` | Starts a system countdown timer. Non-idempotent. | `seconds` (number, req), `message` (string, opt) | Timer started in Clock app |
| `create_calendar_event` | Insert Calendar Event | `EXTERNAL_EFFECT` | Pre-fills calendar event with start/end timestamps. Non-idempotent. | `title` (string, req), `description` (string, opt), `begin_time_ms` (number, opt) | Calendar insert intent dispatched |
| `launch_app` | Launch Application | `REVERSIBLE` | Launches an installed application by package name or common app name. | `app_name` (string, req) | Target application in foreground |
| `take_screenshot` | Capture Screen State | `READ_ONLY` | Requests accessibility screenshot capture. | None | Screen captured |

---

## 5. Accessibility & UI Gestures (5 Tools)

| Tool ID | Name | Risk Level | Description | Parameters | Semantic Disambiguation Rules |
|---|---|---|---|---|---|
| `click_node_text` | Click UI Element | `REVERSIBLE` | Clicks clickable UI element matching label, description, or view ID. | `target_text` (string, req) | Prioritizes View ID > exact text > contentDescription. Rejects ambiguous matches. |
| `scroll_forward` | Scroll Screen Forward | `REVERSIBLE` | Performs forward vertical scroll gesture on scrollable container. | None | Scroll gesture performed on scrollable node |
| `press_home` | Navigate Home | `REVERSIBLE` | Navigates to the Android launcher home screen. | None | Home navigation performed |
| `pull_quick_settings` | Quick Settings Shade | `REVERSIBLE` | Pulls down system Quick Settings toggles panel. | None | Quick settings opened |
| `open_recent_apps` | Recent Apps Overview | `REVERSIBLE` | Opens system multitasking overview switcher. | None | Recents overview active |

---

## 6. Utilities & Execution Flow (2 Tools)

| Tool ID | Name | Risk Level | Description | Parameters | Expected Postcondition |
|---|---|---|---|---|---|
| `wait_delay` | Execution Wait Delay | `READ_ONLY` | Pauses execution for a brief duration to let screens settle. | `seconds` (number, default: 2) | Settling delay elapsed |
| `inspect_screen` | Perceive Screen State | `READ_ONLY` | Captures and analyzes current active window hierarchy. | None | Active window elements inspected |

---

## Safety and Execution Principles

1. **Parameter Validation (`PlanValidator`):** All 25 tools are checked for required parameters, types, string length bounds, and allowed values. Plans with invalid parameters are rejected before execution.
2. **Idempotency Safeguards:** Non-idempotent actions (`send_sms`, `dial_phone_number`, `set_alarm`, etc.) and uncertain timeouts (`TIMEOUT_UNCERTAIN_EFFECT`) cannot be retried directly.
3. **Rigorous Verification (`ResultVerifier`):** External intent actions are transparently categorized as `NOT_VERIFIED`. Direct actions (`launch_app`, `click_node_text`, `get_battery_status`) are evaluated against screen node transitions and expected outcomes.
