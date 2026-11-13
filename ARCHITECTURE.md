# Moru Community Edition architecture

The app uses a small clean architecture split so the task-first Workbench can coordinate terminal, agent session,
file, Git, and Approval capabilities without turning UI code into the runtime.

## Dependency Rule

Dependencies point inward:

```text
app-desktop
  -> feature:*
  -> core:*

feature:*
  -> core:model
  -> core:designsystem
  -> core:ui
  -> other core:* contracts where needed

core:ui
  -> core:model
  -> core:designsystem

core:data
  -> core:domain
  -> core:model

core:domain
  -> core:model
```

`core:model` has no dependency on UI, storage, terminal, or process execution.

## Modules

- `app-desktop`: Compose window, top-level composition, desktop wiring, menus,
  and attention and notifications.
- `core:model`: pure Project, Task, Work Context, and settings data, and the
  session facts the sidebar and the session views share (`core.model.session`:
  `SessionActivity`, `AgentRuntime`, `AgentSessionRow`, `SessionTaskRef`).
- `core:domain`: use cases and repository contracts.
- `core:data`: Room catalog and notification storage, settings, Git, knowledge
  sources, and project files.
- `core:designsystem`: theme, tokens, icons, and shared components. It knows
  nothing of the domain model.
- `core:ui`: domain-aware composables shared by more than one feature: the
  diff file row and file view, code highlighting (`CodeLanguage`,
  `highlightCode`, `CodeColoring`), relative times, short token counts
  (`formatTokenCount`), the Task identity accent (`taskIdentityAccent`), and
  their own strings (`CoreUiStrings`).
- `core:terminal`: terminal session contracts, desktop PTY/JediTerm adapters,
  the detached terminal host, and opening a session in the system's own
  terminal app (`core.terminal.external`).
- `core:runtime`: installed AI CLI and local tool discovery.
- `core:system`: desktop OS integration: system notifications and the Dock
  badge behind `AttentionNotifier`, and the result-sharing helper.
- `feature:terminal`: terminal pane UI.
- `feature:settings`: settings and runtime diagnostics.
- `feature:notifications`: the notification inbox shown from the title bar.
- `feature:git`: the project Git screen: history graph, commit diffs, working
  changes, branches, and sync. `app-desktop` composes `ProjectGitScreen`.
- `feature:codepanel`: the Task's Code panel: the checkout a terminal is
  changing, its changed files and their code, and the per-Task saved checkout
  (`CodePanelState`, `rememberCodePanelState`). `CodePanelSource` is
  implemented in `app-desktop`.
- `feature:sessions`: terminal sessions as the sidebar shows them: what each
  session is doing (`deriveSessionActivity`, `SessionClock`), per-Task activity
  and last lines, the session row, the list of every session
  (`SessionsPopover`), the sessions without a Task, and their strings.
- `feature:workbench`: Projects/Tasks, Result, files, and knowledge
  composition, and the Task overlays of session activity
  (`withSessionActivities`, `runningSessionsByProject`). It shows the terminal
  pane, the Code panel, and the sidebar's sessions through
  `WorkbenchPaneSlots` (`WorkbenchTerminalSlots`, `WorkbenchSessionSlots`,
  `WorkbenchCodeSlots`), which `app-desktop` fills
  from `feature:terminal`, `feature:sessions`, and
  `feature:codepanel`, so features never import each other.

## Boundary Rules

- Keep PTY/JediTerm implementation behind `core:terminal` contracts.
- Keep simple and advanced modes over the same Task state.

## Screen State

- A screen that changes state over time (actions, repeated reads, polling)
  gets a plain `FooStateHolder` class: Compose state it exposes read-only, one
  `onEvent` (or purpose-named methods) for input, and its dispatcher passed in
  with `Dispatchers.IO` as the default so tests can replace it.
- The composition owns the holder's lifetime: the screen `remember`s it keyed
  by what it serves, and either passes a scope from `rememberCoroutineScope()`
  or runs the holder's suspending loops in its own `LaunchedEffect`s. A holder
  never creates its own scope.
- A one-shot read of an injected loader may stay a `produceState` in the
  screen; IO itself always sits behind a `core` contract.
- The stateful `FooScreen` wires the holder; the stateless `FooContent`
  receives values and callbacks only.

## Enforcement

`architecture-test` checks these rules on every `./gradlew test`:

- project dependencies point inward, and no `feature -> feature` or
  `core -> feature` edge exists; `core:ui` depends only on `core:model` and
  `core:designsystem`; capability modules (terminal, runtime, system) may use each other but not `core:data`,
  `core:designsystem`, or `core:ui`;
- every declared project dependency is used;
- feature modules do not start processes, read system properties or the
  environment, import `java.io.File`/`java.nio.file`, or import `core:data`;
- `app-desktop` does not start processes itself;
- Metro appears only in `app-desktop`;
- `core:model` imports only Kotlin and its own package.

Violations that predate the rules are listed in
`architecture-test/.../ArchitectureBaseline.kt`. The list only shrinks: a new
violation fails the build, and so does a fixed violation whose entry was not
deleted.
