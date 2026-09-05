"""Generate the original Thunder Stack audio pack.

The output is deterministic PCM WAV so Android can decode it without codecs,
and the source can be regenerated whenever the mix is adjusted.
"""

from __future__ import annotations

import math
import random
import struct
import wave
from pathlib import Path


RATE = 44_100
ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "app" / "src" / "main" / "res" / "raw"
TAU = math.tau


def buffer(seconds: float) -> list[float]:
    return [0.0] * int(RATE * seconds)


def envelope(t: float, duration: float, attack: float, release: float) -> float:
    if t < 0.0 or t >= duration:
        return 0.0
    a = min(1.0, t / max(attack, 1e-5))
    r = min(1.0, (duration - t) / max(release, 1e-5))
    return a * r


def add_tone(
    out: list[float],
    start: float,
    duration: float,
    frequency: float,
    gain: float,
    *,
    end_frequency: float | None = None,
    attack: float = 0.004,
    release: float = 0.08,
    harmonics: tuple[tuple[float, float], ...] = (),
) -> None:
    first = int(start * RATE)
    count = min(int(duration * RATE), len(out) - first)
    if count <= 0:
        return
    end_frequency = end_frequency if end_frequency is not None else frequency
    phase = 0.0
    for i in range(count):
        t = i / RATE
        p = t / duration
        freq = frequency + (end_frequency - frequency) * p
        phase += TAU * freq / RATE
        sample = math.sin(phase)
        for multiple, level in harmonics:
            sample += math.sin(phase * multiple) * level
        out[first + i] += sample * gain * envelope(t, duration, attack, release)


def add_noise(
    out: list[float],
    start: float,
    duration: float,
    gain: float,
    rng: random.Random,
    *,
    attack: float = 0.002,
    release: float = 0.12,
    smoothing: float = 0.82,
    decay_power: float = 1.0,
) -> None:
    first = int(start * RATE)
    count = min(int(duration * RATE), len(out) - first)
    value = 0.0
    for i in range(max(0, count)):
        t = i / RATE
        value = value * smoothing + (rng.random() * 2.0 - 1.0) * (1.0 - smoothing)
        decay = max(0.0, 1.0 - t / duration) ** decay_power
        out[first + i] += value * gain * decay * envelope(t, duration, attack, release)


def normalize(samples: list[float], peak: float = 0.88) -> list[float]:
    current = max((abs(value) for value in samples), default=1.0)
    scale = peak / max(current, 1e-6)
    return [math.tanh(value * scale * 1.08) / math.tanh(1.08) * peak for value in samples]


def write_mono(name: str, samples: list[float], peak: float = 0.88) -> None:
    OUTPUT.mkdir(parents=True, exist_ok=True)
    samples = normalize(samples, peak)
    with wave.open(str(OUTPUT / name), "wb") as target:
        target.setnchannels(1)
        target.setsampwidth(2)
        target.setframerate(RATE)
        target.writeframes(b"".join(struct.pack("<h", int(value * 32767)) for value in samples))


def select() -> list[float]:
    out = buffer(0.13)
    add_tone(out, 0.0, 0.09, 920, 0.45, end_frequency=1320, release=0.055, harmonics=((2, 0.18),))
    add_tone(out, 0.018, 0.09, 1840, 0.20, end_frequency=2320, release=0.06)
    return out


def place() -> list[float]:
    out, rng = buffer(0.24), random.Random(101)
    add_noise(out, 0.0, 0.18, 0.85, rng, release=0.12, smoothing=0.88, decay_power=1.7)
    add_tone(out, 0.0, 0.20, 118, 0.72, end_frequency=72, attack=0.001, release=0.15, harmonics=((2, 0.24),))
    add_tone(out, 0.012, 0.11, 520, 0.22, end_frequency=330, release=0.08)
    return out


def perfect() -> list[float]:
    out, rng = buffer(0.72), random.Random(202)
    for start, note, gain in ((0.0, 659.25, 0.44), (0.10, 830.61, 0.40), (0.21, 987.77, 0.38), (0.32, 1318.51, 0.31)):
        add_tone(out, start, 0.36, note, gain, attack=0.004, release=0.28, harmonics=((2, 0.13), (3, 0.05)))
    add_noise(out, 0.10, 0.48, 0.18, rng, attack=0.01, release=0.3, smoothing=0.30, decay_power=1.4)
    return out


def crooked() -> list[float]:
    out, rng = buffer(0.38), random.Random(303)
    add_noise(out, 0.0, 0.31, 0.68, rng, release=0.20, smoothing=0.94, decay_power=0.8)
    add_tone(out, 0.0, 0.34, 196, 0.52, end_frequency=112, release=0.20, harmonics=((1.48, 0.25), (2.03, 0.15)))
    add_tone(out, 0.05, 0.23, 78, 0.36, end_frequency=55, release=0.17)
    return out


def collapse() -> list[float]:
    out, rng = buffer(1.36), random.Random(404)
    add_tone(out, 0.0, 1.25, 92, 0.78, end_frequency=31, attack=0.008, release=0.44, harmonics=((1.5, 0.28), (2, 0.18)))
    add_noise(out, 0.0, 1.28, 1.15, rng, attack=0.004, release=0.42, smoothing=0.91, decay_power=0.55)
    for start, freq, gain in ((0.12, 138, 0.45), (0.30, 103, 0.54), (0.52, 74, 0.59), (0.76, 48, 0.64)):
        add_tone(out, start, 0.34, freq, gain, end_frequency=freq * 0.55, release=0.25)
        add_noise(out, start, 0.26, gain * 0.62, rng, release=0.19, smoothing=0.72, decay_power=1.7)
    return out


def reward() -> list[float]:
    out = buffer(0.82)
    for start, note, gain in ((0.0, 523.25, 0.38), (0.09, 659.25, 0.39), (0.18, 783.99, 0.40), (0.29, 1046.5, 0.43)):
        add_tone(out, start, 0.43, note, gain, release=0.34, harmonics=((2, 0.16), (3, 0.05)))
    add_tone(out, 0.30, 0.45, 2093.0, 0.14, release=0.4)
    return out


def error() -> list[float]:
    out = buffer(0.31)
    add_tone(out, 0.0, 0.27, 230, 0.54, end_frequency=118, attack=0.003, release=0.13, harmonics=((1.07, 0.34), (2, 0.12)))
    return out


def booster() -> list[float]:
    out, rng = buffer(0.72), random.Random(505)
    add_noise(out, 0.0, 0.48, 0.42, rng, attack=0.001, release=0.31, smoothing=0.18, decay_power=1.5)
    add_tone(out, 0.0, 0.54, 180, 0.53, end_frequency=1480, attack=0.002, release=0.24, harmonics=((2, 0.12),))
    add_tone(out, 0.10, 0.50, 740, 0.34, end_frequency=370, release=0.35, harmonics=((1.5, 0.15),))
    return out


def victory() -> list[float]:
    out = buffer(1.28)
    sequence = ((0.00, 392.00), (0.13, 523.25), (0.26, 659.25), (0.40, 783.99), (0.57, 1046.50))
    for start, note in sequence:
        add_tone(out, start, 0.62, note, 0.34, release=0.48, harmonics=((2, 0.15), (3, 0.05)))
    add_tone(out, 0.57, 0.68, 1567.98, 0.20, release=0.58)
    return out


def ambience() -> tuple[list[float], list[float]]:
    seconds = 16.0
    count = int(RATE * seconds)
    left = [0.0] * count
    right = [0.0] * count
    chord_a = (55.0, 82.5, 110.0, 165.0, 220.0)
    chord_b = (55.0, 82.5, 123.75, 165.0, 247.5)
    for i in range(count):
        t = i / RATE
        cross = 0.5 - 0.5 * math.cos(TAU * t / seconds)
        breath = 0.72 + 0.16 * math.sin(TAU * t / 8.0) + 0.08 * math.sin(TAU * t / 2.0)
        a = sum(math.sin(TAU * f * t + index * 0.37) / (index + 1.4) for index, f in enumerate(chord_a))
        b = sum(math.sin(TAU * f * t + index * 0.41 + 0.7) / (index + 1.5) for index, f in enumerate(chord_b))
        pad = (a * (1.0 - cross) + b * cross) * breath * 0.16
        air_l = (math.sin(TAU * 0.5 * t) + 0.55 * math.sin(TAU * 1.25 * t + 1.1) + 0.3 * math.sin(TAU * 3.0 * t)) * 0.025
        air_r = (math.sin(TAU * 0.5 * t + 0.8) + 0.55 * math.sin(TAU * 1.25 * t + 2.0) + 0.3 * math.sin(TAU * 3.0 * t + 1.4)) * 0.025
        shimmer = math.sin(TAU * 440.0 * t + 0.5 * math.sin(TAU * t / 4.0)) * (0.012 + 0.008 * math.sin(TAU * t / 8.0))
        left[i] = pad + air_l + shimmer
        right[i] = pad * 0.96 + air_r + shimmer * 0.82
    return normalize(left, 0.62), normalize(right, 0.62)


def write_stereo(name: str, left: list[float], right: list[float]) -> None:
    OUTPUT.mkdir(parents=True, exist_ok=True)
    frames = bytearray()
    for l_value, r_value in zip(left, right):
        frames.extend(struct.pack("<hh", int(l_value * 32767), int(r_value * 32767)))
    with wave.open(str(OUTPUT / name), "wb") as target:
        target.setnchannels(2)
        target.setsampwidth(2)
        target.setframerate(RATE)
        target.writeframes(frames)


def main() -> None:
    sounds = {
        "sfx_select.wav": select(),
        "sfx_place.wav": place(),
        "sfx_perfect.wav": perfect(),
        "sfx_crooked.wav": crooked(),
        "sfx_collapse.wav": collapse(),
        "sfx_reward.wav": reward(),
        "sfx_error.wav": error(),
        "sfx_booster.wav": booster(),
        "sfx_victory.wav": victory(),
    }
    for name, samples in sounds.items():
        write_mono(name, samples)
    write_stereo("music_olympus.wav", *ambience())
    for path in sorted(OUTPUT.glob("*.wav")):
        print(f"{path.name}: {path.stat().st_size / 1024:.1f} KiB")


if __name__ == "__main__":
    main()
