"use strict";

const fs = require("fs");
const path = require("path");
const { encodePng } = require("./png");
const { Grid, radialFalloff, linearFalloff, rampPalette, stopPalette, rgb, hash, clamp01 } =
  require("./grid");
const FONT = require("./font");

// The art is authored on a coarse grid and blown up by an integer factor, so every pixel stays a
// crisp square — the same nearest-neighbour blit the game uses for its sprites.
const W = 126;
const H = 100;

const HORIZON = 88; // where the citadel's ground line sits
const WALL_TOP = 78;
const CENTER_X = 63;

// Palette key groups, all mutually distinct so materials never bleed into each other's ramps.
const SKY = "12345678";
const HILL = "wxy";
const GROUND = "ABCD";
const ROCK = "QRS";
const WALL = "EFGH";
const TOWER = "MNOP";
const MERLON = "IJKL";
const CROWN_GOLD = "abcdef";
const GEM = "ijkl";
const TITLE_GOLD = "ghmnop";
const STAR = "*";
const STAR_DIM = "+";
const WINDOW = "@";
const DOOR = "#";
const EMBER_HOT = "^";
const EMBER_MID = "%";
const EMBER_DIM = "&";
const INK = "~";
const TAG = ";";
const FRAME_INK = "_";
const FRAME_LIGHT = "<";
const FRAME_DARK = ">";

// Everything a subject can be outlined against.
const BG_SKY = SKY + STAR + STAR_DIM + EMBER_HOT + EMBER_MID + EMBER_DIM;
const BG_ALL = BG_SKY + HILL + GROUND + ROCK;

const g = new Grid(W, H);

// ---------------------------------------------------------------- sky and stars
// The sky is one ramp driven by a bloom centred on the crown — the "ember" the kingdom is named
// for, and the contrast the wordmark needs to stay legible at thumbnail size. Laying a separate
// glow circle over a vertical gradient would leave its rim visible wherever the two disagreed;
// folding both into a single falloff keeps the sky seamless out to the corners.
const bloom = radialFalloff(CENTER_X, 44, 62);
g.rect(0, 0, W - 1, HORIZON, "1");
g.ditherRamp(
  "1",
  SKY,
  (x, y) => clamp01(Math.pow(bloom(x, y), 0.8) * 0.9 + Math.pow(clamp01(y / HORIZON), 3) * 0.18),
  1,
);

for (let i = 0; i < 150; i++) {
  const sx = hash(i * 2 + 7919) % W;
  const sy = hash(i * 2 + 1 + 104729) % (HORIZON - 6);
  // Stars only survive in the darker reaches; the bloom's core would swallow them anyway.
  if (!SKY.slice(0, 3).includes(g.at(sx, sy))) continue;
  g.set(sx, sy, hash(i * 31) % 3 === 0 ? STAR : STAR_DIM);
}

// ---------------------------------------------------------------- land
g.ellipse(20, HORIZON + 2, 46, 9, "w");
g.ellipse(108, HORIZON + 1, 40, 8, "w");
g.ditherRamp("w", HILL, linearFalloff(HORIZON - 8, HORIZON + 6), 2);

g.rect(0, HORIZON + 1, W - 1, H - 1, "A");
g.ditherRamp(
  "A",
  GROUND,
  (x, y) => {
    // A cone of gate-light widening toward the viewer, plus a thin ambient band along the wall.
    const d = y - HORIZON;
    const cone = clamp01(1 - Math.abs(x - CENTER_X) / (8 + d * 2.4)) * clamp01(1 - d / 14);
    const ambient = clamp01(1 - d / 4) * 0.25;
    return clamp01(0.08 + cone * 0.95 + ambient);
  },
  2,
);

// Scattered rubble so the foreground is not a flat slab.
for (let i = 0; i < 26; i++) {
  const rx = hash(i * 5 + 4242) % W;
  const ry = HORIZON + 3 + (hash(i * 5 + 1 + 31337) % 9);
  g.rect(rx, ry, rx + 1 + (hash(i * 11) % 3), ry, "Q");
}
g.ditherRamp("Q", ROCK, linearFalloff(HORIZON + 14, HORIZON), 3);

// ---------------------------------------------------------------- citadel
const WALL_X0 = 30;
const WALL_X1 = 96;

g.rect(WALL_X0, WALL_TOP, WALL_X1, HORIZON, "E");
g.ditherRamp("E", WALL, linearFalloff(HORIZON + 4, WALL_TOP - 4), 3);

// Wall crenellations, then the towers, so a tower always reads as being in front of the wall.
for (let x = WALL_X0; x + 1 <= WALL_X1; x += 4) g.rect(x, WALL_TOP - 3, x + 1, WALL_TOP - 1, "I");

const TOWERS = [
  { cx: 34, half: 4, top: 70 },
  { cx: 46, half: 4, top: 66 },
  { cx: CENTER_X, half: 7, top: 72 },
  { cx: 80, half: 4, top: 66 },
  { cx: 92, half: 4, top: 70 },
];

for (const t of TOWERS) {
  g.rect(t.cx - t.half, t.top, t.cx + t.half, HORIZON, "M");
  // Merlons: 2 wide with a 1-wide embrasure between them, laid out from the tower's left edge.
  for (let x = t.cx - t.half; x <= t.cx + t.half; x += 3) {
    g.rect(x, t.top - 3, Math.min(x + 1, t.cx + t.half), t.top - 1, "I");
  }
}
// A per-tower cylindrical falloff would be more correct, but at this size one wide falloff reads
// the same, because every tower shares the bloom as its light source.
g.ditherRamp("M", TOWER, radialFalloff(CENTER_X - 8, 58, 74), 2.6);
g.ditherRamp("I", MERLON, radialFalloff(CENTER_X - 8, 58, 74), 2.6);

// Lit windows — the only saturated points in the architecture, so they carry the inhabited read.
const WINDOWS = [
  [34, 74], [34, 80], [46, 70], [46, 76], [46, 82],
  [59, 76], [67, 76], [59, 82], [67, 82],
  [80, 70], [80, 76], [80, 82], [92, 74], [92, 80],
  [39, 83], [53, 83], [73, 83], [87, 83],
];
for (const [wx, wy] of WINDOWS) g.rect(wx, wy, wx, wy + 1, WINDOW);

// Gate: a rounded arch with the light of the hall standing in it.
g.rect(CENTER_X - 3, HORIZON - 7, CENTER_X + 3, HORIZON, DOOR);
g.rect(CENTER_X - 2, HORIZON - 9, CENTER_X + 2, HORIZON - 8, DOOR);
g.rect(CENTER_X - 1, HORIZON - 10, CENTER_X + 1, HORIZON - 10, DOOR);
g.rect(CENTER_X - 2, HORIZON - 2, CENTER_X + 2, HORIZON, WINDOW);

g.outlineAgainst(BG_ALL, INK);

// Braziers flanking the gate, on the near side of the wall's outline.
for (const bx of [CENTER_X - 12, CENTER_X + 12]) {
  g.set(bx, HORIZON + 1, EMBER_MID);
  g.set(bx, HORIZON, EMBER_HOT);
}

// ---------------------------------------------------------------- embers
for (let i = 0; i < 70; i++) {
  const ex = hash(i * 3 + 555) % W;
  const ey = 24 + (hash(i * 3 + 1 + 999) % 54);
  if (!BG_SKY.includes(g.at(ex, ey))) continue;
  const roll = hash(i * 7 + 13) % 5;
  g.set(ex, ey, roll === 0 ? EMBER_HOT : roll <= 2 ? EMBER_MID : EMBER_DIM);
}

// ---------------------------------------------------------------- the crown
// Geometry mirrors the app icon's crown (ui/pixelart/AppIcon.kt), scaled up and re-lit from the
// upper left, with the same flame-toned gems that tie the name to the art.
g.rect(44, 48, 82, 55, "a");
g.triangle(44, 49, 56, 49, 50, 34, "a");
g.triangle(55, 49, 71, 49, CENTER_X, 32, "a");
g.triangle(70, 49, 82, 49, 76, 34, "a");
// The falloff has to die out inside the crown itself; a radius much larger than the sprite pins
// every cell to the ramp's pale end and the gold reads as flat cream.
g.ditherRamp("a", CROWN_GOLD, radialFalloff(50, 36, 42), 5);

// Each gem is shaded from its own centre, so the nearer ones do not wash out to one shared step,
// and each gets its own contour — without it a flame gem sinks into the gold around it.
const GEMS = [[50, 35, 3], [CENTER_X, 32, 3], [76, 35, 3], [CENTER_X, 51, 3]];
for (let i = 0; i < GEMS.length; i++) {
  const [gx, gy, gr] = GEMS[i];
  const mark = String.fromCharCode(0xe0 + i); // scratch marks, dithered away one at a time
  g.circle(gx, gy, gr + 1, INK);
  g.circle(gx, gy, gr, mark);
  g.ditherRamp(mark, GEM, radialFalloff(gx, gy - 1, gr + 2), 6);
}

g.outlineAgainst(BG_SKY, INK);

// ---------------------------------------------------------------- type
const TITLE_TEXT = "EMBERCROWN";
const TITLE_GAP = 2;
const TITLE_Y = 7;
const titleX = Math.round((W - FONT.measure(FONT.TITLE, TITLE_TEXT, TITLE_GAP)) / 2);
FONT.draw(g, FONT.TITLE, TITLE_TEXT, titleX, TITLE_Y, "m", TITLE_GAP);
g.ditherRamp("m", TITLE_GOLD, linearFalloff(TITLE_Y + 9.5, TITLE_Y - 0.5), 4);
g.outlineAgainst(BG_SKY, INK);

const TAG_TEXT = "A KINGDOM THAT NEVER TRULY DIES";
const TAG_GAP = 1;
const TAG_Y = 20;
const tagX = Math.round((W - FONT.measure(FONT.SMALL, TAG_TEXT, TAG_GAP)) / 2);
// A drop shadow rather than a full contour: at 5px cap height an outline closes the counters.
FONT.draw(g, FONT.SMALL, TAG_TEXT, tagX + 1, TAG_Y + 1, INK, TAG_GAP);
FONT.draw(g, FONT.SMALL, TAG_TEXT, tagX, TAG_Y, TAG, TAG_GAP);

// ---------------------------------------------------------------- frame
// The raised pixel bevel the game puts around every panel: light top/left, dark bottom/right.
for (let x = 1; x < W - 1; x++) {
  g.set(x, 1, FRAME_LIGHT);
  g.set(x, H - 2, FRAME_DARK);
}
for (let y = 1; y < H - 1; y++) {
  g.set(1, y, FRAME_LIGHT);
  g.set(W - 2, y, FRAME_DARK);
}
g.rectOutline(0, 0, W - 1, H - 1, FRAME_INK);

// ---------------------------------------------------------------- palette
// Tones are taken from the game's EmberPalette / Ramps (ui/pixelart/EmberPalette.kt).
const PALETTE = Object.assign(
  {},
  stopPalette(SKY, ["#05040e", "#100b20", "#1f1434", "#3b2038", "#67302a", "#9b481d", "#c25b18", "#dd7a1e"]),
  rampPalette(HILL, "#0d0912", "#241830"),
  rampPalette(GROUND, "#0a0806", "#4a3016"),
  rampPalette(ROCK, "#0c0a08", "#2c2018"),
  rampPalette(WALL, "#1a1a22", "#5e5e6c"),
  rampPalette(TOWER, "#20202a", "#7c7c8a"),
  rampPalette(MERLON, "#191921", "#6e6e7c"),
  stopPalette(CROWN_GOLD, ["#4a2a06", "#7a4d10", "#ad7a22", "#d9a840", "#f0c96a", "#fdecb4"]),
  stopPalette(GEM, ["#5e1502", "#a83206", "#e8792e", "#ffc861"]),
  stopPalette(TITLE_GOLD, ["#4a2a06", "#7a4d10", "#ad7a22", "#d9a840", "#f0c96a", "#fdecb4"]),
  {
    [STAR]: rgb("#f0e6d2"),
    [STAR_DIM]: rgb("#6b5a46"),
    [WINDOW]: rgb("#f4c24c"),
    [DOOR]: rgb("#120d0a"),
    [EMBER_HOT]: rgb("#f4c24c"),
    [EMBER_MID]: rgb("#e8792e"),
    [EMBER_DIM]: rgb("#8b2e2e"),
    [INK]: rgb("#120d0a"),
    [TAG]: rgb("#e0a94d"),
    [FRAME_INK]: rgb("#0a0806"),
    [FRAME_LIGHT]: rgb("#3a2c1e"),
    [FRAME_DARK]: rgb("#080604"),
  },
);

// ---------------------------------------------------------------- render
function render(scale) {
  const ow = W * scale;
  const oh = H * scale;
  const buf = Buffer.alloc(ow * oh * 4);
  const missing = new Set();
  for (let y = 0; y < H; y++) {
    for (let x = 0; x < W; x++) {
      const key = g.at(x, y);
      const color = PALETTE[key];
      if (!color) {
        missing.add(key);
        continue;
      }
      for (let dy = 0; dy < scale; dy++) {
        let o = ((y * scale + dy) * ow + x * scale) * 4;
        for (let dx = 0; dx < scale; dx++) {
          buf[o++] = color[0];
          buf[o++] = color[1];
          buf[o++] = color[2];
          buf[o++] = 255;
        }
      }
    }
  }
  if (missing.size) throw new Error(`unmapped palette keys: ${[...missing].join(" ")}`);
  return { png: encodePng(buf, ow, oh), ow, oh };
}

const outDir = process.argv[2] || __dirname;
fs.mkdirSync(outDir, { recursive: true });
for (const [scale, name] of [
  [5, "embercrown-cover-630x500.png"],
  [10, "embercrown-cover-1260x1000.png"],
]) {
  const { png, ow, oh } = render(scale);
  const file = path.join(outDir, name);
  fs.writeFileSync(file, png);
  console.log(`${file}  ${ow}x${oh}  ${(png.length / 1024).toFixed(1)} KB`);
}
