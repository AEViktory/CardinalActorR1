# AETHEREAL CARDINAL

**AETHEREAL CARDINAL** is an on-device autonomous Android agent directly derived and stabilized from the open-source **OpenDroid** architecture. It provides disciplined, self-evaluating device control, accessibility-driven UI perception, a hardened 25+ tool action registry, and multi-provider intelligence backends (Google Gemini API & deterministic offline execution) under an obsidian and cardinal crimson identity.

---

## 1. Provenance & Attribution

- **Upstream Project:** [OpenDroid (`yashab-cyber/opendroid`)](https://github.com/yashab-cyber/opendroid)
- **Original Author:** Yashab Alam (`yashab-cyber`)
- **License:** Apache License 2.0
- **Classification:** Rebranded, stabilized, and hardened fork with enhanced validation, postcondition verification, bounded replanning, and Room database persistence.

---

## 2. Verified Build Environment & Prerequisites

- **Java Development Kit (JDK):** JDK 17+ (JDK 21 compatible)
- **Gradle:** Gradle 8.13+ (Gradle Wrapper included via `gradlew` / `gradlew.bat`)
- **Android Gradle Plugin (AGP):** 9.1.1
- **Kotlin:** 2.2.10
- **Compile SDK:** Android 36 (Android 15+)
- **Target SDK:** Android 36
- **Min SDK:** Android 24 (Android 7.0+)

---

## 3. Build & Test Commands

Build the debug APK using the Gradle wrapper (or container `gradle`):

```bash
# Verify Gradle wrapper
./gradlew --version

# Assemble debug APK
./gradlew :app:assembleDebug

# Run unit and Robolectric tests
./gradlew :app:testDebugUnitTest
```

---

## 4. Architecture & Key Modules

AETHEREAL CARDINAL enforces strict separation between model suggestions and runtime authority:

`OBSERVE → PLAN → VALIDATE PLAN → VALIDATE ACTION → EXECUTE → VERIFY → UPDATE STATE → CONTINUE / REPLAN / FAIL`

- **`core/agent`**: `AgentLoop` state machine, `PlanManager`, `PlanValidator`, and `ResultVerifier`.
- **`core/actions`**: `ActionRegistry` (25+ audited tools with RiskLevels) and `ActionExecutor`.
- **`core/accessibility`**: `CardinalAccessibilityService` with bounded traversal (1000 nodes, depth 30) and semantic disambiguation.
- **`core/perception`**: `VisionEngine` truthful perception provider (never produces synthetic mock data).
- **`core/llm`**: `GeminiProvider` with classified error taxonomy and `DeterministicPlanner` transparent fallback.
- **`core/database`**: `CardinalDatabase` (Room) with `MissionDao`, `StepDao`, and `EventDao`.
- **`ui`**: Jetpack Compose obsidian/cardinal dark HUD with real-time telemetry stream, tools browser, vision inspector, archive, and settings.

---

## 5. Security & Sensitive Capabilities

- **API Keys:** Never hardcoded or logged. Read at runtime from user settings or injected via secrets (`BuildConfig.GEMINI_API_KEY`).
- **Sensitive Tools:** Actions with external device effects (e.g., SMS, phone dialer, WhatsApp) are classified as `RiskLevel.SENSITIVE` and require explicit user review.
- **Accessibility Service:** Operates on-device only without remote telemetry or external credential leakage.

For detailed documentation, see:
- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md)
- [`docs/TOOL_CATALOG.md`](docs/TOOL_CATALOG.md)
- [`docs/TEST_PLAN.md`](docs/TEST_PLAN.md)
- [`docs/DECISIONS_AND_AUDIT.md`](docs/DECISIONS_AND_AUDIT.md)
