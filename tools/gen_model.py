#!/usr/bin/env python3
"""Single source of truth for the Goddess of Death model.

Defines the part tree, packs UVs, paints the texture atlases and writes the
Java mesh definition, so geometry and textures can never drift apart.

    python3 tools/gen_model.py             # regenerate Java + textures
    python3 tools/gen_model.py --preview   # also render previews to build/preview/

Requires numpy and Pillow.
"""
import math
import os
import sys
import zlib

import numpy as np
from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TEX_DIR = os.path.join(ROOT, 'src/main/resources/assets/deathgoddess/textures/entity')
JAVA_OUT = os.path.join(ROOT, 'src/main/java/com/dentomas/deathgoddess/client/model/GoddessModelLayer.java')

S = 4            # texels per model unit
TEX_W, TEX_H = 256, 256

# ---------------------------------------------------------------- palette
BURG = np.array([59, 7, 19]) / 255
CRIM = np.array([113, 13, 32]) / 255
BLACK = np.array([8, 6, 8]) / 255
DRIED = np.array([74, 14, 18]) / 255
WINE = np.array([88, 24, 40]) / 255
ASH = np.array([120, 114, 116]) / 255
IVORY = np.array([205, 196, 180]) / 255
SILVER = np.array([150, 152, 160]) / 255
DARKMETAL = np.array([24, 22, 27]) / 255
NIGHT = np.array([20, 7, 12]) / 255


def rng_for(name):
    return np.random.default_rng(zlib.crc32(name.encode()))


# ---------------------------------------------------------------- model tree
class Part:
    def __init__(self, name, parent, pivot, rot):
        self.name, self.parent, self.pivot, self.rot = name, parent, pivot, rot
        self.boxes, self.children = [], []


class Box:
    def __init__(self, part, x, y, z, w, h, d, mat, grow, mirror, uvkey, opts):
        self.part = part
        self.o, self.size = (x, y, z), (w, h, d)
        self.mat, self.grow, self.mirror, self.uvkey, self.opts = mat, grow, mirror, uvkey, opts
        self.uv = None


PARTS = {}
ORDER = []
EYES = []   # (x, y, w, h) in halo space, used to paint smoke behind them
REST = {}   # rest rotation of each right arm joint, exported so animations can pose arms absolutely


def part(name, parent, pivot=(0, 0, 0), rot=(0, 0, 0)):
    assert name not in PARTS, name
    p = Part(name, parent, pivot, rot)
    PARTS[name] = p
    ORDER.append(p)
    if parent is not None:
        parent.children.append(p)
    return p


def box(p, x, y, z, w, h, d, mat, grow=0.0, mirror=False, uvkey=None, **opts):
    b = Box(p, x, y, z, w, h, d, mat, grow, mirror, uvkey or f'{p.name}#{len(p.boxes)}', opts)
    p.boxes.append(b)
    return b


def build():
    al = part('all', None, (0, 24, 0))

    # ---- legs (mostly hidden; show through the gown's tears)
    legs = part('legs', al, (0, -34, 0))
    box(legs, -2.1, 0, -1, 2, 32, 2, 'bone', grow=-0.35)
    box(legs, 0.1, 0, -1, 2, 32, 2, 'bone', grow=-0.35, uvkey='legs#0')

    # ---- gown core: three flaring tiers, chained so the hem can trail
    skirt = part('skirt', al, (0, -34, 0))
    box(skirt, -4, -0.5, -3, 8, 11, 6, 'cloth', top=NIGHT, bot=BLACK, folds=0.7)
    mid = part('skirt_mid', skirt, (0, 10, 0))
    box(mid, -6, 0, -4.5, 12, 12, 9, 'cloth', top=NIGHT, bot=BLACK, tear=0.12, holes=2, open_bottom=True)
    low = part('skirt_low', mid, (0, 12, 0))
    box(low, -8.5, 0, -6.5, 17, 11, 13, 'cloth', top=BLACK, bot=NIGHT, tear=0.4, tear_min=0.02, holes=5,
        open_bottom=True, open_top=True)

    # ---- gown overlays: a fan of torn burgundy panels, lace veils, chains
    def panel(name, parent, pivot, rot, bx, mat='cloth', **o):
        p = part(name, parent, pivot, rot)
        box(p, *bx, mat, **o)
        return p

    fan = [(0, 5, 27, 12, CRIM, BURG), (34, 7, 33, 16, BURG, NIGHT), (-36, 7, 31, 17, BURG, DRIED),
           (74, 7, 34, 18, WINE * 0.8, BLACK), (-72, 7, 32, 17, BURG, BLACK), (112, 7, 33, 17, DRIED, NIGHT),
           (-110, 7, 34, 18, BURG, NIGHT), (148, 7, 34, 16, BURG, BLACK), (-146, 7, 32, 17, WINE * 0.7, NIGHT),
           (180, 8, 35, 15, BURG, NIGHT), (18, 4, 22, 21, CRIM * 0.9, DRIED), (-16, 4, 25, 20, DRIED, BURG * 0.8)]
    for i, (yaw, pw, ln, tilt, c0, c1) in enumerate(fan):
        a = math.radians(yaw)
        panel(f'panel_{i}', skirt, (-3.0 * math.sin(a), 0.3, -3.0 * math.cos(a)), (-tilt, yaw, 0),
              (-pw / 2, 0, 0, pw, ln, 0), top=c0, bot=c1, tear=0.3, tear_min=0.05, holes=4, sides=0.22)
    panel('overveil_front', skirt, (0, 3, -4.2), (-19, 0, 0), (-6, 0, 0, 12, 22, 0),
          top=BLACK, bot=BLACK, tear=0.45, tear_min=0.1, holes=4, lace=True, sides=0.3)
    panel('overveil_back', skirt, (0, 3, 4.2), (20, 0, 0), (-7, 0, 0, 14, 26, 0),
          top=BLACK, bot=NIGHT, tear=0.4, tear_min=0.1, holes=5, lace=True, sides=0.3)
    panel('chains_skirt', skirt, (0, 0.2, -4.6), (-17, 0, 0), (-6, 0, 0, 12, 20, 0), mat='chains', arcs=5, drops=6)
    panel('sash_r', skirt, (-3.2, 0, -1.5), (-6, 35, 16), (-2, 0, 0, 4, 20, 0),
          top=CRIM, bot=CRIM * 0.7, tear=0.3, tear_min=0.1, holes=1, sides=0.3)
    panel('sash_l', skirt, (3.2, 0, 1.5), (7, 35, -16), (-2, 0, 0, 4, 22, 0),
          top=CRIM * 0.8, bot=DRIED, tear=0.3, tear_min=0.1, holes=2, sides=0.3)

    # ---- trailing tatters spreading over the ground
    panel('train_r', low, (-9, 1, 0), (0, 0, 52), (-5, 0, 0, 10, 14, 0),
          top=NIGHT, bot=BURG, tear=0.55, tear_min=0.1, holes=4, sides=0.35)
    panel('train_l', low, (9, 1, 0), (0, 0, -55), (-5, 0, 0, 10, 15, 0),
          top=NIGHT, bot=DRIED, tear=0.55, tear_min=0.1, holes=4, sides=0.35)
    panel('train_back', low, (0, 0, 7.5), (56, 0, 0), (-7, 0, 0, 14, 17, 0),
          top=BLACK, bot=BURG, tear=0.5, tear_min=0.1, holes=5, sides=0.3)
    panel('train_front_r', low, (-7, 3, -6.5), (-38, 40, 30), (-3, 0, 0, 6, 11, 0),
          top=BLACK, bot=CRIM * 0.8, tear=0.55, tear_min=0.1, holes=2, sides=0.35)
    panel('train_front_l', low, (7, 3, -6.5), (-36, -42, -30), (-3, 0, 0, 6, 10, 0),
          top=BLACK, bot=BURG, tear=0.55, tear_min=0.1, holes=2, sides=0.35)
    panel('train_back_r', low, (-7.5, 2, 6), (40, -40, 34), (-4, 0, 0, 8, 13, 0),
          top=BLACK, bot=BURG, tear=0.55, tear_min=0.1, holes=3, sides=0.35)
    panel('train_back_l', low, (7.5, 2, 6), (42, 40, -34), (-4, 0, 0, 8, 14, 0),
          top=BLACK, bot=DRIED, tear=0.55, tear_min=0.1, holes=3, sides=0.35)

    # ---- torso: impossibly thin waist, narrow ribcage
    torso = part('torso', al, (0, -34, 0))
    box(torso, -2, -7.5, -1.5, 4, 8, 3, 'bodice', grow=-0.25, top=BLACK, bot=NIGHT)
    box(torso, -2.5, -1.4, -2, 5, 2, 4, 'metal', grow=-0.2)
    chest = part('chest', torso, (0, -7, 0))
    box(chest, -3.5, -8, -2, 7, 8, 4, 'bodice', grow=-0.1, top=BURG, bot=NIGHT)
    box(chest, -5, -10, -2, 10, 2, 4, 'bodice', grow=-0.15, top=DRIED, bot=BURG)
    box(chest, -5.5, -10.6, -2.6, 11, 5, 5, 'cloth', grow=0.15, top=BURG, bot=NIGHT, tear=0.6, tear_min=0.1,
        holes=2, open_bottom=True)
    box(chest, -1, -13.2, -1, 2, 4, 2, 'bone', grow=-0.15)
    panel('chains_chest', chest, (0, -9.6, -2.9), (-4, 0, 0), (-5.5, 0, 0, 11, 14, 0), mat='chains', arcs=5, drops=5)
    panel('chains_back', chest, (0, -9.6, 2.9), (5, 0, 0), (-5, 0, 0, 10, 12, 0), mat='chains', arcs=3, drops=4)

    # ---- head: nothing but layered veils
    head = part('head', chest, (0, -13, 0))
    box(head, -3, -7, -3, 6, 7, 6, 'shadow')
    box(head, -2.5, -8.3, -2.5, 5, 1, 5, 'cloth', grow=0.1, top=WINE, bot=WINE, folds=0.5)
    box(head, -3.5, -7.5, -3.5, 7, 9, 7, 'cloth', grow=0.1, top=WINE, bot=BURG, tear=0.3, tear_min=0.05,
        folds=0.6, open_bottom=True)
    panel('veil_front', head, (0, -6.6, -3.85), (-5, 0, 0), (-4, 0, 0, 8, 21, 0),
          top=BURG * 1.15, bot=BURG * 0.75, tear=0.3, tear_min=0.05, holes=3, sides=0.15, folds=0.8)
    vf2 = panel('veil_front2', head, (0, -7.2, -4.1), (-8, 0, 0), (-3.5, 0, 0, 7, 13, 0),
                top=CRIM * 0.9, bot=WINE * 0.8, tear=0.45, tear_min=0.1, holes=3, sides=0.2, folds=0.8)
    panel('veil_chain', vf2, (0, 1.2, -0.25), (0, 0, 0), (-4, 0, 0, 8, 10, 0), mat='chains', arcs=3, drops=4)
    panel('veil_back', head, (0, -7, 3.85), (9, 0, 0), (-4.5, 0, 0, 9, 25, 0),
          top=BURG, bot=NIGHT, tear=0.3, tear_min=0.05, holes=4, sides=0.15)
    panel('veil_side_r', head, (-3.85, -7, 0), (0, 0, 8), (0, 0, -3.5, 0, 16, 7),
          top=BURG, bot=NIGHT, tear=0.4, tear_min=0.05, holes=2, sides=0.15)
    panel('veil_side_l', head, (3.85, -7, 0), (0, 0, -8), (0, 0, -3.5, 0, 14, 7),
          top=BURG, bot=DRIED, tear=0.4, tear_min=0.05, holes=2, sides=0.15)

    # ---- crown of twisted black metal
    crown = part('crown', head, (0, -7, 0))
    box(crown, -3.5, -0.6, -3.5, 7, 2, 7, 'metal', grow=0.35)
    spikes = [(0, 9, 12), (38, 6, 28), (-42, 7, 25), (85, 8, 32), (-90, 6, 36), (132, 7, 28),
              (-128, 9, 22), (180, 7, 16), (18, 5, 42), (-22, 4, 40), (158, 4, 44), (-160, 5, 40)]
    for i, (yaw, ln, tilt) in enumerate(spikes):
        a = math.radians(yaw)
        sp = part(f'spike_{i}', crown, (-3.7 * math.sin(a), -0.4, -3.7 * math.cos(a)), (tilt, yaw, 0))
        box(sp, -0.5, -ln, -0.5, 1, ln, 1, 'metal', grow=-0.3, uvkey=f'spike{ln}', spike=True)
    panel('crown_ring', crown, (0, -2.5, 3.5), (-6, 0, 0), (-10, -10, 0, 20, 20, 0), mat='crown_ring')

    # ---- six arms
    pairs = {
        'top': dict(sh=(-5, -9.2, 0), arm=(-12, 0, 112), fore=(-18, 0, 38), hand=(-10, 20, 12),
                    spread=17, curl=(-6, -16)),
        'mid': dict(sh=(-4.6, -6.6, 0.6), arm=(-14, 0, 84), fore=(-30, 0, -24), hand=(-14, 0, -8),
                    spread=11, curl=(-16, -34)),
        'low': dict(sh=(-4.0, -3.8, 0.4), arm=(-4, 0, 30), fore=(-34, 0, -30), hand=(-18, 10, -14),
                    spread=8, curl=(-24, -44)),
    }
    for pn, cfg in pairs.items():
        REST[f'arm_{pn}'], REST[f'forearm_{pn}'], REST[f'hand_{pn}'] = cfg['arm'], cfg['fore'], cfg['hand']
    for sx, s in ((-1, 'r'), (1, 'l')):
        mir = sx > 0

        def MP(v):
            return (-v[0], v[1], v[2]) if mir else v

        def MR(r):
            return (r[0], -r[1], -r[2]) if mir else r

        def MB(p, x, y, z, w, h, d, mat, key, **o):
            if mir:
                x = -x - w
            return box(p, x, y, z, w, h, d, mat, mirror=mir, uvkey=key, **o)

        for pn, cfg in pairs.items():
            arm = part(f'arm_{pn}_{s}', chest, MP(cfg['sh']), MR(cfg['arm']))
            MB(arm, -1, -1, -1, 2, 12, 2, 'bone', f'arm_{pn}', grow=-0.4)
            MB(arm, -1.5, -1.4, -1.5, 3, 7, 3, 'cloth', f'sleeve_{pn}', grow=0.05, top=BURG, bot=NIGHT,
               tear=0.6, tear_min=0.15, holes=1, open_bottom=True)
            if pn != 'low':
                MB(arm, 0.4, 0.5, 0, 7 if pn == 'top' else 6, 9, 0, 'cloth', f'drape_{pn}', top=BURG * 0.8, bot=BLACK,
                   tear=0.7, tear_min=0.15, holes=3, sides=0.15, dir='right')
            fore = part(f'forearm_{pn}_{s}', arm, MP((0, 10.4, 0)), MR(cfg['fore']))
            MB(fore, -1, 0, -1, 2, 12, 2, 'bone', f'fore_{pn}', grow=-0.48)
            MB(fore, -1, 9.2, -1, 2, 1, 2, 'metal', f'bracelet_{pn}', grow=-0.2)
            if pn == 'mid':
                MB(fore, -1, 7.6, -1, 2, 1, 2, 'metal', 'bracelet_mid2', grow=-0.3)
            if pn != 'low':
                MB(fore, 0.3, 0, 0, 5, 8, 0, 'cloth', f'tatter_{pn}', top=NIGHT, bot=BLACK,
                   tear=0.6, tear_min=0.2, holes=2, sides=0.35, dir='right', lace=(pn == 'mid'))
                MB(fore, 0.2, 5, 0.1, 9, 6, 0, 'threads', f'threads_{pn}', dir='right')
            hand = part(f'hand_{pn}_{s}', fore, MP((0, 11.4, 0)), MR(cfg['hand']))
            MB(hand, -1, -0.2, -0.5, 2, 2, 1, 'bone', 'palm', grow=-0.12)
            c0, c1 = cfg['curl']
            for fi, fx in enumerate((-0.95, -0.32, 0.32, 0.95)):
                ln = (3, 4, 4, 3)[fi]
                f = part(f'finger_{pn}_{s}_{fi}', hand, MP((fx, 1.5, 0)), MR((c0, 0, -fx * cfg['spread'])))
                MB(f, -0.5, 0, -0.5, 1, ln, 1, 'bone', f'finger{ln}', grow=-0.3, joints=0.6)
                f2 = part(f'fingertip_{pn}_{s}_{fi}', f, MP((0, ln - 0.5, 0)), MR((c1, 0, -fx * 6)))
                MB(f2, -0.5, 0, -0.5, 1, 4, 1, 'bone', 'fingertip', grow=-0.36, claw=True)
            th = part(f'thumb_{pn}_{s}', hand, MP((1.0, 0.3, -0.2)), MR((-20, 0, -52)))
            MB(th, -0.5, 0, -0.5, 1, 3, 1, 'bone', 'finger3', grow=-0.3, joints=0.6)
            th2 = part(f'thumbtip_{pn}_{s}', th, MP((0, 2.5, 0)), MR((-25, 0, 20)))
            MB(th2, -0.5, 0, -0.5, 1, 3, 1, 'bone', 'thumbtip', grow=-0.36, claw=True)

    # ---- the watching halo: eyes drift independently behind head and shoulders
    halo = part('halo', al, (0, -57, 6.2))
    sizes = {'L': (9, 6), 'M': (7, 5), 'S': (5, 4), 'T': (3, 2)}
    eyes = [(-8, 15.5, 'L', 5), (22, 14, 'M', -12), (-34, 14, 'L', 14), (48, 16, 'L', -8), (-58, 16.5, 'M', 20),
            (73, 14.5, 'M', -25), (-80, 14, 'L', 8), (97, 16.5, 'S', 10), (-103, 16, 'S', -14), (8, 9.5, 'S', 0),
            (-20, 21, 'S', 30), (34, 21.5, 'T', 0), (-47, 22, 'T', -20), (62, 21.5, 'S', 15), (-70, 22.5, 'T', 0),
            (119, 15, 'T', 20), (-123, 16, 'S', -10), (85, 21.5, 'T', -30), (-92, 21, 'S', 12),
            (4, 23, 'T', 8), (-36, 8.5, 'T', -5), (40, 9, 'T', 12)]
    for i, (ang, rad, sz, tilt) in enumerate(eyes):
        a = math.radians(ang)
        w, h = sizes[sz]
        ex, ey = rad * math.sin(a), -rad * math.cos(a)
        e = part(f'eye_{i}', halo, (ex, ey, (i % 4) * 0.55), (0, 0, tilt))
        box(e, -w / 2, -h / 2, 0, w, h, 0, 'eye')
        ph = max(1, int(round(h * 0.45)))
        pu = part(f'pupil_{i}', e, (0, 0, -0.14))
        box(pu, -ph / 2, -ph / 2, 0, ph, ph, 0, 'pupil')
        EYES.append((ex, ey, w, h))
    panel('halo_web', halo, (0, 0, 0.85), (0, 0, 0), (-25, -25, 0, 50, 44, 0), mat='web')


# ---------------------------------------------------------------- uv packing
def rect_size(b):
    w, h, d = b.size
    return 2 * d + 2 * w, d + h


def pack():
    seen = {}
    todo = []
    for p in ORDER:
        for b in p.boxes:
            if b.uvkey in seen:
                assert rect_size(seen[b.uvkey]) == rect_size(b), b.uvkey
                continue
            seen[b.uvkey] = b
            todo.append(b)
    todo.sort(key=lambda b: (-rect_size(b)[1], -rect_size(b)[0]))
    x = y = shelf = 0
    for b in todo:
        w, h = rect_size(b)
        w, h = int(math.ceil(w)) + 1, int(math.ceil(h)) + 1
        if x + w > TEX_W:
            x, y, shelf = 0, y + shelf, 0
        b.uv = (x, y)
        x += w
        shelf = max(shelf, h)
    assert y + shelf <= TEX_H, f'atlas overflow: {y + shelf}'
    for p in ORDER:
        for b in p.boxes:
            b.uv = seen[b.uvkey].uv
    return [seen[k] for k in seen], y + shelf


def face_rects(b):
    """Face name -> (u, v, w, h) in model units, mirroring ModelPart.Cube."""
    u, v = b.uv
    w, h, d = b.size
    return {
        'top': (u + d, v, w, d), 'bottom': (u + d + w, v, w, d),
        'west': (u, v + d, d, h), 'north': (u + d, v + d, w, h),
        'east': (u + d + w, v + d, d, h), 'south': (u + d + w + d, v + d, w, h),
    }


# ---------------------------------------------------------------- painting
def vnoise1(n, cells, rng):
    cells = max(1, int(cells))
    pts = rng.random(cells + 2)
    x = np.linspace(0, cells, n, endpoint=False)
    i = x.astype(int)
    f = x - i
    f = f * f * (3 - 2 * f)
    return pts[i] * (1 - f) + pts[i + 1] * f


def fbm1(n, cells, rng, octaves=3):
    out, amp, tot = np.zeros(n), 1.0, 0.0
    for _ in range(octaves):
        out += amp * vnoise1(n, cells, rng)
        tot += amp
        amp *= 0.5
        cells *= 2
    return out / tot


def vnoise2(h, w, cy, cx, rng):
    cy, cx = max(1, int(cy)), max(1, int(cx))
    g = rng.random((cy + 2, cx + 2))
    y = np.linspace(0, cy, h, endpoint=False)[:, None]
    x = np.linspace(0, cx, w, endpoint=False)[None, :]
    iy, ix = y.astype(int), x.astype(int)
    fy, fx = y - iy, x - ix
    fy, fx = fy * fy * (3 - 2 * fy), fx * fx * (3 - 2 * fx)
    return (g[iy, ix] * (1 - fy) * (1 - fx) + g[iy + 1, ix] * fy * (1 - fx)
            + g[iy, ix + 1] * (1 - fy) * fx + g[iy + 1, ix + 1] * fy * fx)


def fbm2(h, w, cy, cx, rng, octaves=3):
    out, amp, tot = np.zeros((h, w)), 1.0, 0.0
    for _ in range(octaves):
        out += amp * vnoise2(h, w, cy, cx, rng)
        tot += amp
        amp *= 0.5
        cy, cx = cy * 2, cx * 2
    return out / tot


def blank(w, h):
    return np.zeros((h, w, 4))


def darken_edges(img, amount=0.5):
    """Dirty, singed look along every torn edge."""
    a = img[..., 3] > 0.5
    pad = np.pad(a, 1, constant_values=True)
    near = ~(pad[:-2, 1:-1] & pad[2:, 1:-1] & pad[1:-1, :-2] & pad[1:-1, 2:])
    img[..., :3][near & a] *= amount


def cloth(w, h, rng, top=BURG, bot=BLACK, tear=0.0, tear_min=0.0, holes=0, sides=0.0, folds=1.0, lace=False,
          accent=CRIM, **_):
    if w <= 0 or h <= 0:
        return blank(max(w, 0), max(h, 0))
    y = np.linspace(0, 1, h)[:, None, None]
    col = np.asarray(top)[None, None, :] * (1 - y) + np.asarray(bot)[None, None, :] * y
    col = np.broadcast_to(col, (h, w, 3)).copy()
    fold = fbm1(w, max(2, w / 7), rng)
    drift = (fbm2(h, w, max(1, h / 24), max(1, w / 10), rng) - 0.5) * 0.5
    ridge = np.clip(fold[None, :] + drift, 0, 1)
    col *= (1 - 0.55 * folds) + 1.1 * folds * ridge[..., None]
    hi = np.clip((ridge - 0.66) / 0.3, 0, 1)[..., None] * 0.55 * folds
    col = col * (1 - hi) + np.asarray(accent)[None, None, :] * hi
    col *= 1 + (rng.random((h, w, 1)) - 0.5) * 0.22          # weave grain
    stain = fbm2(h, w, max(1, h / 14), max(1, w / 14), rng)
    col *= np.where(stain > 0.62, 0.6, 1.0)[..., None]       # dried blotches
    for _ in range(max(1, w // 14)):                         # loose crimson threads
        x0 = rng.integers(0, w)
        y0, y1 = sorted(rng.integers(0, h, 2))
        col[y0:y1, x0] = col[y0:y1, x0] * 0.5 + CRIM * 0.8
    alpha = np.ones((h, w))
    yy = np.arange(h)[:, None]
    xx = np.arange(w)[None, :]
    if tear > 0:
        depth = tear_min + tear * fbm1(w, max(4, w / 3), rng, 4) ** 1.4
        spikes = rng.random(w) < 0.08
        depth = np.where(spikes, depth * rng.uniform(1.2, 1.9, w), depth)
        edge = (h * (1 - np.clip(depth, 0, 0.95))).astype(int)
        alpha[yy >= edge[None, :]] = 0
        for x0 in np.nonzero(rng.random(w) < 0.14)[0]:       # frayed strands
            ln = rng.integers(2, max(3, int(h * 0.14)))
            alpha[edge[x0]:edge[x0] + ln, x0] = 1
            col[edge[x0]:edge[x0] + ln, x0] *= 0.6
    if sides > 0:
        for flip in (False, True):
            d = (sides * w * fbm1(h, max(3, h / 6), rng, 4) ** 1.2 * np.linspace(0.25, 1.0, h)).astype(int)
            m = (xx < d[:, None]) if not flip else (xx >= w - d[:, None])
            alpha[m] = 0
    for _ in range(holes):
        cy = h * (0.25 + 0.7 * rng.random() ** 0.7)
        cx = w * rng.random()
        ry, rx = rng.uniform(1.5, 2.0 + h * 0.07), rng.uniform(1.0, 1.5 + w * 0.09)
        n = vnoise2(h, w, max(1, h / 4), max(1, w / 4), rng)
        alpha[((xx - cx) / rx) ** 2 + ((yy - cy) / ry) ** 2 + n * 0.9 < 1] = 0
    if lace:
        net = ((xx + yy) % 2 == 0) | (xx % 5 == 0) | (yy % 7 == 0)
        alpha[~net] = 0
    img = np.dstack([np.clip(col, 0, 1), alpha])
    darken_edges(img)
    return img


def bone(w, h, rng, joints=1.0, claw=False, **_):
    if w <= 0 or h <= 0:
        return blank(max(w, 0), max(h, 0))
    xx = np.linspace(-1, 1, w)[None, :]
    yy = np.linspace(0, 1, h)[:, None]
    col = np.broadcast_to(IVORY[None, None, :], (h, w, 3)).copy()
    col *= (1 - 0.42 * xx ** 2)[..., None]                   # fake roundness
    col *= (0.82 + 0.3 * fbm2(h, w, max(1, h / 6), max(1, w / 3), rng))[..., None]
    end = np.clip(1 - np.minimum(yy, 1 - yy) * 7, 0, 1) * joints
    joint_col = ASH * 0.75 + DRIED * 0.25
    col = col * (1 - 0.7 * end[..., None]) + joint_col[None, None, :] * 0.7 * end[..., None]
    for _ in range(max(1, h // 14)):                         # hairline porcelain cracks
        x0, y0 = rng.integers(0, w), rng.integers(0, h)
        for _ in range(rng.integers(4, 4 + h // 3)):
            if 0 <= x0 < w and 0 <= y0 < h:
                col[y0, x0] = col[y0, x0] * 0.35 + DRIED * 0.3
            y0 += 1
            x0 += rng.integers(-1, 2)
    if claw:
        tip = np.clip((yy - 0.45) / 0.55, 0, 1)[..., None]
        col = col * (1 - tip) + (DRIED * 0.7)[None, None, :] * tip
    return np.dstack([np.clip(col, 0, 1), np.ones((h, w))])


def metal(w, h, rng, spike=False, **_):
    if w <= 0 or h <= 0:
        return blank(max(w, 0), max(h, 0))
    n = fbm2(h, w, max(1, h / 3), max(1, w / 3), rng)
    col = DARKMETAL[None, None, :] * (0.6 + 0.9 * n[..., None])
    glint = rng.random((h, w)) < 0.07
    col[glint] = SILVER * rng.uniform(0.5, 0.9)
    edge = np.zeros((h, w), bool)
    edge[:, 0] = True
    col[edge] = col[edge] * 0.4 + SILVER * 0.35
    if spike:
        yy = np.linspace(0, 1, h)[:, None, None]
        col = col * (0.9 + 0.5 * yy) + SILVER[None, None, :] * 0.5 * (1 - yy) ** 2
    return np.dstack([np.clip(col, 0, 1), np.ones((h, w))])


def bodice(w, h, rng, face='north', top=BURG, bot=NIGHT, **_):
    img = cloth(w, h, rng, top=top, bot=bot, folds=0.5)
    if face == 'north' and w >= 8:
        c = w // 2
        yy = np.arange(h)
        img[:, c - 1:c + 1, :3] = BLACK                      # busk line and lacing
        for y0 in range(2, h - 2, 5):
            for k in range(4):
                for sgn in (-1, 1):
                    x0 = c + sgn * (k + 1)
                    if 0 <= x0 < w and y0 + k < h:
                        img[y0 + k, x0, :3] = SILVER * 0.55
        for sgn in (-1, 1):                                  # rib shadows
            for k, y0 in enumerate(range(3, h - 2, 6)):
                xs = np.arange(2, c - 3)
                img[np.clip(y0 + xs // 3, 0, h - 1), c + sgn * (xs + 3) - (1 if sgn < 0 else 0), :3] *= 0.45
        del yy
    return img


def line(img, x0, y0, x1, y1, colfn):
    n = int(max(abs(x1 - x0), abs(y1 - y0))) + 1
    h, w = img.shape[:2]
    for i in range(n + 1):
        t = i / max(1, n)
        x, y = int(round(x0 + (x1 - x0) * t)), int(round(y0 + (y1 - y0) * t))
        if 0 <= x < w and 0 <= y < h:
            img[y, x, :3] = colfn(i)
            img[y, x, 3] = 1


def chain_col(i):
    return SILVER * (0.95 if i % 2 == 0 else 0.42)


def pendant(img, x, y, rng):
    h, w = img.shape[:2]
    ln = rng.integers(2, 7)
    line(img, x, y, x, y + ln, chain_col)
    y += ln
    kind = rng.integers(0, 4)
    pts = []
    if kind == 0:      # ring
        pts = [(dx, dy, SILVER * 0.8) for dx, dy in ((-1, 1), (0, 0), (1, 1), (-1, 2), (1, 2), (0, 3))]
    elif kind == 1:    # blood-drop gem
        pts = [(0, 0, SILVER * 0.6), (-1, 1, CRIM), (0, 1, CRIM * 1.6), (1, 1, CRIM), (0, 2, CRIM * 0.8)]
    elif kind == 2:    # tiny bell
        pts = [(0, 0, SILVER * 0.7), (-1, 1, SILVER * 0.6), (0, 1, SILVER), (1, 1, SILVER * 0.5),
               (-1, 2, SILVER * 0.5), (1, 2, SILVER * 0.4), (0, 3, DARKMETAL)]
    else:              # occult cross
        pts = [(0, 0, SILVER * 0.7), (0, 1, SILVER * 0.8), (-1, 1, SILVER * 0.6), (1, 1, SILVER * 0.6),
               (0, 2, SILVER * 0.7), (0, 3, SILVER * 0.5)]
    for dx, dy, c in pts:
        if 0 <= x + dx < w and 0 <= y + dy < h:
            img[y + dy, x + dx, :3] = np.clip(c, 0, 1)
            img[y + dy, x + dx, 3] = 1


def chains(w, h, rng, arcs=4, drops=4, **_):
    img = blank(w, h)
    for _ in range(arcs):
        xa, xb = sorted(rng.integers(0, w, 2))
        if xb - xa < w // 3:
            xa, xb = max(0, xa - w // 4), min(w - 1, xb + w // 4)
        ya, yb = rng.integers(0, h // 3), rng.integers(0, h // 3)
        sag = rng.uniform(0.2, 0.6) * h
        prev = None
        for i in range(xb - xa + 1):
            t = i / max(1, xb - xa)
            y = int(round(ya + (yb - ya) * t + sag * 4 * t * (1 - t)))
            x = xa + i
            if prev is not None:
                line(img, prev[0], prev[1], x, y, lambda k, i=i: chain_col(i + k))
            prev = (x, y)
        if rng.random() < 0.8:
            t = 0.5
            pendant(img, (xa + xb) // 2, int(round(ya + (yb - ya) * t + sag)), rng)
    for _ in range(drops):
        x = rng.integers(1, w - 1)
        ln = rng.integers(h // 4, h - 6)
        line(img, x, 0, x, ln, chain_col)
        pendant(img, x, ln, rng)
    return img


def threads(w, h, rng, **_):
    img = blank(w, h)
    for _ in range(max(2, w // 3)):
        x = rng.integers(0, w)
        ln = rng.integers(h // 3, h)
        wob = 0
        for y in range(ln):
            if rng.random() < 0.08:
                wob += rng.integers(-1, 2)
            xx = min(w - 1, max(0, x + wob))
            img[y, xx] = (*(BLACK * 1.4), 1)
        if rng.random() < 0.5:
            img[ln - 1, min(w - 1, max(0, x + wob))] = (*(SILVER * 0.7), 1)
    return img


def eye(w, h, rng, **_):
    img = blank(w, h)
    u = (np.arange(w) + 0.5) / w * 2 - 1
    v = (np.arange(h) + 0.5) / h * 2 - 1
    uu, vv = np.meshgrid(u, v)
    wob = (fbm1(w, 4, rng) - 0.5) * 0.18
    lid = np.clip(1 - np.abs(uu) ** 2.2, 0, 1) ** 0.75 * 0.94 + wob[None, :]
    inside = np.abs(vv) < lid
    rim = inside & (np.abs(vv) > lid - 0.3)
    flesh = (BURG * 0.5 + ASH * 0.22)[None, None, :] * (0.55 + 1.0 * fbm2(h, w, 3, 4, rng)[..., None])
    sclera = np.broadcast_to((BLACK * 0.7)[None, None, :], (h, w, 3)).copy()
    for _ in range(5):                                       # veins creeping in from the corners
        x, y = (0 if rng.random() < 0.5 else w - 1), h / 2
        dx = 1 if x == 0 else -1
        for _ in range(int(w * rng.uniform(0.2, 0.42))):
            if 0 <= int(y) < h and 0 <= x < w:
                sclera[int(y), x] = sclera[int(y), x] * 0.4 + CRIM * 0.55
            x += dx
            y += rng.uniform(-0.9, 0.9)
    r = np.sqrt((uu * w / h) ** 2 + vv ** 2)
    iris = (r > 0.5) & (r < 0.78)
    sclera[iris] = sclera[iris] * 0.5 + DRIED * 0.6
    sclera *= np.clip(0.55 + 0.6 * (vv + 0.9), 0.4, 1.1)[..., None]   # upper lid shadow
    img[..., :3] = np.where(rim[..., None], flesh, sclera)
    img[..., 3] = inside
    darken_edges(img, 0.45)
    return img


def pupil(w, h, rng, **_):
    img = blank(w, h)
    u = (np.arange(w) + 0.5) / w * 2 - 1
    uu, vv = np.meshgrid(u, u[:h] if h == w else (np.arange(h) + 0.5) / h * 2 - 1)
    r = np.sqrt(uu ** 2 + vv ** 2)
    img[r < 1.0] = (0.62, 0.03, 0.07, 1)
    img[r < 0.78] = (0.16, 0.0, 0.02, 1)
    img[r < 0.5] = (1.0, 0.16, 0.14, 1)
    img[r < 0.24] = (1.0, 0.55, 0.45, 1)
    return img


def crown_ring(w, h, rng, **_):
    img = blank(w, h)
    cx, cy = w / 2, h / 2
    R = w * 0.36

    def mcol(i):
        return np.clip(DARKMETAL * (1.2 + 0.5 * (i % 3 == 0)) + SILVER * (0.5 if i % 4 == 0 else 0.16), 0, 1)

    for k in range(720):
        a = k / 720 * 2 * math.pi
        for rr in (R - 1, R, R + 1):
            x, y = int(cx + rr * math.sin(a)), int(cy - rr * math.cos(a))
            if 0 <= x < w and 0 <= y < h:
                img[y, x] = (*mcol(k // 6), 1)
    n = 26
    for k in range(n):
        a = (k + rng.uniform(-0.25, 0.25)) / n * 2 * math.pi
        up = math.cos(a)                                     # longer spikes toward the top
        ln = rng.uniform(0.08, 0.2) * w * (0.55 + 0.75 * max(0, up)) * (1.6 if k % 4 == 0 else 1.0)
        line(img, cx + R * math.sin(a), cy - R * math.cos(a),
             cx + (R + ln) * math.sin(a), cy - (R + ln) * math.cos(a), mcol)
        if k % 3 == 0:
            line(img, cx + R * math.sin(a), cy - R * math.cos(a),
                 cx + (R - ln * 0.35) * math.sin(a), cy - (R - ln * 0.35) * math.cos(a), mcol)
    for k in range(7):                                       # ornaments dangling from the lower arc
        a = math.radians(180 + (k - 3) * 17 + rng.uniform(-4, 4))
        x, y = int(cx + R * math.sin(a)), int(cy - R * math.cos(a))
        ln = rng.integers(2, 6)
        line(img, x, y, x, y + ln, chain_col)
        pendant(img, x, y + ln, rng)
    return img


def web(w, h, rng, **_):
    """Organic tendrils, smoke and dangling threads knitting the eyes together."""
    img = blank(w, h)
    ox, oy = 25 * S, 25 * S                                  # halo origin inside this plane
    for ex, ey, ew, eh in EYES:                              # dithered smoke behind every eye
        cx, cy = ox + ex * S, oy + ey * S
        yy, xx = np.mgrid[0:h, 0:w]
        n = vnoise2(h, w, h / 10, w / 10, rng)
        m = (((xx - cx) / (ew * S * 0.9)) ** 2 + ((yy - cy) / (eh * S * 1.1)) ** 2 + n * 0.8) < 1
        dith = vnoise2(h, w, h / 2.5, w / 2.5, rng) > 0.4
        sel = m & dith
        shade = 0.5 + 1.2 * vnoise2(h, w, h / 6, w / 6, rng)
        img[sel, :3] = (BLACK * 1.2)[None, :] * shade[sel][:, None] + (BURG * 0.25)[None, :]
        img[sel, 3] = 1
    for i, (ex, ey, ew, eh) in enumerate(EYES):              # tendrils reaching back toward the crown
        x, y = ox + ex * S, oy + ey * S
        tx, ty = ox + rng.uniform(-3, 3) * S, oy + rng.uniform(-2, 4) * S
        steps = int(math.hypot(tx - x, ty - y) * 0.85)
        ang = math.atan2(ty - y, tx - x)
        for k in range(steps):
            ang += rng.uniform(-0.35, 0.35)
            ang = ang * 0.8 + math.atan2(ty - y, tx - x) * 0.2
            x += math.cos(ang)
            y += math.sin(ang)
            for t in range(2 if k < steps * 0.4 else 1):
                xi, yi = int(x) + t, int(y)
                if 0 <= xi < w and 0 <= yi < h:
                    img[yi, xi] = (*(BLACK * 1.3 + BURG * 0.35 * (k % 4 == 0)), 1)
        if i % 2 == 0:                                       # threads hanging from the eyes
            x0, y0 = int(ox + ex * S), int(oy + (ey + eh / 2) * S)
            ln = rng.integers(6, 14) * S
            for k in range(ln):
                if 0 <= y0 + k < h and 0 <= x0 < w:
                    img[y0 + k, x0] = (*(BLACK * 1.5), 1)
    return img


def shadow(w, h, rng, **_):
    img = np.ones((h, w, 4))
    img[..., :3] = (BLACK * 0.9)[None, None, :]
    return img


PAINTERS = dict(cloth=cloth, bone=bone, metal=metal, bodice=bodice, chains=chains, threads=threads, eye=eye,
                pupil=pupil, crown_ring=crown_ring, web=web, shadow=shadow)


def paint_face(b, face, w, h, rng):
    o = dict(b.opts)
    fn = PAINTERS[b.mat]
    if b.mat == 'cloth':
        if face in ('top', 'bottom'):
            img = cloth(w, h, rng, top=o.get('top', BURG), bot=o.get('top', BURG), folds=0.4)
            if (face == 'bottom' and o.get('open_bottom')) or (face == 'top' and o.get('open_top')):
                img[..., 3] = 0
            return img
        if o.get('dir') == 'right':                          # cloth hanging toward +u
            return np.rot90(fn(h, w, rng, **{k: v for k, v in o.items() if k != 'dir'}), 1)
    if b.mat == 'threads' and o.get('dir') == 'right':
        return np.rot90(fn(h, w, rng), 1)
    if b.mat == 'bone' and face in ('top', 'bottom'):
        img = bone(w, h, rng, joints=0)
        img[..., :3] *= 0.6
        return img
    return fn(w, h, rng, face=face, **o)


def paint(uniq):
    atlas = np.zeros((TEX_H * S, TEX_W * S, 4))
    glow = np.zeros_like(atlas)
    rage = np.zeros_like(atlas)
    for b in uniq:
        rng = rng_for(b.uvkey)
        rects = face_rects(b)
        w, h, d = b.size
        flat_z, flat_x = d == 0, w == 0
        cache = None
        for face, (u, v, fw, fh) in rects.items():
            tw, th = int(round(fw * S)), int(round(fh * S))
            if tw <= 0 or th <= 0:
                continue
            if (flat_z and face == 'south') or (flat_x and face == 'east'):
                img = cache[:, ::-1]                         # coplanar faces must show the same image
            else:
                img = paint_face(b, face, tw, th, rng)
                cache = img
            y0, x0 = int(round(v * S)), int(round(u * S))
            atlas[y0:y0 + th, x0:x0 + tw] = img
            a = img[..., 3:4]
            if b.mat == 'pupil':
                glow[y0:y0 + th, x0:x0 + tw] = img
                rage[y0:y0 + th, x0:x0 + tw] = img
            elif b.mat == 'eye':
                g = img.copy()
                g[..., :3] = CRIM[None, None, :] * 0.35 * (img[..., :1] + 0.3)
                glow[y0:y0 + th, x0:x0 + tw] = g * a
                g[..., :3] *= 2.2
                rage[y0:y0 + th, x0:x0 + tw] = np.clip(g, 0, 1) * a
            elif b.mat in ('cloth', 'bodice', 'bone'):       # phase two: wounds of light under the surface
                r2 = rng_for(b.uvkey + '!rage')
                n = fbm2(th, tw, max(1, th / 9), max(1, tw / 9), r2, 4)
                vein = np.abs(n - 0.5) < (0.012 if b.mat == 'bone' else 0.02)
                g = np.zeros((th, tw, 4))
                g[vein] = (0.75, 0.05, 0.08, 1)
                rage[y0:y0 + th, x0:x0 + tw] = g * (a > 0.5)
    return atlas, glow, rage


def save(arr, name, folder=TEX_DIR):
    os.makedirs(folder, exist_ok=True)
    Image.fromarray((np.clip(arr, 0, 1) * 255 + 0.5).astype(np.uint8), 'RGBA').save(os.path.join(folder, name))


def composite_eye(w, h, rng):
    img = eye(w, h, rng)
    ph = int(h * 0.5)
    pu = pupil(ph, ph, rng)
    y0, x0 = (h - ph) // 2, (w - ph) // 2
    a = pu[..., 3:4]
    img[y0:y0 + ph, x0:x0 + ph] = img[y0:y0 + ph, x0:x0 + ph] * (1 - a) + pu * a
    return img


EFFIGY = (
    "....m..m..m.....",
    ".....m.mm.m.....",
    "......mmmm......",
    "......vVVv......",
    ".i....vVVv....i.",
    "..i...vVVv...i..",
    "...i.cvVVvc.i...",
    "....iicVVcii....",
    ".iiii.cvvc.iiii.",
    "....i.bccb.i....",
    "...i..bCCb..i...",
    "..i..bbCCbb..i..",
    ".....bcCCcb.....",
    "....bbcCCcbb....",
    "...bbccCCccbb...",
    "..bb.bc.bc.b.bb.",
)


SIGIL = np.array([0.86, 0.09, 0.17])       # bright enough to read on grass in daylight


def polar(n):
    c = (n - 1) / 2
    yy, xx = np.mgrid[0:n, 0:n]
    dx, dy = (xx - c) / c, (yy - c) / c
    return dx, dy, np.hypot(dx, dy), np.arctan2(dy, dx)


def stain(img, mask, rng, base=0.3):
    h, w = img.shape[:2]
    n = fbm2(h, w, 6, 6, rng)
    img[..., :3][mask] = NIGHT * 0.5
    img[..., 3][mask] = (base + 0.22 * n)[mask]


def ink(img, mask, col=SIGIL, shade=None):
    img[..., :3][mask] = np.clip(col, 0, 1) if shade is None else np.clip(col[None, :] * shade[mask][:, None], 0, 1)
    img[..., 3][mask] = 1


def thick_line(img, x0, y0, x1, y1, col=SIGIL):
    for ox, oy in ((0, 0), (1, 0), (0, 1)):
        line(img, x0 + ox, y0 + oy, x1 + ox, y1 + oy, lambda i: np.clip(col, 0, 1))


def sigil_disc(n, rng):
    """The standard seal: runes between two rings, a seven-pointed star and an eye at the centre."""
    img = blank(n, n)
    dx, dy, r, th = polar(n)
    c = (n - 1) / 2
    stain(img, r < 0.96, rng)
    turn = (th + math.pi) / (2 * math.pi)
    runes = rng.random(40) < 0.62
    ink(img, (r > 0.865) & (r < 0.925) & runes[(turn * 40).astype(int) % 40] & ((turn * 40) % 1 < 0.72), SIGIL * 0.8)
    pts = [(c + 0.84 * c * math.cos(a), c + 0.84 * c * math.sin(a)) for a in np.arange(7) * 2 * math.pi / 7 - math.pi / 2]
    for i in range(7):
        thick_line(img, *pts[i], *pts[(i + 3) % 7], SIGIL * 0.85)
    ink(img, (r > 0.93) & (r < 0.995))
    ink(img, (r > 0.835) & (r < 0.86))
    ink(img, (r > 0.36) & (r < 0.39))
    lid = 0.17 * np.clip(1 - (dx / 0.31) ** 2, 0, 1) ** 0.7
    stain(img, (np.abs(dy) < lid) & (np.abs(dx) < 0.31), rng, 0.75)
    ink(img, (np.abs(np.abs(dy) - lid) < 0.022) & (np.abs(dx) < 0.31))
    ink(img, r < 0.075, IVORY)
    return img


def sigil_mark(n, rng):
    """Marked for Death: a single open eye that follows its victim."""
    img = blank(n, n)
    dx, dy, r, th = polar(n)
    stain(img, r < 0.95, rng, 0.38)
    turn = (th + math.pi) / (2 * math.pi)
    ink(img, (r > 0.9) & (r < 0.99) & ((turn * 16) % 1 < 0.7))
    lid = 0.5 * np.clip(1 - (dx / 0.86) ** 2, 0, 1) ** 0.75
    ink(img, (np.abs(np.abs(dy) - lid) < 0.05) & (np.abs(dx) < 0.86))
    ink(img, (r > 0.27) & (r < 0.35), SIGIL * 0.8)
    ink(img, r < 0.15, IVORY)
    for ang in (0, 90, 180, 270):
        ca, sa = math.cos(math.radians(ang)), math.sin(math.radians(ang))
        along, across = dx * ca + dy * sa, -dx * sa + dy * ca
        ink(img, (along > 0.66) & (along < 0.9) & (np.abs(across) < 0.035))
    return img


def sigil_safe(n, rng):
    """A refuge: pale, calm and unmistakably not hers."""
    img = blank(n, n)
    dx, dy, r, th = polar(n)
    img[..., :3][r < 0.92] = IVORY
    img[..., 3][r < 0.92] = 0.22 + 0.2 * (1 - r[r < 0.92])
    turn = (th + math.pi) / (2 * math.pi)
    ink(img, (r > 0.56) & (r < 0.84) & ((turn * 16) % 1 < 0.16), IVORY)
    ink(img, (r > 0.9) & (r < 0.99), np.array([1.0, 0.97, 0.9]))
    ink(img, (r > 0.48) & (r < 0.53), SILVER * 1.4)
    ink(img, r < 0.1, np.array([1.0, 0.97, 0.9]))
    return img


def sigil_ring(w, h, rng):
    """Tile for an annulus: u runs around it, v from the inner edge to the outer."""
    img = blank(w, h)
    yy, xx = np.mgrid[0:h, 0:w]
    stain(img, np.ones((h, w), bool), rng)
    ink(img, ((yy + np.abs((xx % 16) - 7.5) * 1.1) % 16 < 2) & (yy > 5) & (yy < h - 6), SIGIL * 0.7)
    ink(img, (yy < 3) | (yy >= h - 3))
    return img


def sigil_line(n, rng):
    """Tile for a strip: u across it, v along it, with chevrons pointing the way the blow travels."""
    img = blank(n, n)
    yy, xx = np.mgrid[0:n, 0:n]
    stain(img, np.ones((n, n), bool), rng)
    ink(img, ((yy + np.abs(xx - (n - 1) / 2) * 0.8) % 16 < 2.2) & (xx > 4) & (xx < n - 5), SIGIL * 0.7)
    ink(img, (xx < 3) | (xx >= n - 3))
    return img


def death_pillar(w, h, rng):
    """Rising wisps for the burst: opaque at the base, torn away toward the top. Tiles sideways."""
    x = np.arange(w)[None, :]
    y = np.linspace(0, 1, h)[:, None]
    streak = np.zeros((h, w))
    for k in range(1, 7):
        streak += np.sin(2 * math.pi * k * x / w + rng.uniform(0, 6.28) + y * rng.uniform(-3, 3) * k) / k
    streak = (streak - streak.min()) / (streak.max() - streak.min())
    alpha = np.clip(streak * 1.7 - 0.35, 0, 1) * y ** 1.2
    img = blank(w, h)
    heat = np.clip(streak * 1.6 - 0.6, 0, 1)[..., None]
    img[..., :3] = BLACK[None, None, :] * (1 - heat) + (SIGIL * 1.1)[None, None, :] * heat
    img[..., 3] = np.clip(alpha * 1.3, 0, 1)
    sparks = (rng.random((h, w)) < 0.012) & (alpha > 0.3)
    img[sparks] = (*IVORY, 1)
    return img


def death_crescent(w, h, rng):
    img = blank(w, h)
    yy, xx = np.mgrid[0:h, 0:w]
    outer = np.hypot(xx - (w - 1) / 2, yy - (h + 2)) / (w * 0.5)
    inner = np.hypot(xx - (w - 1) / 2, yy - (h + 9)) / (w * 0.5)
    blade = (outer < 1.36) & (inner > 1.42)
    edge = np.clip((outer - 1.1) / 0.26, 0, 1)
    img[..., :3] = BLACK[None, None, :] * (1 - edge[..., None]) + (SIGIL * 1.15)[None, None, :] * edge[..., None]
    img[..., 3] = blade
    img[blade & (outer > 1.3)] = (*IVORY, 1)
    return img


def death_orb(n, rng):
    img = blank(n, n)
    dx, dy, r, th = polar(n)
    img[r < 1.0] = (*(SIGIL * 0.55), 0.55)
    img[r < 0.82] = (*SIGIL, 1)
    img[r < 0.6] = (*BLACK, 1)
    img[np.hypot(dx + 0.2, dy + 0.22) < 0.16] = (*IVORY, 1)
    return img


def soul_lance(w, h, rng):
    """A spectral spear, point at the right-hand end."""
    img = blank(w, h)
    yy, xx = np.mgrid[0:h, 0:w]
    mid = (h - 1) / 2
    t = xx / (w - 1)
    half = np.where(t < 0.6, 0.6, 0.6 + 3.0 * np.sin(np.clip((t - 0.6) / 0.4, 0, 1) * math.pi) ** 0.8)
    body = np.abs(yy - mid) <= half
    img[..., :3] = np.where((t > 0.6)[..., None], IVORY[None, None, :], (SIGIL * 0.9)[None, None, :])
    rim = body & (np.abs(yy - mid) > half - 1.0) & (t > 0.6)
    img[..., :3][rim] = SIGIL
    img[..., 3] = body * np.clip(t * 4, 0, 1)
    return img


TOME = (
    "................",
    "..bbbbbbbbbbb...",
    ".bBBBBBBBBBBBs..",
    ".bBrBBBBBBBrBsp.",
    ".bBBBBBBBBBBBsp.",
    ".bBBBkkkkkBBBsp.",
    ".bBBkkeEekkBBsp.",
    ".bBBBkkkkkBBBsp.",
    ".bBBBBBcBBBBBsp.",
    ".bBBBBBcBBBBBsp.",
    ".bBrBBBBBBBrBsp.",
    ".bBBBBBBBBBBBsp.",
    ".bbbbbbbbbbbbsp.",
    "..ppppppppppppp.",
    "................",
    "................",
)


def extras():
    """Small standalone textures: the thrown eye, the wrist threads and the item icons."""
    rng = rng_for('extras')
    canvas = blank(32, 32)
    canvas[6:26] = composite_eye(32, 20, rng)
    save(canvas, 'watching_eye.png')

    string = np.zeros((64, 4, 4))
    fade = np.linspace(0, 1, 64)[:, None] ** 1.5              # v=1 is the wrist, v=0 dissolves upward
    string[..., :3] = (BURG * 0.7 + BLACK * 0.3)[None, None, :]
    string[::7, :, :3] = SILVER * 0.35
    string[..., 3] = fade * 0.9
    save(string, 'marionette_string.png')

    item_dir = os.path.join(os.path.dirname(TEX_DIR), 'item')
    icon = blank(16, 16)
    icon[3:13, 0:16] = composite_eye(16, 10, rng)
    for x in (3, 7, 11):                                      # dangling threads
        icon[13:13 + rng.integers(1, 4), x] = (*(BLACK * 2), 1)
    save(icon, 'eye_of_oblivion.png', item_dir)

    colors = {'m': SILVER * 0.55, 'v': BURG, 'V': WINE, 'i': IVORY * 0.9, 'c': CRIM, 'C': DRIED, 'b': BLACK * 2.5}
    doll = blank(16, 16)
    for y, row in enumerate(EFFIGY):
        for x, ch in enumerate(row):
            if ch != '.':
                doll[y, x] = (*np.clip(colors[ch] * rng.uniform(0.88, 1.12), 0, 1), 1)
    save(doll, 'mourning_effigy.png', item_dir)

    book_colors = {'b': BURG * 0.6, 'B': BURG * 1.1, 's': SILVER * 0.6, 'p': IVORY * 0.85, 'k': BLACK * 1.5,
                   'e': CRIM * 1.3, 'E': np.array([1.0, 0.3, 0.25]), 'r': SILVER * 0.9, 'c': SILVER * 0.5}
    book_rng = rng_for('tome')
    book = blank(16, 16)
    for y, row in enumerate(TOME):
        for x, ch in enumerate(row):
            if ch != '.':
                book[y, x] = (*np.clip(book_colors[ch] * book_rng.uniform(0.9, 1.1), 0, 1), 1)
    save(book, 'tome.png', item_dir)

    # The great eye that opens over a player: lids and pupil are separate so the pupil can move.
    save(eye(64, 40, rng_for('omen_eye')), 'omen_eye.png')
    save(pupil(16, 16, rng_for('omen_pupil')), 'omen_pupil.png')

    # Spell effects. Each painter gets its own seed so adding one never repaints the others.
    spells = {
        'sigil_disc.png': lambda r: sigil_disc(128, r), 'sigil_mark.png': lambda r: sigil_mark(64, r),
        'sigil_safe.png': lambda r: sigil_safe(64, r), 'sigil_ring.png': lambda r: sigil_ring(64, 32, r),
        'sigil_line.png': lambda r: sigil_line(32, r), 'death_pillar.png': lambda r: death_pillar(32, 64, r),
        'death_crescent.png': lambda r: death_crescent(32, 24, r), 'death_orb.png': lambda r: death_orb(16, r),
        'soul_lance.png': lambda r: soul_lance(32, 8, r),
    }
    for name, painter in spells.items():
        save(painter(rng_for(name)), name)
    save(np.ones((8, 8, 4)), 'sigil_fill.png')


# ---------------------------------------------------------------- java
def jf(v):
    s = f'{v:.4f}'.rstrip('0').rstrip('.')
    return ('0' if s in ('-0', '') else s) + 'F'


def java_ident(name):
    return 'p_' + name


def emit_part(p, lines, methods):
    cubes = 'CubeListBuilder.create()'
    for b in p.boxes:
        cubes += f'\n                .texOffs({b.uv[0]}, {b.uv[1]})'
        if b.mirror:
            cubes += '.mirror()'
        cubes += '.addBox(' + ', '.join(jf(v) for v in (*b.o, *b.size))
        cubes += f', new CubeDeformation({jf(b.grow)}))' if b.grow else ')'
        if b.mirror:
            cubes += '.mirror(false)'
    if any(p.rot):
        pose = 'PartPose.offsetAndRotation(' + ', '.join(jf(v) for v in p.pivot) + ', ' + \
               ', '.join(jf(math.radians(v)) for v in p.rot) + ')'
    else:
        pose = 'PartPose.offset(' + ', '.join(jf(v) for v in p.pivot) + ')'
    call = f'parent.addOrReplaceChild("{p.name}", {cubes},\n                {pose})'
    if p.children:
        body = [f'        PartDefinition self = {call};']
        for c in p.children:
            body.append(f'        {java_ident(c.name)}(self);')
    else:
        body = [f'        {call};']
    methods.append(f'    private static void {java_ident(p.name)}(PartDefinition parent) {{\n'
                   + '\n'.join(body) + '\n    }\n')
    for c in p.children:
        emit_part(c, lines, methods)


def write_java():
    methods = []
    rest_entries = ',\n'.join('            Map.entry("%s", new float[]{%s})' % (k, ', '.join(jf(v) for v in rot))
                               for k, rot in REST.items())
    roots = [p for p in ORDER if p.parent is None]
    for r in roots:
        emit_part(r, None, methods)
    src = f'''// GENERATED by tools/gen_model.py -- edit the generator, not this file.
package com.dentomas.deathgoddess.client.model;

import java.util.Map;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

public final class GoddessModelLayer {{
    public static final int EYE_COUNT = {len(EYES)};
    /** Eye sizes in model pixels, used to keep each pupil inside its lids. */
    public static final float[] EYE_WIDTH = {{{', '.join(jf(e[2]) for e in EYES)}}};
    public static final float[] EYE_HEIGHT = {{{', '.join(jf(e[3]) for e in EYES)}}};
    /** Rest rotation in degrees of each right arm joint; the left side mirrors y and z. */
    public static final Map<String, float[]> REST_DEGREES = Map.ofEntries(
{rest_entries});

    private GoddessModelLayer() {{
    }}

    public static LayerDefinition create() {{
        MeshDefinition mesh = new MeshDefinition();
{chr(10).join(f'        {java_ident(r.name)}(mesh.getRoot());' for r in roots)}
        return LayerDefinition.create(mesh, {TEX_W}, {TEX_H});
    }}

{chr(10).join(methods)}}}
'''
    os.makedirs(os.path.dirname(JAVA_OUT), exist_ok=True)
    with open(JAVA_OUT, 'w') as f:
        f.write(src)


# ---------------------------------------------------------------- preview
def rot_matrix(rx, ry, rz):
    cx, sx, cy, sy, cz, sz = math.cos(rx), math.sin(rx), math.cos(ry), math.sin(ry), math.cos(rz), math.sin(rz)
    mx = np.array([[1, 0, 0], [0, cx, -sx], [0, sx, cx]])
    my = np.array([[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]])
    mz = np.array([[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]])
    return mz @ my @ mx


def part_matrix(p, pose=None):
    m = np.eye(4)
    rot = np.radians(p.rot)
    piv = np.array(p.pivot, float)
    if pose and p.name in pose:
        extra = pose[p.name]
        rot = rot + np.radians(extra.get('rot', (0, 0, 0)))
        piv = piv + np.array(extra.get('pos', (0, 0, 0)))
    m[:3, :3] = rot_matrix(*rot)
    m[:3, 3] = piv
    return m


def box_quads(b):
    x0, y0, z0 = (b.o[i] - b.grow for i in range(3))
    x1, y1, z1 = (b.o[i] + b.size[i] + b.grow for i in range(3))
    if b.mirror:
        x0, x1 = x1, x0
    V = {7: (x0, y0, z0), 0: (x1, y0, z0), 1: (x1, y1, z0), 2: (x0, y1, z0),
         3: (x0, y0, z1), 4: (x1, y0, z1), 5: (x1, y1, z1), 6: (x0, y1, z1)}
    r = face_rects(b)

    def q(ids, rect, flipv=False):
        u, v, w, h = rect
        u1, v1, u2, v2 = u, v, u + w, v + h
        if flipv:
            v1, v2 = v2, v1
        uv = [(u2, v1), (u1, v1), (u1, v2), (u2, v2)]
        return [V[i] for i in ids], uv

    out = [q((4, 3, 7, 0), r['top']), q((1, 2, 6, 5), r['bottom'], True), q((7, 3, 6, 2), r['west']),
           q((0, 7, 2, 1), r['north']), q((4, 0, 1, 5), r['east']), q((3, 4, 5, 6), r['south'])]
    return [(vs, uv) for vs, uv in out if abs(uv[0][0] - uv[1][0]) > 0 and abs(uv[0][1] - uv[2][1]) > 0]


def render(atlas, yaw, path, pose=None, scale=9, size=(760, 760)):
    W, H = size
    color = np.zeros((H, W, 3))
    color[:] = (0.16, 0.15, 0.16)
    zbuf = np.full((H, W), 1e9)
    view = np.eye(4)
    view[:3, :3] = rot_matrix(0, math.radians(yaw), 0)
    light = np.array([0.35, -0.6, -0.7])
    light /= np.linalg.norm(light)
    mats = {}
    for p in ORDER:
        m = part_matrix(p, pose)
        mats[p.name] = m if p.parent is None else mats[p.parent.name] @ m
    for p in ORDER:
        M = view @ mats[p.name]
        for b in p.boxes:
            for vs, uv in box_quads(b):
                P = (M @ np.hstack([np.array(vs), np.ones((4, 1))]).T).T[:, :3]
                n = np.cross(P[1] - P[0], P[2] - P[0])
                ln = np.linalg.norm(n)
                shade = 0.55 + 0.45 * abs(n @ light) / ln if ln > 1e-9 else 1
                sx = P[:, 0] * scale + W / 2
                sy = (P[:, 1] + 8) * scale + H / 2
                for tri in ((0, 1, 2), (0, 2, 3)):
                    ax, ay, bx, by, cx, cy = sx[tri[0]], sy[tri[0]], sx[tri[1]], sy[tri[1]], sx[tri[2]], sy[tri[2]]
                    den = (by - cy) * (ax - cx) + (cx - bx) * (ay - cy)
                    if abs(den) < 1e-9:
                        continue
                    xa, xb = max(0, int(min(ax, bx, cx))), min(W - 1, int(max(ax, bx, cx)) + 1)
                    ya, yb = max(0, int(min(ay, by, cy))), min(H - 1, int(max(ay, by, cy)) + 1)
                    if xa > xb or ya > yb:
                        continue
                    gy, gx = np.mgrid[ya:yb + 1, xa:xb + 1]
                    px, py = gx + 0.5, gy + 0.5
                    l0 = ((by - cy) * (px - cx) + (cx - bx) * (py - cy)) / den
                    l1 = ((cy - ay) * (px - cx) + (ax - cx) * (py - cy)) / den
                    l2 = 1 - l0 - l1
                    ins = (l0 >= 0) & (l1 >= 0) & (l2 >= 0)
                    z = l0 * P[tri[0], 2] + l1 * P[tri[1], 2] + l2 * P[tri[2], 2]
                    tu = l0 * uv[tri[0]][0] + l1 * uv[tri[1]][0] + l2 * uv[tri[2]][0]
                    tv = l0 * uv[tri[0]][1] + l1 * uv[tri[1]][1] + l2 * uv[tri[2]][1]
                    ti = np.clip((tu * S).astype(int), 0, atlas.shape[1] - 1)
                    tj = np.clip((tv * S).astype(int), 0, atlas.shape[0] - 1)
                    tex = atlas[tj, ti]
                    ok = ins & (tex[..., 3] > 0.1) & (z <= zbuf[gy, gx])
                    zbuf[gy[ok], gx[ok]] = z[ok]
                    color[gy[ok], gx[ok]] = tex[ok][:, :3] * shade
    os.makedirs(os.path.dirname(path), exist_ok=True)
    Image.fromarray((np.clip(color, 0, 1) * 255).astype(np.uint8)).save(path)


def main():
    build()
    uniq, used = pack()
    atlas, glow, rage = paint(uniq)
    save(atlas, 'goddess_of_death.png')
    save(glow, 'goddess_of_death_glow.png')
    save(rage, 'goddess_of_death_enraged.png')
    extras()
    write_java()
    boxes = sum(len(p.boxes) for p in ORDER)
    print(f'{len(ORDER)} parts, {boxes} boxes, atlas rows used {used}/{TEX_H}')
    if '--preview' in sys.argv:
        out = os.path.join(ROOT, 'build/preview')
        for name, yaw in (('front', 0), ('quarter', 35), ('side', 90), ('back', 180)):
            render(atlas, yaw, os.path.join(out, f'{name}.png'))
        ims = [Image.open(os.path.join(out, f'{n}.png')) for n in ('front', 'quarter', 'side', 'back')]
        sheet = Image.new('RGB', (ims[0].width * 2, ims[0].height * 2))
        for i, im in enumerate(ims):
            sheet.paste(im, ((i % 2) * im.width, (i // 2) * im.height))
        sheet.save(os.path.join(out, 'sheet.png'))
        print('previews in', out)


if __name__ == '__main__':
    main()
