# Asset generation record

## Method

All 28 bitmap assets were generated with the `imagegen` image-generation mode using the three supplied Zeus/Olympus references as visual direction. Follow-up image-edit generations were used only where transparent background extraction needed correction. The generated masters were then losslessly named and technically resized for Android memory safety; the design content was not redrawn during optimization.

## Shared art-direction prompt

`Premium Zeus/Olympus mobile game art inspired by the supplied references: polished casual-game readability, antique Greek gold trim, white marble, deep royal purple and storm navy, electric cyan lightning, saturated but controlled highlights, centered silhouette, strong depth, no UI-sheet background, no watermarks, no extra labels. Keep a consistent Thunder Stack visual system across every asset.`

## Prompt set

- Branding: a transparent, centered `THUNDER STACK` logo with temple, lightning bolt, marble, gold, and purple enamel.
- Loading background: Zeus above a cloud temple, warm sunrise below and blue storm above, portrait, composition left open for logo/progress.
- Home background: bright Olympus terrace and winding celestial path, portrait, readable central negative space.
- Level-path background: long winding road through six changing Olympus biomes, portrait, no baked nodes or text.
- Gameplay background: open luminous sky with a calm central build lane and cloud horizon, portrait.
- UI controls: primary default/disabled, square button, title banner, popup, universal card, leaderboard row, and level node; isolated front-facing transparent pieces using the same gold/marble/purple/cyan construction.
- Currency: isolated gold thunder coin and faceted blue divine crystal.
- Gameplay pieces: start platform and stone, moss, gold, storm, and cracked block variants; wide readable silhouettes and transparent backgrounds.
- Boosters: isolated premium emblems for thunder, aegis shield, hourglass slow time, and crystal magnet.
- FX: isolated crystal collectible, branched cyan lightning, and radial blue-gold perfect glow.

## Final 28-file contract

```text
logo_thunder_stack.png
bg_loading_zeus.png
bg_home_olympus.png
bg_level_select_path.png
bg_gameplay_olympus_sky.png
btn_primary_default.png
btn_primary_disabled.png
btn_square.png
panel_title_banner.png
panel_popup.png
panel_card.png
panel_leaderboard_row.png
level_node.png
ic_coin.png
ic_crystal.png
platform_start.png
block_stone.png
block_stone_moss.png
block_gold.png
block_storm.png
block_cracked.png
booster_thunder.png
booster_shield.png
booster_slow_time.png
booster_crystal_magnet.png
collectible_crystal.png
fx_lightning.png
fx_perfect_glow.png
```

The files are stored in `app/src/main/res/drawable-nodpi`. Backgrounds are full portrait RGB art. All isolated art uses real PNG alpha except the additive perfect-glow texture, whose black pixels are intentionally neutralized by the gameplay renderer's `BlendMode.Plus` pass.

## Crystal Rush gem extension

Four additional transparent collectibles were generated with the built-in `imagegen` mode from the verified `ui.png` gem silhouettes and the existing Thunder Stack crystal as style references. Each master was requested as a single centered mobile-game collectible with crisp faceting, antique-gold bevel, restrained cyan glow, no text, no container, and a genuinely transparent background. The red rounded-square ruby, blue diamond sapphire, purple pentagonal amethyst, and green triangular emerald variants were downscaled to 320 × 320 PNG while preserving alpha:

```text
rush_gem_red.png
rush_gem_blue.png
rush_gem_purple.png
rush_gem_green.png
```

The source masters are retained in the Codex generated-images output for this task; the optimized runtime files are stored with the other game assets in `app/src/main/res/drawable-nodpi`.

## Navigation and settings-control extension

The built-in `imagegen` mode generated six additional Thunder Stack UI layers from the verified `ui.png` and the generated `btn_square.png` material reference:

```text
ic_nav_back.png
ic_nav_home.png
ic_nav_pause.png
toggle_track_off.png
toggle_track_on.png
toggle_knob.png
```

The navigation glyphs are isolated marble-and-gold symbols with restrained cyan edge light and no baked button plate. The toggle uses separate OFF/ON tracks plus a lightning knob so Compose can crossfade the track and physically slide the knob. Checkerboard-backed generations were rejected during alpha QA; the accepted track and home masters used a flat chroma background that was mechanically removed, then every final asset was resized and verified with zero-alpha corners.
