# Thunder Stack

Portrait offline Android stacking game set on the path to Olympus. The project is a native Kotlin/Jetpack Compose application with deterministic gameplay rules, persistent progression, procedural audio, haptics, and an expanded reference-driven Olympus art pack.

## Included gameplay

- 60 campaign trials across six realms plus an endless mode.
- Full-block placement with support ratio, overhang, counterbalance, lean, impact sway, animated tower collapse, wind, gold, storm, cracked, and moss materials.
- Crystal Rush: a 30-second magnet minigame with gems, crystals, coins, cracked hazards, lightning shields, one rewarded run per day, and unlimited practice.
- Four functional boosters: Thunder Strike, Aegis Shield, Slow Time, and Crystal Magnet.
- Persistent stars, best scores (including Crystal Rush), currency, booster inventory, settings, daily streak, achievements, and local records via DataStore schema 2.
- Home, level path, pre-level briefing, gameplay, pause, result, shop, achievements, daily reward, local leaderboard, and settings screens.
- Offline procedural ambience/SFX and optional haptics; no fake network services.

## Build and test

Use JDK 17 and an Android SDK with API 35.

```powershell
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:assembleDebug
```

The installable APK is produced at `app/build/outputs/apk/debug/app-debug.apk`.

## Project notes

- Prior-project audit: `docs/PREVIOUS_GAMES_AUDIT.md`
- Implementation plan: `docs/IMPLEMENTATION_PLAN.md`
- Asset generation record: `docs/ASSET_GENERATION.md`
- Device QA record: `docs/QA_REPORT.md`
