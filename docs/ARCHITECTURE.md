# System Architecture & Runtime Design

AETHEREAL CARDINAL is an on-device autonomous Android agent engineered around disciplined state machines, bounded execution budgets, and deterministic validation gates.

---

## 1. Core Operating Cycle

Every action executed by the agent strictly traverses the following sequence:

```
┌───────────┐     ┌──────────┐     ┌──────────────┐     ┌─────────────┐
│  OBSERVE  │ ──► │   PLAN   │ ──► │ VALIDATE PLAN│ ──► │ POLICY GATE │
└───────────┘     └──────────┘     └──────────────┘     └─────────────┘
                                                               │
                                                               ▼
┌───────────┐     ┌──────────┐     ┌──────────────┐     ┌─────────────┐
│CONTINUE / │ ◄── │  UPDATE  │ ◄── │    VERIFY    │ ◄── │   EXECUTE   │
│  REPLAN   │     │  STATE   │     │ POSTCONDITION│     │   ACTION    │
└───────────┘     └──────────┘     └──────────────┘     └─────────────┘
```

1. **Observe:** `VisionEngine` captures a snapshot of current device state via `CardinalAccessibilityService`.
2. **Plan:** The LLM client (`GeminiProvider` or deterministic fallback) generates candidate task steps.
3. **Validate Plan:** `PlanValidator` rejects unregistered tools, invalid parameter types, missing required keys, or excess step counts.
4. **Policy Gate:** Tools marked `RiskLevel.SENSITIVE` require user confirmation or explicit authorization.
5. **Execute Action:** `ActionExecutor` triggers the validated tool with execution timeout and cancellation support.
6. **Verify Postcondition:** `ResultVerifier` inspects device telemetry or UI changes, classifying outcomes into `VERIFIED`, `NOT_VERIFIED`, or `EXECUTION_FAILED`.
7. **Update State:** State is committed to memory (`StateFlow`) and persisted into Room database.
8. **Continue / Replan / Fail:** If a step fails, `PlanManager` attempts bounded replanning (max 3 retries per step, max 10 replans per mission).

---

## 2. Mission State Machine & Concurrency

Missions follow an explicit lifecycle governed by `AgentLoop.kt`:

- `IDLE`: Runtime standing by for mission prompt.
- `PLANNING`: Synthesizing execution plan and validating schema.
- `RUNNING`: Actively executing steps and verifying postconditions.
- `PAUSED`: Execution suspended cooperatively via a non-spinning `CompletableDeferred` pause gate.
- `COMPLETED`: Terminal state; all steps executed and verified.
- `FAILED`: Terminal state; unrecoverable step failure or budget exceeded.
- `ABORTED`: Terminal state; user cancelled execution.

### Concurrency Guarantees
- **Mutual Exclusion:** Starting a mission while one is `PLANNING` or `RUNNING` is rejected with `BUSY`.
- **Cancellation Propagation:** Coroutine cancellation cleans up resources, records `ABORTED` in Room, and rethrows `CancellationException`.
- **Terminal Immutability:** Terminal states (`COMPLETED`, `FAILED`, `ABORTED`) reject late resume calls.

---

## 3. Bounded Recovery Budgets (`ExecutionLimits`)

- **Max Steps per Plan:** 30 steps
- **Max Retries per Step:** 3 attempts
- **Max Replans per Mission:** 10 dynamic recovery injections
- **Mission Timeout:** 600 seconds (10 minutes)
- **Tool Execution Timeout:** 15 seconds
- **LLM Provider Timeout:** 45 seconds
- **Max Accessibility Nodes Traversed:** 1,000 nodes (depth limit 30, text limit 512 chars)
