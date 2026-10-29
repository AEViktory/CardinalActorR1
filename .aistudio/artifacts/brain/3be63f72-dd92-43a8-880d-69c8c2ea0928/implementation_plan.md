# AETHEREAL CARDINAL

AETHEREAL CARDINAL is an autonomous, on-device AI agent for Android derived from the open-source OpenDroid project. It plans multi-step device tasks, perceives the screen via dual-tier accessibility inspection and visual capture, executes system tools and application automations, and maintains persistent task memory under an obsidian and cardinal crimson identity.

---

### User Review & Critical Decisions

> [!IMPORTANT]
> The upstream OpenDroid project combines high-level LLM task planning with Android `AccessibilityService` automation and device action executors. In accordance with the project specification:
> 1. **Complete Rebranding & Independent Identity**: The application will be identified as **AETHEREAL CARDINAL**, with package namespace and application ID `com.aethereal.cardinal`, custom launcher icons, and zero-conflict manifest declarations.
> 2. **Full Functional Architecture**: The core modules (`AgentLoop`, `PlanManager`, `DualTierVisionEngine`, `ActionRegistry`, `LLMProvider` fallback chain with Gemini, Groq, OpenAI, and Ollama) are preserved and faithfully reconstructed.
> 3. **Attribution & Upstream Licensing**: Full upstream open-source attribution to Yashab Alam (`yashab-cyber/opendroid`) will be maintained in the dedicated About view and license notices.

- **Confirmed Decision 1**: Application ID and root package namespace migrate cleanly to `com.aethereal.cardinal`.
- **Confirmed Decision 2**: Visual aesthetic adopts a deep obsidian dark theme with cardinal crimson accents (`#C41E3A`, `#8B0000`, `#FF3B30`), high-contrast state badges, and real-time execution telemetry.
- **Confirmed Decision 3**: Gemini API via Google AI Studio configuration is prioritized as the primary remote cloud provider, with multi-provider fallbacks and local Ollama routing support.

---

### 1. Overview & Core Concept

- **What It Does**: AETHEREAL CARDINAL is a self-evaluating autonomous agent that accepts natural language mission requests (e.g., *"Turn on Do Not Disturb, set an alarm for 7:30 AM, and draft a message to Alex"*), breaks them down into hierarchical task trees, verifies each step's prerequisite state via accessibility inspection, and executes device tools.
- **Target Audience / Persona**: Power users, accessibility automated workflow enthusiasts, and developers seeking a sovereign, local-first on-device autonomous assistant.
- **Key Value**: Hands-free device control through reasoned planning, robust tool execution, automated replanning upon failure, and complete transparency into agent thoughts and system perception.

---

### 2. User Experience & Visual Design

#### Key User Flows
1. **Mission Dispatch & Agent Execution**:
   - The user inputs an objective or selects a quick action prompt.
   - The agent initiates planning: the visualizer dynamically animates the generated sub-task tree (Pending → In Progress → Verified / Replanned).
   - Live perception displays current screen context and reasoning log.
   - The user can pause, inspect, step-execute, or abort the mission at any time.
2. **Capability & Tool Exploration**:
   - Dedicated tools catalog displaying all registered system automations (Settings, Connectivity, Notifications, Alarms, App Launching, Device Toggles).
   - Live diagnostics showing permission status (Accessibility Service, Notification Access, System Alerts) with direct grant shortcuts.
3. **Provider & LLM Configuration**:
   - Model switcher supporting Google Gemini, OpenAI, Groq, and custom Ollama endpoints.
   - Dynamic parameter tuning (temperature, vision mode, max replan retries).
4. **Mission History & Audit**:
   - Historical task logs with step-by-step breakdown, latency, tools called, and result summaries.
5. **About & Heritage**:
   - Cardinal identity, version details, and upstream OpenDroid attribution.

#### Visual Identity & Theme
- **Aesthetic Direction**: Sovereign, high-precision dark HUD with cardinal crimson atmospheric glows, sharp typography, and telemetry micro-indicators.
- **Color Palette**:
  - `Background`: Obsidian Dark (`#0A0A0F`)
  - `Surface`: Elevated Obsidian (`#14141E`, `#1E1E2C`)
  - `Primary / Brand`: Cardinal Crimson (`#C41E3A`, `#E62E5C`)
  - `Accent / Secondary`: Radiant Scarlet (`#FF4757`)
  - `Warning / Replan`: Imperial Amber (`#F59E0B`)
  - `Success / Verified`: Emerald Matrix (`#10B981`)
  - `On-Surface Text`: Crisp Frost (`#F8FAFC`, `#94A3B8`)
- **Typography & Hierarchy**:
  - Display: Monospace / Technical Sans (`JetBrains Mono` / Medium Header) for agent reasoning and tool signatures.
  - Body: Material 3 Sans-Serif (`Roboto` / System Sans) with generous line-height and legible contrast ratios.

---

### 3. Key Product Decisions & Trade-Offs

- **Decision 1: Modular Clean Architecture (`core/agent`, `core/llm`, `core/actions`, `core/accessibility`, `core/memory`)**
  - *Chosen Approach*: Maintain strict separation between agent orchestration, LLM client interfaces, hardware action handlers, and Compose UI screens.
  - *Why*: Allows independent compilation, easy mocking for Robolectric JVM unit tests, and seamless addition of future device action executors without touching the agent loop.
  - *Alternatives Considered*: Monolithic ViewModel containing all automation logic. Rejected due to maintainability and testability concerns.

- **Decision 2: Dual-Tier Vision & Fallback Perception Engine**
  - *Chosen Approach*: Implement both Accessibility node hierarchy tree scraping and bitmap buffer capture contracts with intelligent fallback.
  - *Why*: Direct screen capture requires media projection or modern Android 11+ accessibility capture APIs; node scraping provides an instant, zero-permission-overhead structural understanding of active UI elements.

- **Decision 3: Local Persistence via Room & StateFlow**
  - *Chosen Approach*: Local Room database for mission records, step execution histories, and agent memory graphs, paired with DataStore/SharedPreferences for provider credentials.
  - *Why*: Guarantees sovereign, private on-device operation without external telemetry or cloud database lock-in.

---

### 4. Technical Architecture & Data Strategy

#### System & Component Architecture Diagram

```
┌────────────────────────────────────────────────────────────────────────┐
│                        AETHEREAL CARDINAL UI                           │
│  ┌──────────────────┬─────────────────┬─────────────────┬───────────┐  │
│  │  Mission Studio  │  Tools Catalog  │ History & Logs  │ Settings  │  │
│  └─────────┬────────┴────────┬────────┴────────┬────────┴─────┬─────┘  │
└────────────┼─────────────────┼─────────────────┼──────────────┼────────┘
             ▼                 ▼                 ▼              ▼
┌────────────────────────────────────────────────────────────────────────┐
│                       AetherealViewModel / State                        │
│   • ExecutionState (Idle, Planning, Executing, Replanning, Completed)  │
│   • TaskTree, ActiveNode, ToolResults, TelemetryStream                │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
       ┌────────────────────────────┼────────────────────────────┐
       ▼                            ▼                            ▼
┌──────────────┐             ┌──────────────┐             ┌──────────────┐
│  AgentLoop   │◄───────────►│ PlanManager  │◄───────────►│ IntentClass. │
└──────┬───────┘             └──────────────┘             └──────────────┘
       │
       ├────────────────────────────┬────────────────────────────┐
       ▼                            ▼                            ▼
┌──────────────┐             ┌──────────────┐             ┌──────────────┐
│ VisionEngine │             │ActionRegistry│             │  LLMProvider │
│  (Dual-Tier) │             │ (60+ Tools)  │             │   Gateway    │
└──────┬───────┘             └──────┬───────┘             └──────┬───────┘
       │                            │                            │
       ▼                            ▼                            ▼
┌──────────────┐             ┌──────────────┐             ┌──────────────┐
│Accessibility │             │Device Systems│             │Gemini/OpenAI │
│   Service    │             │Alarms, WiFi, │             │Groq / Ollama │
│ Node Parsing │             │Apps, Volume  │             │   Backends   │
└──────────────┘             └──────────────┘             └──────────────┘
```

#### Core Data Entities & Models
- `Mission`: `id`, `goalPrompt`, `timestamp`, `status`, `totalSteps`, `executedSteps`, `durationMs`
- `TaskPlan`: `planId`, `missionId`, `rootGoal`, `steps: List<PlanStep>`, `evaluationVerdict`
- `PlanStep`: `stepId`, `stepIndex`, `toolName`, `parameters: Map<String, Any>`, `expectedOutcome`, `status` (`PENDING`, `RUNNING`, `SUCCESS`, `FAILED`, `SKIPPED`), `resultMessage`
- `AgentMessage`: `id`, `sender` (`USER`, `CARDINAL`, `SYSTEM`, `TOOL`), `content`, `timestamp`, `toolCall`
- `ProviderConfig`: `providerType` (`GEMINI`, `OPENAI`, `GROQ`, `OLLAMA`), `apiKey`, `modelName`, `baseUrl`, `temperature`

#### Interactive Component & State Mapping
- `GoalInputField` → triggers `IntentClassifier` & `PlanManager.generatePlan()`
- `TaskStepTreeItem` → expands step inspection details, parameters, and live status
- `ActionTriggerButton` → directly fires single tools via `ActionRegistry` for validation
- `AccessibilityPermissionCard` → directs to Android system Accessibility Settings
- `ProviderSelector` → dynamically updates available models and active LLM configuration
