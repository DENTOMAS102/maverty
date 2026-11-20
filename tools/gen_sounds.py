#!/usr/bin/env python3
"""Synthesises every Goddess of Death sound from scratch and encodes it as mono Ogg Vorbis.

    python3 tools/gen_sounds.py

Requires numpy, scipy and ffmpeg (with libvorbis). Output is deterministic.
"""
import os
import subprocess
import tempfile

import numpy as np
from scipy.io import wavfile
from scipy.signal import butter, fftconvolve, sosfilt

SR = 44100
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, 'src/main/resources/assets/deathgoddess/sounds/goddess')
TAU = 2 * np.pi


def tt(d):
    return np.arange(int(SR * d)) / SR


def noise(d, rng):
    return rng.standard_normal(int(SR * d))


def bp(x, lo, hi, order=2):
    return sosfilt(butter(order, [lo, hi], 'bandpass', fs=SR, output='sos'), x)


def lp(x, f, order=2):
    return sosfilt(butter(order, f, 'lowpass', fs=SR, output='sos'), x)


def hp(x, f, order=2):
    return sosfilt(butter(order, f, 'highpass', fs=SR, output='sos'), x)


def fade(x, a=0.005, r=0.05):
    x = x.copy()
    na, nr = int(SR * a), int(SR * r)
    if na:
        x[:na] *= np.linspace(0, 1, na)
    if nr:
        x[-nr:] *= np.linspace(1, 0, nr)
    return x


def swell(n, peak, sharp=2.0):
    """Envelope rising to `peak` (0..1 of the length) and falling away."""
    x = np.linspace(0, 1, n)
    return np.where(x < peak, (x / peak) ** sharp, ((1 - x) / (1 - peak)) ** sharp)


def phase_of(freq):
    return TAU * np.cumsum(freq) / SR


def saw(ph):
    return 2 * ((ph / TAU) % 1) - 1


def bell(f, d, rng, decay=3.0, partials=((1, 1), (2.0, 0.6), (2.76, 0.45), (5.4, 0.25), (8.93, 0.12))):
    t = tt(d)
    out = np.zeros_like(t)
    for ratio, amp in partials:
        out += amp * np.sin(TAU * f * ratio * t + rng.uniform(0, TAU)) * np.exp(-t * decay * ratio ** 0.6)
    return out


def pluck(f, d, rng, damp=0.995):
    """Karplus-Strong string: the sound of a taut thread."""
    n = int(SR * d)
    period = max(2, int(SR / f))
    buf = rng.uniform(-1, 1, period)
    out = np.empty(n)
    for i in range(n):
        out[i] = buf[i % period]
        buf[i % period] = damp * 0.5 * (buf[i % period] + buf[(i + 1) % period])
    return out


def formant(x, freqs, width=0.12):
    return sum(bp(x, f * (1 - width), f * (1 + width)) * a for f, a in freqs)


def reverb(x, rng, dur=1.6, wet=0.35, tone=3200, decay=4.0):
    t = tt(dur)
    ir = lp(rng.standard_normal(len(t)) * np.exp(-t * decay), tone)
    ir /= np.sqrt(np.sum(ir ** 2))
    tail = fftconvolve(x, ir)
    out = np.zeros(len(tail))
    out[:len(x)] = x
    return out + wet * tail


def mix(d, *layers):
    out = np.zeros(int(SR * d))
    for at, sig in layers:
        i = int(SR * at)
        sig = sig[:max(0, len(out) - i)]
        out[i:i + len(sig)] += sig
    return out


def shatter(d, rng, clicks=5):
    """Porcelain cracking: sharp clicks over a short ceramic ring."""
    out = np.zeros(int(SR * d))
    for _ in range(clicks):
        i = int(rng.uniform(0, 0.07) * SR)
        c = hp(noise(0.012, rng), 2500) * np.exp(-tt(0.012) * 400)
        out[i:i + len(c)] += c * rng.uniform(0.5, 1.0)
    t = tt(d)
    for f in (1830, 2940, 4310, 6120):
        out += 0.25 * np.sin(TAU * f * rng.uniform(0.97, 1.03) * t) * np.exp(-t * rng.uniform(18, 40))
    return out


def whisper(d, rng, center=1600):
    """Breath broken into syllables by an irregular envelope."""
    n = int(SR * d)
    env = np.interp(np.arange(n), np.linspace(0, n, int(d * 7) + 2), rng.random(int(d * 7) + 2)) ** 2.2
    sweep = np.interp(np.arange(n), np.linspace(0, n, 6), rng.uniform(0.7, 1.4, 6))
    x = noise(d, rng)
    return (bp(x, center * 0.6, center * 1.5) + 0.5 * bp(x, center * 2.2, center * 3.4)) * env * sweep


def chains(d, rng, hits=6):
    out = np.zeros(int(SR * d))
    for _ in range(hits):
        at = rng.uniform(0, d * 0.8)
        f = rng.uniform(2800, 6500)
        ping = bell(f, 0.12, rng, decay=45, partials=((1, 1), (1.51, 0.6), (2.32, 0.4)))
        i = int(at * SR)
        ping = ping[:len(out) - i]
        out[i:i + len(ping)] += ping * rng.uniform(0.3, 1.0)
    return out


def drone(d, rng, f=55.0):
    t = tt(d)
    out = np.zeros_like(t)
    for ratio, amp in ((1, 1), (1.498, 0.5), (2.01, 0.35), (2.67, 0.2)):
        out += amp * np.sin(TAU * f * ratio * t + rng.uniform(0, TAU)) * (0.8 + 0.2 * np.sin(TAU * rng.uniform(0.2, 0.6) * t))
    return out


# ------------------------------------------------------------------ the sounds
def ambient(seed):
    rng = np.random.default_rng(seed)
    d = 4.5
    n = int(SR * d)
    body = drone(d, rng, 49 + seed * 3) * swell(n, 0.45, 1.5) * 0.5
    breath = whisper(d, rng, 1300 + 250 * seed) * swell(n, 0.4, 1.2) * 0.9
    tinkle = mix(d, *[(rng.uniform(0.4, 3.2), bell(rng.uniform(2200, 4200), 0.9, rng, decay=7) * 0.12) for _ in range(3)])
    return reverb(fade(body + breath + tinkle, 0.3, 0.6), rng, 2.2, 0.5)


def hurt(seed):
    rng = np.random.default_rng(10 + seed)
    d = 0.7
    t = tt(d)
    crack = shatter(d, rng, 4 + seed)
    thud = np.sin(TAU * 88 * t) * np.exp(-t * 16) * 0.9
    ph = phase_of(np.linspace(250 + 30 * seed, 190, len(t)))
    moan = formant(saw(ph) + saw(ph * 1.059), ((700, 1.0), (1150, 0.6), (2600, 0.3))) * np.exp(-t * 6) * 0.5
    return reverb(fade(crack * 0.8 + thud + moan), rng, 0.8, 0.25)


def death():
    rng = np.random.default_rng(20)
    d = 5.0
    n = int(SR * d)
    t = tt(d)
    freq = np.interp(t, [0, 0.4, 3.6, 5.0], [300, 420, 120, 70])
    vib = 1 + 0.02 * np.sin(TAU * 5.5 * t)
    ph = phase_of(freq * vib)
    wail = formant(saw(ph) + saw(ph * 1.012) + saw(ph * 0.5), ((650, 1.0), (1080, 0.7), (2450, 0.35), (3400, 0.15)))
    wail *= np.interp(t, [0, 0.3, 2.5, 4.2, 5.0], [0, 1, 0.8, 0.25, 0]) * 0.45
    cuts = [0.5, 0.9, 1.2, 1.5, 1.8, 2.1]                    # one string snapping per arm, as animated
    snaps = mix(d, *[(c, fade(pluck(880 * rng.uniform(0.7, 1.5), 0.5, rng, 0.99), 0.0, 0.1) * 0.5) for c in cuts])
    cracks = mix(d, *[(c + 0.03, shatter(0.4, rng, 3) * 0.3) for c in cuts], (2.5, shatter(0.6, rng, 9) * 0.6))
    toll = mix(d, (3.3, bell(62, 1.7, rng, decay=1.6) * 0.9))
    hiss = bp(noise(d, rng), 500, 3000) * np.interp(t, [0, 2.5, 3.4, 5.0], [0, 0.05, 0.35, 0])
    return reverb(fade(wail + snaps + cracks + toll + hiss, 0.01, 0.4), rng, 2.6, 0.5)


def spawn():
    rng = np.random.default_rng(30)
    d = 3.5
    n = int(SR * d)
    t = tt(d)
    rise = bp(noise(d, rng), 200, 2400) * np.interp(t, [0, 2.1, 2.25, 3.5], [0, 0.9, 0.1, 0]) ** 2
    cluster = sum(np.sin(phase_of(np.interp(t, [0, 2.2, 3.5], [f * 0.5, f, f]))) for f in (110, 116.5, 164.8, 233))
    cluster *= np.interp(t, [0, 2.2, 3.5], [0, 0.22, 0])
    tolls = mix(d, (0.0, bell(55, 2.2, rng, decay=1.3) * 0.9), (2.2, bell(73.4, 1.3, rng, decay=1.8)))
    return reverb(fade(rise * 0.6 + cluster + tolls + whisper(d, rng, 1500) * 0.35 * swell(n, 0.6), 0.01, 0.3),
                  rng, 2.4, 0.5)


def glide():
    rng = np.random.default_rng(40)
    d = 1.2
    n = int(SR * d)
    env = np.interp(np.arange(n), np.linspace(0, n, 14), rng.random(14)) ** 1.5 * swell(n, 0.4, 0.8)
    cloth = bp(noise(d, rng), 1400, 6000) * env
    return fade(cloth * 0.9 + chains(d, rng, 3) * 0.12, 0.05, 0.2)


def sweep():
    rng = np.random.default_rng(50)
    d = 0.8
    n = int(SR * d)
    x = noise(d, rng)
    env = swell(n, 0.35, 2.5)
    # Crossfade three bands to fake a filter sweeping up and back down.
    whoosh = (bp(x, 300, 900) * np.interp(np.arange(n), [0, n * 0.3, n], [1, 0.3, 0.8])
              + bp(x, 900, 2600) * swell(n, 0.35, 1.5) * 1.3 + bp(x, 2600, 7000) * swell(n, 0.38, 4) * 0.8) * env
    return reverb(fade(whoosh + mix(d, (0.22, chains(0.5, rng, 7) * 0.22))), rng, 0.6, 0.2)


def eye_cast():
    rng = np.random.default_rng(60)
    d = 0.6
    t = tt(d)
    blip = np.sin(phase_of(np.interp(t, [0, 0.12, d], [260, 940, 620]))) * np.exp(-t * 9)
    wet = lp(noise(d, rng), 900) * np.exp(-t * 22) * 1.2
    air = bp(noise(d, rng), 1800, 5200) * swell(len(t), 0.3, 2) * 0.35
    return reverb(fade(blip * 0.6 + wet + air), rng, 0.7, 0.3)


def eye_hit():
    rng = np.random.default_rng(70)
    d = 0.45
    t = tt(d)
    squelch = lp(noise(d, rng), 1400) * np.exp(-t * 16) * (1 + 0.6 * np.sin(TAU * 38 * t))
    drop = np.sin(phase_of(np.interp(t, [0, d], [520, 110]))) * np.exp(-t * 11)
    return fade(squelch + drop * 0.6 + shatter(d, rng, 2) * 0.2)


def gaze_charge():
    rng = np.random.default_rng(80)
    d = 2.0                                                  # the gaze lands on tick 40
    n = int(SR * d)
    t = tt(d)
    grow = np.linspace(0, 1, n)
    out = np.zeros(n)
    for ratio in (1, 1.06, 1.5, 2.12, 3.02):
        f = np.interp(t, [0, d], [110 * ratio, 330 * ratio])
        trem = 1 + 0.4 * np.sin(phase_of(np.interp(t, [0, d], [3, 22])) + rng.uniform(0, TAU))
        out += np.sin(phase_of(f)) * trem / ratio
    riser = bp(noise(d, rng), 600, 5000) * grow ** 3
    reverse_bell = bell(880, d, rng, decay=2.5)[::-1] * 0.5
    return fade(out * grow ** 1.5 * 0.4 + riser * 0.5 + reverse_bell + whisper(d, rng, 2000) * grow * 0.5, 0.05, 0.01)


def gaze_release():
    rng = np.random.default_rng(90)
    d = 1.6
    t = tt(d)
    boom = np.sin(phase_of(np.interp(t, [0, 0.5, d], [62, 38, 32]))) * np.exp(-t * 3.2) * 1.4
    crack = hp(noise(0.08, rng), 1500) * np.exp(-tt(0.08) * 60)
    ring = bell(1210, d, rng, decay=4, partials=((1, 1), (1.48, 0.7), (2.71, 0.4), (4.1, 0.2))) * 0.35
    return reverb(fade(mix(d, (0, boom), (0, crack), (0, ring)), 0.001, 0.3), rng, 2.0, 0.45)


def strings():
    rng = np.random.default_rng(100)
    d = 1.3
    creak_t = tt(0.75)
    creak = saw(phase_of(np.interp(creak_t, [0, 0.75], [70, 190]))) * (np.sin(TAU * 31 * creak_t) > 0.2)
    creak = bp(creak, 500, 2500) * np.linspace(0.1, 1, len(creak_t)) * 0.5
    # Server yanks the target 16 ticks after this starts.
    twang = mix(d, *[(0.78 + i * 0.035, fade(pluck(f, 0.45, rng, 0.992), 0, 0.1) * 0.6)
                     for i, f in enumerate((660, 990, 1320, 1485))])
    return reverb(fade(mix(d, (0, creak)) + twang), rng, 0.9, 0.3)


def veil_step():
    rng = np.random.default_rng(110)
    d = 1.0
    n = int(SR * d)
    t = tt(d)
    suck = bp(noise(d, rng), 300, 3200) * np.interp(t, [0, 0.72, 0.78, d], [0, 1, 0.05, 0]) ** 2
    flutter = 1 + 0.6 * np.sin(TAU * 24 * t)
    thump = mix(d, (0.76, np.sin(TAU * 70 * tt(0.22)) * np.exp(-tt(0.22) * 20)))
    return reverb(fade(suck * flutter + thump), rng, 0.8, 0.3)


def phase():
    rng = np.random.default_rng(120)
    d = 3.5
    n = int(SR * d)
    t = tt(d)
    freq = np.interp(t, [0, 1.4, 1.5, 2.6, 3.5], [260, 520, 740, 690, 400])
    vib = 1 + np.interp(t, [0, 1.5, 3.5], [0.005, 0.04, 0.02]) * np.sin(TAU * 7 * t)
    ph = phase_of(freq * vib)
    raw = saw(ph) + saw(ph * 1.015) + saw(ph * 1.34) + 0.5 * saw(ph * 0.5)
    scream = formant(raw, ((850, 1.0), (1400, 0.9), (2900, 0.6), (3900, 0.3)), 0.16)
    scream = np.tanh(scream * 3) * np.interp(t, [0, 1.35, 1.5, 2.6, 3.5], [0, 0.35, 1, 0.7, 0])
    burst_t = tt(1.4)                                         # second phase bursts out on tick 30
    burst = np.sin(phase_of(np.interp(burst_t, [0, 1.4], [70, 34]))) * np.exp(-burst_t * 3) * 1.3
    burst[:int(0.1 * SR)] += hp(noise(0.1, rng), 1000) * np.exp(-tt(0.1) * 40)
    rattle = bp(noise(d, rng), 700, 4200) * np.interp(t, [0, 1.4, 1.5, 3.5], [0, 0.45, 0.1, 0]) * (np.sin(TAU * 17 * t) > 0)
    return reverb(fade(mix(d, (0, scream * 0.6), (1.5, burst), (0, rattle), (1.5, chains(1.5, rng, 14) * 0.2)), 0.02, 0.3),
                  rng, 2.4, 0.5)


# ------------------------------------------------------------------ death magic
def choir(freq, d, rng, vowels=((650, 1.0), (1080, 0.7), (2450, 0.35))):
    """A breathy, detuned cluster of voices; freq may be a number or a per-sample glide."""
    t = tt(d)
    out = np.zeros_like(t)
    for detune in (0.994, 1.0, 1.007, 0.5, 1.498):
        vibrato = 1 + 0.006 * np.sin(TAU * rng.uniform(4, 6) * t + rng.uniform(0, TAU))
        out += saw(phase_of(freq * detune * vibrato))
    return formant(out, vowels, 0.14) + 0.15 * bp(noise(d, rng), 1500, 5000)


def cast_sigil():
    rng = np.random.default_rng(130)
    d = 1.5
    n = int(SR * d)
    t = tt(d)
    glide = np.interp(t, [0, d], [0.94, 1.0])
    voice = choir(146.8 * glide, d, rng) + 0.7 * choir(207.7 * glide, d, rng)     # a tritone, rising
    rumble = np.sin(phase_of(np.interp(t, [0, d], [40, 62])))
    body = (voice * 0.3 + rumble * 0.5 + whisper(d, rng, 1800) * 0.4) * swell(n, 0.75, 1.6)
    return reverb(fade(body, 0.05, 0.3), rng, 1.6, 0.45)


def cast_blade():
    rng = np.random.default_rng(140)
    d = 1.2
    n = int(SR * d)
    t = tt(d)
    grow = np.linspace(0.1, 1, n)
    scrape = bp(noise(d, rng), 2500, 9000) * (0.5 + 0.5 * np.sin(phase_of(np.interp(t, [0, d], [30, 160])))) * grow ** 2
    ring = sum(np.sin(phase_of(np.interp(t, [0, d], [f * 0.8, f]))) for f in (1870, 2790, 4130)) * grow ** 3 * 0.2
    shing = mix(d, (0.95, bell(3300, 0.25, rng, decay=14, partials=((1, 1), (1.47, 0.6), (2.09, 0.4))) * 0.6))
    low = np.sin(phase_of(np.interp(t, [0, d], [90, 140]))) * swell(n, 0.8) * 0.3
    return reverb(fade(scrape * 0.6 + ring + shing + low, 0.02, 0.1), rng, 1.0, 0.3)


def sigil_erupt(seed):
    rng = np.random.default_rng(150 + seed)
    d = 1.0
    t = tt(d)
    boom = np.sin(phase_of(np.interp(t, [0, 0.3, d], [95 - seed * 8, 48, 36]))) * np.exp(-t * 4.5) * 1.3
    rush = bp(noise(d, rng), 250, 3800) * np.exp(-t * 5) * np.interp(t, [0, 0.04, d], [0, 1, 1])
    crack = hp(noise(0.05, rng), 1800) * np.exp(-tt(0.05) * 70)
    wail = choir(np.interp(t, [0, d], [420 + 40 * seed, 250]), d, rng) * np.exp(-t * 5) * 0.25
    return reverb(fade(mix(d, (0, boom), (0, rush * 0.8), (0, crack * 0.7), (0, wail)), 0.001, 0.2), rng, 1.4, 0.4)


def blade_wave():
    rng = np.random.default_rng(160)
    d = 0.7
    n = int(SR * d)
    t = tt(d)
    x = noise(d, rng)
    slash = (bp(x, 800, 2500) + bp(x, 2500, 9000) * swell(n, 0.15, 3) * 1.4) * swell(n, 0.18, 2.5)
    ring = bell(2450, d, rng, decay=9, partials=((1, 1), (1.5, 0.5), (2.63, 0.35))) * 0.3
    thump = np.sin(TAU * 70 * t) * np.exp(-t * 18) * 0.7
    return reverb(fade(slash + ring + thump), rng, 0.8, 0.25)


def knell():
    rng = np.random.default_rng(170)
    d = 2.2
    strike = hp(noise(0.03, rng), 900) * np.exp(-tt(0.03) * 90)
    tone = bell(82.4, d, rng, decay=1.5,
                partials=((0.5, 0.7), (1, 1), (1.19, 0.5), (2.0, 0.6), (2.51, 0.4), (3.0, 0.25), (4.1, 0.15)))
    wave = bp(noise(0.6, rng), 100, 900) * np.exp(-tt(0.6) * 7) * 0.5
    return reverb(fade(mix(d, (0, tone), (0, strike * 0.6), (0, wave)), 0.001, 0.4), rng, 2.4, 0.5)


def rain_fall():
    rng = np.random.default_rng(180)
    d = 0.6
    fall = tt(0.14)
    whistle = np.sin(phase_of(np.interp(fall, [0, 0.14], [2600, 700]))) * np.linspace(0.2, 1, len(fall)) * 0.35
    thud = np.sin(phase_of(np.interp(tt(0.4), [0, 0.4], [130, 45]))) * np.exp(-tt(0.4) * 12)
    hiss = bp(noise(0.45, rng), 900, 5000) * np.exp(-tt(0.45) * 10) * 0.5
    return reverb(fade(mix(d, (0, whistle), (0.12, thud), (0.12, hiss))), rng, 0.6, 0.25)


def lance_form():
    rng = np.random.default_rng(190)
    d = 0.7
    n = int(SR * d)
    t = tt(d)
    shimmer = sum(np.sin(TAU * f * t + rng.uniform(0, TAU)) * (1 + 0.3 * np.sin(TAU * rng.uniform(9, 17) * t))
                  for f in (1568, 2093, 2349, 3136)) * 0.2
    return reverb(fade(shimmer * swell(n, 0.8, 2) + bell(1568, d, rng, decay=6)[::-1] * 0.6, 0.02, 0.08), rng, 0.8, 0.35)


def lance_fire():
    rng = np.random.default_rng(200)
    d = 0.5
    t = tt(d)
    zipped = np.sin(phase_of(np.interp(t, [0, 0.22, d], [2600, 420, 300]))) * np.exp(-t * 11)
    air = bp(noise(d, rng), 1500, 8000) * np.exp(-t * 14)
    thump = np.sin(TAU * 85 * t) * np.exp(-t * 20) * 0.6
    return reverb(fade(zipped * 0.6 + air * 0.7 + thump, 0.001, 0.1), rng, 0.6, 0.25)


def danse():
    """A wound-down music box playing four bars of a waltz in A minor."""
    rng = np.random.default_rng(210)
    d = 4.2
    n = int(SR * d)
    beat = 0.32

    def note(semitones):
        return 440 * 2 ** (semitones / 12)

    melody = [(0, 12), (1, 7), (2, 3), (3, 15), (4, 14), (5, 12), (6, 11), (7, 7), (8, 2), (9, 12), (10, 15), (11, 19),
              (12, 24)]
    bass = [(0, -12), (3, -17), (6, -17), (9, -12), (12, -24)]
    box = ((1, 1), (2.0, 0.45), (3.01, 0.2), (5.2, 0.06))
    layers = [(b * beat + 0.1, bell(note(s) * rng.uniform(0.996, 1.004), 1.4, rng, decay=4.5, partials=box) * 0.5)
              for b, s in melody]
    layers += [(b * beat + 0.1, bell(note(s), 1.6, rng, decay=3, partials=((1, 1), (2, 0.3))) * 0.6) for b, s in bass]
    under = drone(d, rng, 55) * 0.12 * swell(n, 0.5, 1.2) + whisper(d, rng, 1700) * 0.2 * swell(n, 0.5, 1.2)
    return reverb(fade(mix(d, *layers) + under, 0.01, 0.4), rng, 2.0, 0.45)


def requiem_charge():
    rng = np.random.default_rng(220)
    d = 2.5                                                  # the Requiem lands 2.5 s after this starts
    n = int(SR * d)
    t = tt(d)
    grow = np.linspace(0, 1, n)
    glide = np.interp(t, [0, d], [0.84, 1.0])
    voices = sum(choir(f * glide, d, rng) for f in (110, 164.8, 220, 261.6, 311.1))
    tolls = mix(d, *[(at, bell(110, 0.9, rng, decay=3.5) * 0.5) for at in (0.0, 0.7, 1.25, 1.65, 1.95, 2.15, 2.3)])
    riser = bp(noise(d, rng), 400, 6000) * grow ** 3
    return fade(voices * 0.12 * grow ** 1.3 + tolls + riser * 0.5 + whisper(d, rng, 2200) * grow * 0.4, 0.05, 0.02)


def requiem_release():
    rng = np.random.default_rng(230)
    d = 2.4
    t = tt(d)
    boom = np.sin(phase_of(np.interp(t, [0, 0.6, d], [70, 34, 28]))) * np.exp(-t * 2.2) * 1.5
    crack = hp(noise(0.12, rng), 1200) * np.exp(-tt(0.12) * 35)
    fall = choir(np.interp(t, [0, d], [330, 82]), d, rng) * np.exp(-t * 2.5) * 0.3
    toll = bell(55, d, rng, decay=1.2) * 0.8
    wash = bp(noise(d, rng), 150, 2500) * np.exp(-t * 3) * 0.6
    return reverb(fade(mix(d, (0, boom), (0, crack), (0, fall), (0, toll), (0, wash)), 0.001, 0.4), rng, 2.8, 0.55)


def reap():
    """A life leaving: one short falling breath."""
    rng = np.random.default_rng(240)
    d = 0.8
    n = int(SR * d)
    t = tt(d)
    breath = formant(noise(d, rng), ((900, 1.0), (1500, 0.6), (2800, 0.3)), 0.2) * swell(n, 0.2, 1.5)
    tone = np.sin(phase_of(np.interp(t, [0, d], [620, 240]))) * swell(n, 0.15, 2) * 0.25
    return reverb(fade(breath * 0.8 + tone, 0.01, 0.2), rng, 1.0, 0.4)


def wither():
    """Leaves and grass drying out all at once."""
    rng = np.random.default_rng(250)
    d = 1.0
    n = int(SR * d)
    out = np.zeros(n)
    for _ in range(90):
        i = int(rng.random() ** 1.6 * (n - 400))
        click = hp(noise(0.006, rng), 1800) * np.exp(-tt(0.006) * 500)
        out[i:i + len(click)] += click * rng.uniform(0.3, 1.0)
    hiss = bp(noise(d, rng), 2500, 8000) * np.exp(-tt(d) * 4) * 0.25
    return fade(bp(out, 1200, 9000) + hiss, 0.005, 0.2)


SOUNDS = {
    'ambient1': lambda: ambient(1), 'ambient2': lambda: ambient(2), 'ambient3': lambda: ambient(3),
    'hurt1': lambda: hurt(1), 'hurt2': lambda: hurt(2), 'death': death, 'spawn': spawn, 'glide': glide,
    'sweep': sweep, 'eye_cast': eye_cast, 'eye_hit': eye_hit, 'gaze_charge': gaze_charge,
    'gaze_release': gaze_release, 'strings': strings, 'veil_step': veil_step, 'phase': phase,
    'cast_sigil': cast_sigil, 'cast_blade': cast_blade, 'sigil_erupt1': lambda: sigil_erupt(1),
    'sigil_erupt2': lambda: sigil_erupt(2), 'blade_wave': blade_wave, 'knell': knell, 'rain_fall': rain_fall,
    'lance_form': lance_form, 'lance_fire': lance_fire, 'danse': danse, 'requiem_charge': requiem_charge,
    'requiem_release': requiem_release, 'reap': reap, 'wither': wither,
}


def main():
    os.makedirs(OUT, exist_ok=True)
    with tempfile.TemporaryDirectory() as tmp:
        for name, make in SOUNDS.items():
            x = make()
            x = fade(x / (np.max(np.abs(x)) + 1e-9) * 0.89, 0.002, 0.03)
            wav = os.path.join(tmp, name + '.wav')
            wavfile.write(wav, SR, (x * 32767).astype(np.int16))
            subprocess.run(['ffmpeg', '-v', 'error', '-y', '-i', wav, '-ac', '1', '-c:a', 'libvorbis', '-q:a', '4',
                            '-map_metadata', '-1', '-bitexact', os.path.join(OUT, name + '.ogg')], check=True)
            print(f'{name}: {len(x) / SR:.2f}s')


if __name__ == '__main__':
    main()
