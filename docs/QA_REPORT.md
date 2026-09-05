# QA report

## Automated verification

- Kotlin/Compose compilation: passed.
- Debug APK assembly: passed.
- JVM unit tests: 15 passed.
- Covered tower rules: full-width overhang, support ratio, counterbalance, delayed collapse, shield save, Thunder alignment, campaign completion, and early material variety.
- Covered Crystal Rush rules: clamped drag, single-hit collisions, frame-rate-independent spawn scheduling, once-per-day payout, practice payout suppression, and best-score persistence rules.
- Final visual resource set: 33 PNG files. It contains the 28 generated ThunderStack assets, four colored gems extracted from the supplied `ui.png`, and the gold-column decoration extracted from the supplied `back.png`.
- Removed every asset imported from the unverified secondary pack. Navigation, tabs, progress, and toggles are now composed from the generated ThunderStack controls or native Compose shapes.

## Android runtime verification

Test targets: local `tt_pixel` Android AVD at 360 × 800, 412 × 915, and 432 × 960 dp; font scale 1.0 and 1.3. Final install target: physical Seeker device.

Verified flow:

1. Cold app launch and persistent progress load.
2. Home screen rendering and currency display.
3. Navigation to the 60-trial level path.
4. Locked/unlocked level behavior and Trial 1 briefing.
5. Gameplay start, full moving/placed blocks, alternating materials, tap-to-place, score/coin/height/stability update.
6. Pause overlay, fatal miss collapse phase, delayed result, and stopped simulation while paused.
7. Shop, Heroic Feats, Local Legends, Settings, and Daily at safe insets with scroll and 1.3 font scale.
8. Crystal Rush ready/play/result flow, timer, collectibles, hazards, magnet drag, and daily payout.
9. No fatal exception, ANR, or out-of-memory event in runtime logs.

Final polish pass on the physical Seeker verified the restored Home composition, the unchanged Path layout, gameplay HUD, single-layer ornate Pause popup, and Crystal Rush ready popup.

Captured screens are stored in `qa/screenshots`.
