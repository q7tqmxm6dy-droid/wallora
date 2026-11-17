#!/usr/bin/env python3
"""Synthesises the short intro sound (app/src/main/res/raw/intro_chime.wav). No assets, no numpy: pure maths, so the
sound is original and royalty free. Timeline matches IntroSplash: rising sweep at 0 s, soft bells while the W draws
(0.4-1.1 s), a sparkle on the shine (1.23 s) and an airy whoosh as the halves part (2.75 s)."""
import math, random, struct, wave, os

SR = 32000
DUR = 3.5
N = int(SR * DUR)
buf = [0.0] * N
random.seed(7)

def add(t0, samples, gain=1.0):
    i0 = int(t0 * SR)
    for i, v in enumerate(samples):
        if 0 <= i0 + i < N:
            buf[i0 + i] += v * gain

def bell(freq, length=1.4, bright=1.0):
    out = []
    partials = [(1.0, 1.0, 1.0), (2.0, 0.45 * bright, 1.6), (3.01, 0.22 * bright, 2.4), (4.2, 0.1 * bright, 3.2)]
    for i in range(int(length * SR)):
        t = i / SR
        env = min(1.0, t / 0.006)  # soft attack
        v = 0.0
        for mult, amp, dec in partials:
            v += amp * math.sin(2 * math.pi * freq * mult * t) * math.exp(-t * dec * 2.2)
        out.append(v * env)
    return out

def sweep(f0, f1, length, level, rising=True):
    """Airy noise sweep: one-pole low-pass whose cutoff glides from f0 to f1, with a smooth swell."""
    out, y = [], 0.0
    n = int(length * SR)
    for i in range(n):
        p = i / n
        f = f0 + (f1 - f0) * (p if rising else p)
        a = 1 - math.exp(-2 * math.pi * f / SR)
        y += a * (random.uniform(-1, 1) - y)
        env = math.sin(math.pi * p) ** 2
        out.append(y * env * level * 3.0)
    return out

# 0.00 s  rising shimmer under the seam opening
add(0.0, sweep(300, 2400, 0.9, 0.10))
# 0.40-1.10 s  three soft bells as the W draws (C5 E5 G5)
for t, f in [(0.40, 523.25), (0.70, 659.25), (1.00, 783.99)]:
    add(t, bell(f, 1.1), 0.20)
# 1.23 s  sparkle on the shine: C6 + G6 with a little extra brightness
add(1.23, bell(1046.5, 1.6, 1.3), 0.24)
add(1.27, bell(1567.98, 1.4, 1.3), 0.14)
add(1.34, bell(2093.0, 1.0, 1.0), 0.07)
# 2.70 s  airy whoosh as the halves slide apart
add(2.70, sweep(2600, 500, 0.75, 0.12, rising=False))

peak = max(abs(v) for v in buf) or 1.0
scale = 0.9 / peak  # fairly loud; the app plays it at the phone media volume
# gentle fade out at the very end
fade = int(0.08 * SR)
for i in range(fade):
    buf[N - 1 - i] *= i / fade

out = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "res", "raw", "intro_chime.wav")
with wave.open(out, "wb") as w:
    w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR)
    w.writeframes(b"".join(struct.pack("<h", int(max(-1, min(1, v * scale)) * 32767)) for v in buf))
print("wrote", os.path.abspath(out), os.path.getsize(out) // 1024, "KB")
