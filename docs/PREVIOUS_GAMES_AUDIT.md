# Thunder Stack — previous games audit

## Scope

ThunderStack started as an empty project. The read-only review covered the adjacent `zeus`,
`aero/zeus-temple-stack`, `aero/thunderbound-zeus`, `Volcanix`, `Frostline Fishing`, and `Fruvio`
projects, plus the existing Thunder Stack atlas pack. The old atlas is a generated crop pack, not
a screen implementation, and its 124-file contract is intentionally not carried into this game.

## Useful patterns found

- The Zeus stack prototype keeps overlap, placement grade, score, stability, and collapse in a
  deterministic Kotlin engine. Presentation consumes snapshots and never decides results.
- Volcanix uses a single `AndroidViewModel` with `StateFlow`, a DataStore persistence facade,
  immutable progress updates, idempotent rewards, and a Compose Canvas for gameplay.
- The strongest projects centralize routes and Android Back handling, freeze gameplay under pause,
  save on lifecycle transitions, and keep local leaderboard copy honest.
- Reusable image-backed buttons/panels, safe-area layout, reverse-order modal input capture, and
  one source of truth for currency/inventory are consistently reliable patterns.
- Unit tests around engine outcomes, purchases, daily claims, unlocks, and achievement claims catch
  the most expensive regressions early.

## Patterns worth reusing

- Pure Kotlin `StackEngine` + tests, independent from Compose and Android resources.
- One application state owner (`ThunderStackViewModel`) and one persistence boundary
  (`ProgressStore`).
- Data-driven campaign definitions, shop offers, achievements, rewards, and local leaderboard.
- Responsive portrait Compose layout with a fixed logical gameplay coordinate space.
- Generated art used as layered backgrounds/chrome only; all labels, values, controls, and hit
  targets remain live UI.
- Explicit disabled states, transactional spends/claims, haptic/audio cues, and lifecycle-aware
  pause/save behavior.

## Patterns that should not be reused

- Monolithic thousand-line screens that mix rendering, routing, economy, and rule resolution.
- Frame-dependent physics, arbitrary hard-coded device pixels, or collision derived from the
  stretched bitmap rather than the engine model.
- Flattened mockup screenshots with invisible hitboxes.
- Multiple near-identical button/background/panel files where one component can be reused.
- Fake online leaderboard language, duplicate-tap rewards, mutable preference keys in screens,
  or settings switches that do not control real behavior.
- Mixed remnants from merge-game prototypes; Thunder Stack remains a precision tower stacker.

## Thunder Stack decision

Use a single Android app module with Jetpack Compose, a pure Kotlin rules package, DataStore-backed
progress, and a Canvas-based real-time stack arena. The production drawable contract is exactly the
28 files requested by the developer; navigation glyphs, labels, progress bars, stars, and toggles
are code-rendered so they stay crisp and functional.
