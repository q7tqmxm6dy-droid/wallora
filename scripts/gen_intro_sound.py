#!/usr/bin/env python3
"""Synthesises the short intro sound (app/src/main/res/raw/intro_chime.wav). No assets, no numpy: pure maths, so the
sound is original and royalty free. "Glass swell": an airy rise while the W draws (0.2-1.3 s), then one clear glass
ping as the W lands (1.25 s). Matches the timeline of IntroSplash."""
import math, os, random, struct, wave

SR = 32000
DUR = 2.6
N = int(SR * DUR)
buf = [0.0] * N
random.seed(3)

def add(t0, samples, gain=1.0):
    i0 = int(t0 * SR)
    for i, v in enumerate(samples):
        if i0 + i < N:
            buf[i0 + i] += v * gain

def tone(freq, length, decay, harm=((1, 1.0),), attack=0.004):
    out = []
    for i in range(int(length * SR)):
        t = i / SR
        env = min(1, t / attack) * math.exp(-t * decay)
        out.append(env * sum(a * math.sin(2 * math.pi * freq * m * t) for m, a in harm))
    return out

def sweep(f0, f1, length, level):
    """Airy noise: a one-pole low-pass whose cutoff glides from f0 to f1, with a swelling volume."""
    out, y, n = [], 0.0, int(length * SR)
    for i in range(n):
        p = i / n
        f = f0 + (f1 - f0) * p
        y += (1 - math.exp(-2 * math.pi * f / SR)) * (random.uniform(-1, 1) - y)
        out.append(y * (math.sin(math.pi * p / 2) ** 2) * level * 3)
    return out

add(0.2, sweep(250, 3200, 1.1, 0.12))
add(1.25, tone(1318.5, 1.3, 3.2, ((1, 1.0), (2.76, 0.35), (5.4, 0.12))), 0.8)
add(1.25, tone(659.25, 1.3, 3.5, ((1, 1.0), (2, 0.2))), 0.35)

peak = max(abs(v) for v in buf) or 1.0
fade = int(0.1 * SR)
for i in range(fade):
    buf[N - 1 - i] *= i / fade

out = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "res", "raw", "intro_chime.wav")
with wave.open(out, "wb") as w:
    w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR)
    w.writeframes(b"".join(struct.pack("<h", int(max(-1, min(1, v * 0.9 / peak)) * 32767)) for v in buf))
print("wrote", os.path.abspath(out), os.path.getsize(out) // 1024, "KB")
