// On-device wallpaper generator for the Create tab: the same four styles and eight palettes as the Android app.
// Everything is placed in proportion to the output size and driven by one seeded RNG, so the small preview and the
// full-size export of the same settings are the same picture.
import { rng } from './core.js';

export const STYLES = ['aura', 'gradient', 'waves', 'orbs'];
export const PALETTES = [
  { name: 'Sunset', colors: ['#2B1055', '#7B2F8E', '#FF6B6B', '#FFB86B'] },
  { name: 'Ocean', colors: ['#021B3A', '#0B5FA5', '#22C1C3', '#B8F2E6'] },
  { name: 'Aurora', colors: ['#04122B', '#1B6B5A', '#3DDC97', '#8E7DFF'] },
  { name: 'Bubblegum', colors: ['#3A1C5B', '#D65DB1', '#FF9AC1', '#FFE3F1'] },
  { name: 'Ember', colors: ['#140808', '#7A1F1F', '#E8542B', '#FFC857'] },
  { name: 'Mono', colors: ['#050505', '#2A2D34', '#6B7280', '#E5E7EB'] },
  { name: 'Forest', colors: ['#07140D', '#1E5631', '#6FBF73', '#E9F5B8'] },
  { name: 'Candy', colors: ['#1B1B3A', '#5B5BD6', '#45D9C6', '#FFD6A5'] },
];

const rgba = (hex, a) => { const n = parseInt(hex.slice(1), 16); return `rgba(${n >> 16},${(n >> 8) & 255},${n & 255},${a})`; };

function background(ctx, c, w, h) {
  const g = ctx.createLinearGradient(0, 0, 0, h); g.addColorStop(0, c[0]); g.addColorStop(1, c[1]);
  ctx.fillStyle = g; ctx.fillRect(0, 0, w, h);
}

function radialBloom(ctx, x, y, r, color, a) {
  const g = ctx.createRadialGradient(x, y, 0, x, y, r); g.addColorStop(0, rgba(color, a)); g.addColorStop(1, rgba(color, 0));
  ctx.fillStyle = g; ctx.beginPath(); ctx.arc(x, y, r, 0, Math.PI * 2); ctx.fill();
}

function aura(ctx, c, r, w, h) {
  background(ctx, c, w, h);
  for (let i = 0; i < 6; i++) radialBloom(ctx, r() * w, r() * h, w * (0.55 + r() * 0.6), c[1 + (i % (c.length - 1))], 0.9);
}

function gradient(ctx, c, r, w, h) {
  const angle = r() * Math.PI * 2, len = (w + h) / 2, cx = w / 2, cy = h / 2;
  const g = ctx.createLinearGradient(cx - Math.cos(angle) * len, cy - Math.sin(angle) * len, cx + Math.cos(angle) * len, cy + Math.sin(angle) * len);
  c.forEach((col, i) => g.addColorStop(i / (c.length - 1), col));
  ctx.fillStyle = g; ctx.fillRect(0, 0, w, h);
  radialBloom(ctx, r() * w, r() * h * 0.6, w * 0.8, '#FFFFFF', 0.28);
}

function waves(ctx, c, r, w, h) {
  background(ctx, c, w, h);
  const layers = 7;
  for (let i = 0; i < layers; i++) {
    const t = i / (layers - 1), base = h * (0.30 + 0.62 * t), amp = h * (0.02 + 0.045 * (1 - t) + r() * 0.02);
    const f1 = (2 * Math.PI * (1.1 + r() * 1.4)) / w, f2 = f1 * (1.8 + r()), ph = r() * 6.28;
    ctx.beginPath(); ctx.moveTo(0, h);
    for (let x = 0; x <= w; x += w / 90) ctx.lineTo(x, base + amp * Math.sin(x * f1 + ph) + amp * 0.4 * Math.sin(x * f2 + ph * 2));
    ctx.lineTo(w, h); ctx.closePath();
    const g = ctx.createLinearGradient(0, base - amp, 0, h);
    g.addColorStop(0, rgba(c[1 + (i % (c.length - 1))], 0.55 + 0.3 * t)); g.addColorStop(1, rgba(c[0], 0.85));
    ctx.fillStyle = g; ctx.fill();
  }
}

function orbs(ctx, c, r, w, h) {
  background(ctx, c, w, h);
  for (let i = 0; i < 7; i++) {
    const color = c[1 + (i % (c.length - 1))], rad = w * (0.09 + r() * 0.2), cx = r() * w, cy = h * (0.12 + r() * 0.76);
    radialBloom(ctx, cx, cy, rad * 2.4, color, 0.35);
    const fx = cx - rad * 0.35, fy = cy - rad * 0.4, g = ctx.createRadialGradient(fx, fy, 0, fx, fy, rad * 1.25);
    g.addColorStop(0, '#FFFFFF'); g.addColorStop(0.45, color); g.addColorStop(1, c[0]);
    ctx.fillStyle = g; ctx.beginPath(); ctx.arc(cx, cy, rad, 0, Math.PI * 2); ctx.fill();
  }
}

function addGrain(ctx, amount, seed, w, h) {
  const size = 128, tile = document.createElement('canvas'); tile.width = tile.height = size;
  const tctx = tile.getContext('2d'), img = tctx.createImageData(size, size), r = rng(seed);
  for (let i = 0; i < size * size; i++) { const v = Math.floor(r() * 256); img.data.set([v, v, v, 255], i * 4); }
  tctx.putImageData(img, 0, 0);
  ctx.save(); ctx.globalAlpha = Math.min(0.47, amount * 0.27); ctx.globalCompositeOperation = 'overlay';
  ctx.fillStyle = ctx.createPattern(tile, 'repeat'); ctx.fillRect(0, 0, w, h); ctx.restore();
}

/** Draws a wallpaper onto [canvas] at [width] x [height]. */
export function render(canvas, style, palette, seed, grain, width, height) {
  canvas.width = width; canvas.height = height;
  const ctx = canvas.getContext('2d'), r = rng(seed * 7919 + STYLES.indexOf(style) * 104729);
  ({ aura, gradient, waves, orbs })[style](ctx, palette.colors, r, width, height);
  if (grain > 0) addGrain(ctx, grain, seed, width, height);
  return canvas;
}
