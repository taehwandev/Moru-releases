# Moru Community Edition

A task-first desktop workbench for coding agents, written in Kotlin Multiplatform and Compose Multiplatform.
Each Project holds Tasks; each Task runs its agents in real terminals (a real PTY, not a pseudo-console) next to a
Result view with the changed code and Git history.

This repository is the open-source IDE core of an earlier Moru app. It is in **maintenance mode**:
bug fixes are welcome, new product features are not planned here. This repository ships the IDE core only.

## What is here

- Projects and Tasks with a persistent catalog.
- Real terminals through [pty4j](https://github.com/JetBrains/pty4j) and
  [JediTerm](https://github.com/JetBrains/jediterm), including a detached terminal host that outlives the window.
- An agent session list that shows which sessions are working, waiting or done.
- A Result pane with the Task's changed files and code, and a Git screen with history, diffs and branches.
- A notification inbox and desktop notifications when an agent needs you.
- Settings, a knowledge-sources screen, English and Korean text.

See [ARCHITECTURE.md](ARCHITECTURE.md) for the module layout and the rules `architecture-test` enforces.

## Build and run

Requires JDK 21 and macOS (the primary platform).

```sh
./gradlew :app-desktop:run          # run the app in development mode
./gradlew desktopTest               # run every module's tests
./gradlew :app-desktop:packageDmg   # build a macOS DMG
```

Release signing is off unless `MORU_SIGN_IDENTITY` names a Developer ID certificate. The app stores its data in
`~/Library/Application Support/Moru Community` and `~/.moru-community`, and does not touch other Moru data.

## License

Apache License 2.0, see [LICENSE](LICENSE) and [NOTICE](NOTICE). Third-party libraries and their licenses are listed
in [THIRD_PARTY.md](THIRD_PARTY.md).
