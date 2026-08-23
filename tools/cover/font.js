"use strict";

/**
 * Two hand-authored bitmap faces. The game itself sets type in Press Start 2P, but a TTF can't
 * be rasterized here, so these mirror its blocky, square-terminal look: a heavy 8x10 face for
 * the wordmark and a 3x5 face for the tagline.
 */

// 8 wide x 10 tall, 2px strokes. Only the glyphs "EMBERCROWN" needs.
const TITLE = {
  E: [
    "########",
    "########",
    "##......",
    "##......",
    "######..",
    "######..",
    "##......",
    "##......",
    "########",
    "########",
  ],
  M: [
    "##....##",
    "###..###",
    "########",
    "########",
    "##.##.##",
    "##.##.##",
    "##....##",
    "##....##",
    "##....##",
    "##....##",
  ],
  B: [
    "#######.",
    "########",
    "##....##",
    "##....##",
    "#######.",
    "#######.",
    "##....##",
    "##....##",
    "########",
    "#######.",
  ],
  R: [
    "#######.",
    "########",
    "##....##",
    "##....##",
    "########",
    "#######.",
    "##.##...",
    "##..##..",
    "##...##.",
    "##....##",
  ],
  C: [
    ".######.",
    "########",
    "##....##",
    "##......",
    "##......",
    "##......",
    "##......",
    "##....##",
    "########",
    ".######.",
  ],
  O: [
    ".######.",
    "########",
    "##....##",
    "##....##",
    "##....##",
    "##....##",
    "##....##",
    "##....##",
    "########",
    ".######.",
  ],
  W: [
    "##....##",
    "##....##",
    "##....##",
    "##....##",
    "##....##",
    "##....##",
    "##.##.##",
    "##.##.##",
    "########",
    ".##..##.",
  ],
  N: [
    "##....##",
    "###...##",
    "####..##",
    "####..##",
    "##.##.##",
    "##.##.##",
    "##..####",
    "##..####",
    "##...###",
    "##....##",
  ],
};

// 3 wide (1 for I) x 5 tall.
const SMALL = {
  A: [".#.", "#.#", "###", "#.#", "#.#"],
  // D needs the fourth column too: at 3px its bowl closes up and it reads as an O.
  D: ["###.", "#..#", "#..#", "#..#", "###."],
  E: ["###", "#..", "##.", "#..", "###"],
  G: ["###", "#..", "#.#", "#.#", "###"],
  H: ["#.#", "#.#", "###", "#.#", "#.#"],
  I: ["#", "#", "#", "#", "#"],
  K: ["#.#", "##.", "#..", "##.", "#.#"],
  L: ["#..", "#..", "#..", "#..", "###"],
  // M and N go a column wider than the rest: at 3px they collapse into the same shape.
  M: ["#..#", "####", "####", "#..#", "#..#"],
  N: ["#..#", "##.#", "####", "#.##", "#..#"],
  O: ["###", "#.#", "#.#", "#.#", "###"],
  R: ["##.", "#.#", "##.", "#.#", "#.#"],
  S: ["###", "#..", "###", "..#", "###"],
  T: ["###", ".#.", ".#.", ".#.", ".#."],
  U: ["#.#", "#.#", "#.#", "#.#", "###"],
  V: ["#.#", "#.#", "#.#", "#.#", ".#."],
  Y: ["#.#", "#.#", ".#.", ".#.", ".#."],
  ".": ["...", "...", "...", "...", ".#."],
};

function glyphWidth(face, ch) {
  if (ch === " ") return face === TITLE ? 4 : 2;
  const g = face[ch];
  if (!g) throw new Error(`no glyph for ${JSON.stringify(ch)}`);
  return g[0].length;
}

/** Total advance width of `text`, with `gap` blank columns between glyphs. */
function measure(face, text, gap) {
  let w = 0;
  for (let i = 0; i < text.length; i++) {
    w += glyphWidth(face, text[i]);
    if (i < text.length - 1) w += gap;
  }
  return w;
}

/** Stamps `text` into `grid` at (x, y) using `char` for lit pixels. Returns the end x. */
function draw(grid, face, text, x, y, char, gap) {
  let cx = x;
  for (const ch of text) {
    const g = face[ch];
    if (g) {
      for (let row = 0; row < g.length; row++) {
        for (let col = 0; col < g[row].length; col++) {
          if (g[row][col] === "#") grid.set(cx + col, y + row, char);
        }
      }
    }
    cx += glyphWidth(face, ch) + gap;
  }
  return cx - gap;
}

module.exports = { TITLE, SMALL, measure, draw };
