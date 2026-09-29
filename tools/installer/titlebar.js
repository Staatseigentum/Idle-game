"use strict";

// Deterministic pixel assets for WiX 3's three-state image buttons and window header.
const fs = require("fs");
const path = require("path");
const { encodePng } = require("../cover/png");

function canvas(width, height, fill) {
  const data = Buffer.alloc(width * height * 4);
  function rect(x, y, w, h, color) {
    for (let py = Math.max(0, y); py < Math.min(height, y + h); py++) {
      for (let px = Math.max(0, x); px < Math.min(width, x + w); px++) {
        const offset = (py * width + px) * 4;
        data[offset] = color[0];
        data[offset + 1] = color[1];
        data[offset + 2] = color[2];
        data[offset + 3] = 255;
      }
    }
  }
  rect(0, 0, width, height, fill);
  return { data, rect, width, height };
}

const header = canvas(740, 38, [33, 24, 39]);
header.rect(0, 0, 740, 2, [68, 41, 53]);
header.rect(0, 36, 740, 2, [153, 95, 51]);
header.rect(4, 5, 3, 27, [102, 59, 55]);
// A tiny ember crown, legible at normal Windows scaling.
for (const x of [17, 25, 33]) header.rect(x, 10, 4, 4, [244, 176, 83]);
header.rect(18, 15, 18, 9, [216, 136, 65]);
header.rect(20, 24, 14, 3, [136, 75, 47]);
header.rect(24, 18, 6, 5, [255, 205, 104]);
header.rect(14, 29, 27, 2, [105, 64, 53]);

function button(kind) {
  const result = canvas(28, 84, [33, 24, 39]);
  const states = [
    { bg: [43, 31, 46], edge: [120, 79, 58], ink: [226, 173, 94] },
    { bg: [80, 40, 47], edge: [235, 145, 67], ink: [255, 210, 130] },
    { bg: [111, 46, 47], edge: [255, 182, 86], ink: [255, 234, 180] },
  ];
  for (let state = 0; state < 3; state++) {
    const y = state * 28;
    const { bg, edge, ink } = states[state];
    result.rect(0, y, 28, 28, bg);
    result.rect(0, y, 28, 1, edge);
    result.rect(0, y + 27, 28, 1, edge);
    result.rect(0, y, 1, 28, edge);
    result.rect(27, y, 1, 28, edge);
    if (kind === "close") {
      for (let i = 0; i < 6; i++) {
        result.rect(8 + i * 2, y + 8 + i * 2, 2, 2, ink);
        result.rect(18 - i * 2, y + 8 + i * 2, 2, 2, ink);
      }
    } else {
      result.rect(8, y + 17, 12, 2, ink);
    }
  }
  return result;
}

for (const [name, art] of [
  ["titlebar.png", header],
  ["titlebar-close.png", button("close")],
  ["titlebar-minimize.png", button("minimize")],
]) {
  fs.writeFileSync(path.join(__dirname, name), encodePng(art.data, art.width, art.height));
}
