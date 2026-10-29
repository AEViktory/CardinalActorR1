# Decisions & Provenance Audit

## 1. Upstream Provenance & Historical Context

- **Source Repository:** `https://github.com/yashab-cyber/opendroid`
- **Original Project Name:** OpenDroid
- **Author:** Yashab Alam (`yashab-cyber`)
- **License:** Apache License 2.0
- **Classification:** **Rebranded, Stabilized, and Hardened Fork.** The project faithfully adapts OpenDroid's core autonomous agent architecture (planning, accessibility perception, device action registry, and multi-provider fallback) while introducing rigorous runtime safety, deterministic plan validation, Room database persistence, and bounded recovery budgets.

---

## 2. Key Architectural Decisions & Rationale

### Decision 1: Deterministic Plan Validation Gate (`PlanValidator.kt`)
- **Context:** LLM generated plans are non-deterministic and can produce unregistered tools, malformed arguments, or oversized parameter values.
- **Decision:** All plans must pass through `PlanValidator` before execution. Any plan containing unregistered tools or violating schema types is rejected prior to touching Android system APIs.

### Decision 2: Distinct Postcondition Verification (`ResultVerifier.kt`)
- **Context:** An action executing without an exception (e.g., launching WiFi settings) does not mean the user's objective (turning on WiFi) was verified.
- **Decision:** Split execution outcomes into `VERIFIED`, `NOT_VERIFIED`, and `EXECUTION_FAILED`. Android sandboxing limits are explicitly reported to the user rather than falsely claiming guaranteed completion.

### Decision 3: Suspendable Pause Gate and Cooperative Cancellation
- **Context:** OpenDroid and previous templates relied on busy-wait `delay(300)` loops and boolean flags for pausing, and could swallow `CancellationException`.
- **Decision:** Implement `CompletableDeferred` pause gates that suspend coroutines cooperatively without CPU spin. Catch `CancellationException` solely to update state to `ABORTED` in Room and immediately rethrow.

### Decision 4: Semantic Accessibility Disambiguation
- **Context:** If multiple UI elements share the same text, picking the first match arbitrarily causes erratic clicks.
- **Decision:** Check for unique matches. If multiple elements match identically, return `AMBIGUOUS_TARGET` and decline to click until a unique identifier or qualifier is supplied.

### Decision 5: Room Database Persistence with Legacy Migration
- **Context:** Persisting missions in SharedPreferences JSON lacks relational integrity, step indexing, and query capability.
- **Decision:** Implement Room database (`CardinalDatabase`) with `MissionDao`, `StepDao`, and `EventDao`. Migrate legacy JSON data idempotently on first startup.

### Decision 6: Package Identity & Cloud Build Environment Alignment
- **Context:** The cloud-based AI Studio environment enforces `namespace = "com.example"` for internal generated R classes, while allowing independent `applicationId = "com.aethereal.cardinal"`.
- **Decision:** Set `applicationId = "com.aethereal.cardinal"` in `app/build.gradle.kts` and define public brand identity `AETHEREAL CARDINAL` in launcher and strings resources to achieve an independent installation identity without breaking container compiler paths.
