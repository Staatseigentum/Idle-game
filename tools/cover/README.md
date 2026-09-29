# itch.io cover art

Generates Embercrown's store cover the same way the game generates its sprites: shapes composed
on a coarse grid, shaded across multi-step ramps with an ordered Bayer dither, then contoured and
blitted nearest-neighbour. `grid.js` is a JS port of `PixelGridBuilder`
(`composeApp/src/commonMain/kotlin/com/embercrown/game/ui/pixelart/PixelArt.kt`), so the cover and
the in-game art share one drawing model — no PNG is hand-painted, and re-running regenerates
everything from source.

```bash
node tools/cover/cover.js tools/cover
```

No dependencies: `png.js` writes the PNG with Node's own `zlib`.

| File | Purpose |
| --- | --- |
| `embercrown-cover-630x500.png` | itch.io cover image — its recommended size exactly |
| `embercrown-cover-1260x1000.png` | same art at 10x, for press kits or resizing down |
| `embercrown-cover-installer-314x249.png` | fitted art for the Windows setup theme; WiX clips oversized images |

The art is authored at 126x100 and scaled by an integer factor, so every pixel stays a square at
either size. Changing the aspect ratio means changing `W`/`H` in `cover.js` and re-laying out the
composition — the layout constants (`HORIZON`, `WALL_TOP`, tower table, type positions) are all at
the top of their sections.

## Notes

- Colors come from `EmberPalette` / `Ramps` in `ui/pixelart/EmberPalette.kt`. Gold and flame use
  hand-picked stops rather than a two-endpoint lerp, which would pass through desaturated beige.
- The crown mirrors `appIcon()`'s geometry, so the store page and the desktop icon agree.
- The wordmark is a bitmap face in `font.js` rather than Press Start 2P: the TTF cannot be
  rasterized without a dependency, so the glyphs are drawn to match its blocky, square-terminal
  look. Only the glyphs the cover actually sets are defined; adding text may need new ones.
