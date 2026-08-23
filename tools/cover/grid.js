"use strict";

/**
 * A JS port of the game's `PixelGridBuilder` (composeApp .../ui/pixelart/PixelArt.kt) so the
 * cover is drawn with the exact same primitives, Bayer dithering and outlining as the in-game
 * sprites — same look, no hand-pushed pixels.
 */

const BAYER_4X4 = [
  [0, 8, 2, 10],
  [12, 4, 14, 6],
  [3, 11, 1, 9],
  [15, 7, 13, 5],
];

class Grid {
  constructor(width, height) {
    this.width = width;
    this.height = height;
    this.cells = Array.from({ length: height }, () => new Array(width).fill("."));
  }

  set(x, y, c) {
    if (x >= 0 && x < this.width && y >= 0 && y < this.height) this.cells[y][x] = c;
    return this;
  }

  at(x, y) {
    if (x < 0 || x >= this.width || y < 0 || y >= this.height) return ".";
    return this.cells[y][x];
  }

  rect(x0, y0, x1, y1, c) {
    for (let y = Math.max(0, Math.min(y0, y1)); y <= Math.min(this.height - 1, Math.max(y0, y1)); y++) {
      for (let x = Math.max(0, Math.min(x0, x1)); x <= Math.min(this.width - 1, Math.max(x0, x1)); x++) {
        this.cells[y][x] = c;
      }
    }
    return this;
  }

  rectOutline(x0, y0, x1, y1, c) {
    for (let x = x0; x <= x1; x++) { this.set(x, y0, c); this.set(x, y1, c); }
    for (let y = y0; y <= y1; y++) { this.set(x0, y, c); this.set(x1, y, c); }
    return this;
  }

  circle(cx, cy, r, c) {
    for (let y = cy - r; y <= cy + r; y++) for (let x = cx - r; x <= cx + r; x++) {
      const dx = x - cx, dy = y - cy;
      if (dx * dx + dy * dy <= r * r) this.set(x, y, c);
    }
    return this;
  }

  ellipse(cx, cy, rx, ry, c) {
    for (let y = cy - ry; y <= cy + ry; y++) for (let x = cx - rx; x <= cx + rx; x++) {
      const dx = (x - cx) / rx, dy = (y - cy) / ry;
      if (dx * dx + dy * dy <= 1) this.set(x, y, c);
    }
    return this;
  }

  triangle(x0, y0, x1, y1, x2, y2, c) {
    const sign = (ax, ay, bx, by, cx, cy) => (ax - cx) * (by - cy) - (bx - cx) * (ay - cy);
    for (let y = Math.min(y0, y1, y2); y <= Math.max(y0, y1, y2); y++) {
      for (let x = Math.min(x0, x1, x2); x <= Math.max(x0, x1, x2); x++) {
        const d1 = sign(x, y, x0, y0, x1, y1);
        const d2 = sign(x, y, x1, y1, x2, y2);
        const d3 = sign(x, y, x2, y2, x0, y0);
        const hasNeg = d1 < 0 || d2 < 0 || d3 < 0;
        const hasPos = d1 > 0 || d2 > 0 || d3 > 0;
        if (!(hasNeg && hasPos)) this.set(x, y, c);
      }
    }
    return this;
  }

  /** Same contract as the Kotlin version: shade `maskChar` across `ramp` with Bayer dithering. */
  ditherRamp(maskChar, ramp, t, sharpness = 2.4) {
    if (ramp.length < 2) return this;
    for (let y = 0; y < this.height; y++) for (let x = 0; x < this.width; x++) {
      if (this.cells[y][x] !== maskChar) continue;
      const p = clamp01(t(x, y)) * (ramp.length - 1);
      const lo = Math.min(Math.floor(p), ramp.length - 2);
      const frac = clamp01(((p - lo) - 0.5) * sharpness + 0.5);
      const threshold = BAYER_4X4[y % 4][x % 4] / 16;
      this.cells[y][x] = frac > threshold ? ramp[lo + 1] : ramp[lo];
    }
    return this;
  }

  /** Wraps every subject touching a background cell in `outlineChar`, off a snapshot. */
  outlineAgainst(backgroundChars, outlineChar) {
    const snapshot = this.cells.map((row) => row.slice());
    const isBg = (x, y) => {
      if (x < 0 || x >= this.width || y < 0 || y >= this.height) return true;
      return backgroundChars.includes(snapshot[y][x]);
    };
    for (let y = 0; y < this.height; y++) for (let x = 0; x < this.width; x++) {
      if (!isBg(x, y)) continue;
      if (!isBg(x - 1, y) || !isBg(x + 1, y) || !isBg(x, y - 1) || !isBg(x, y + 1)) {
        this.cells[y][x] = outlineChar;
      }
    }
    return this;
  }
}

const clamp01 = (v) => (v < 0 ? 0 : v > 1 ? 1 : v);

const radialFalloff = (cx, cy, radius) => (x, y) => {
  const dx = x + 0.5 - cx, dy = y + 0.5 - cy;
  return clamp01(1 - Math.sqrt(dx * dx + dy * dy) / radius);
};

const linearFalloff = (from, to, vertical = true) => (x, y) => {
  const p = (vertical ? y : x) + 0.5;
  return clamp01((p - from) / (to - from));
};

const directionalFalloff = (ox, oy, dx, dy, reach) => (x, y) =>
  clamp01(0.5 + ((x + 0.5 - ox) * dx + (y + 0.5 - oy) * dy) / reach);

/** `#rrggbb` -> [r,g,b]. */
function rgb(hex) {
  const n = parseInt(hex.slice(1), 16);
  return [(n >> 16) & 0xff, (n >> 8) & 0xff, n & 0xff];
}

function lerpColor(a, b, t) {
  const ct = clamp01(t);
  const ca = rgb(a), cb = rgb(b);
  return [0, 1, 2].map((i) => Math.round(ca[i] + (cb[i] - ca[i]) * ct));
}

/**
 * Keys `ramp`'s chars to hand-picked stops. A straight lerp between two endpoints walks through
 * whatever sits on the line between them, which for gold means desaturated beige mid-tones; the
 * stops let the ramp bow toward saturation where the eye expects it.
 */
function stopPalette(ramp, stops) {
  if (ramp.length !== stops.length) {
    throw new Error(`ramp ${ramp} has ${ramp.length} chars but ${stops.length} stops`);
  }
  const out = {};
  for (let i = 0; i < ramp.length; i++) out[ramp[i]] = rgb(stops[i]);
  return out;
}

/** Evenly spaced colors dark->light keyed by the ramp's chars. */
function rampPalette(ramp, dark, light) {
  const out = {};
  for (let i = 0; i < ramp.length; i++) out[ramp[i]] = lerpColor(dark, light, i / (ramp.length - 1));
  return out;
}

/** Integer hash — scatters stars without lining them up into diagonals. */
function hash(n) {
  let h = Math.imul(n, 374761393) + 668265263;
  h = Math.imul(h ^ (h >>> 13), 1274126177);
  return (h ^ (h >>> 16)) & 0x7fffffff;
}

module.exports = {
  Grid, radialFalloff, linearFalloff, directionalFalloff,
  rgb, lerpColor, rampPalette, stopPalette, hash, clamp01,
};
