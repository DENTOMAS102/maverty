#!/usr/bin/env python3
"""Builds the three ruins as structure templates (.nbt), block by block.

    python3 tools/gen_structures.py             # write data/deathgoddess/structures/*.nbt
    python3 tools/gen_structures.py --preview   # also render build/preview/structure_*.png

In every structure y=0 is the ground surface (it replaces the top block of the terrain), y>=1 is
above ground and y=-1,-2 are foundations. The worldgen JSON sinks each piece by FOUNDATION to match.
Positions that are never set are left as the terrain was. Output is deterministic.
"""
import gzip
import io
import math
import os
import random
import struct
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, 'src/main/resources/data/deathgoddess/structures')
DATA_VERSION = 3120     # Minecraft 1.19.2
FOUNDATION = 2


# ---------------------------------------------------------------- minimal NBT writer
class Byte(int):
    pass


class Float(float):
    pass


def tag_id(v):
    if isinstance(v, Byte):
        return 1
    if isinstance(v, Float):
        return 5
    if isinstance(v, int):
        return 3
    if isinstance(v, float):
        return 6
    if isinstance(v, str):
        return 8
    if isinstance(v, list):
        return 9
    if isinstance(v, dict):
        return 10
    raise TypeError(type(v))


def write_string(out, s):
    data = s.encode('utf-8')
    out.write(struct.pack('>H', len(data)) + data)


def write_payload(out, v):
    t = tag_id(v)
    if t == 1:
        out.write(struct.pack('>b', v))
    elif t == 3:
        out.write(struct.pack('>i', v))
    elif t == 5:
        out.write(struct.pack('>f', v))
    elif t == 6:
        out.write(struct.pack('>d', v))
    elif t == 8:
        write_string(out, v)
    elif t == 9:
        out.write(struct.pack('>bi', tag_id(v[0]) if v else 0, len(v)))
        for item in v:
            write_payload(out, item)
    else:
        for key, item in v.items():
            out.write(struct.pack('>b', tag_id(item)))
            write_string(out, key)
            write_payload(out, item)
        out.write(b'\x00')


def write_nbt(path, root):
    raw = io.BytesIO()
    raw.write(b'\x0a')
    write_string(raw, '')
    write_payload(raw, root)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'wb') as f:
        with gzip.GzipFile(fileobj=f, mode='wb', mtime=0) as gz:
            gz.write(raw.getvalue())


# ---------------------------------------------------------------- builder
def smooth_noise(x, z, scale, seed):
    """Cheap value noise in 0..1."""
    def corner(ix, iz):
        return random.Random(ix * 73856093 ^ iz * 19349663 ^ seed * 83492791).random()
    fx, fz = x / scale, z / scale
    ix, iz = math.floor(fx), math.floor(fz)
    tx, tz = fx - ix, fz - iz
    tx, tz = tx * tx * (3 - 2 * tx), tz * tz * (3 - 2 * tz)
    top = corner(ix, iz) * (1 - tx) + corner(ix + 1, iz) * tx
    bottom = corner(ix, iz + 1) * (1 - tx) + corner(ix + 1, iz + 1) * tx
    return top * (1 - tz) + bottom * tz


def line3d(a, b):
    n = max(abs(b[0] - a[0]), abs(b[1] - a[1]), abs(b[2] - a[2]))
    return [tuple(round(a[i] + (b[i] - a[i]) * t / max(1, n)) for i in range(3)) for t in range(n + 1)]


class Build:
    def __init__(self, seed):
        self.blocks = {}
        self.entities = []
        self.rng = random.Random(seed)
        self.seed = seed

    def set(self, x, y, z, block, nbt=None):
        if block is not None:
            self.blocks[(x, y, z)] = (block(self.rng) if callable(block) else block, nbt)

    def get(self, x, y, z):
        entry = self.blocks.get((x, y, z))
        return entry[0] if entry else None

    def clear(self, x, y, z):
        """Forget a block so the terrain (or air) that was there stays."""
        self.blocks.pop((x, y, z), None)

    def fill(self, x0, y0, z0, x1, y1, z1, block):
        for x in range(min(x0, x1), max(x0, x1) + 1):
            for y in range(min(y0, y1), max(y0, y1) + 1):
                for z in range(min(z0, z1), max(z0, z1) + 1):
                    self.set(x, y, z, block)

    def solid(self, x, y, z):
        block = self.get(x, y, z)
        return block is not None and block != 'air' and 'carpet' not in block and 'candle' not in block

    def pick(self, options, weights):
        return self.rng.choices(options, weights)[0]

    def chance(self, p):
        return self.rng.random() < p

    def entity(self, x, y, z, nbt):
        self.entities.append((x + 0.5, float(y), z + 0.5, nbt))

    def foundation(self, x, z, block='cobblestone'):
        for y in range(-FOUNDATION, 1):
            if self.get(x, y, z) is None:
                self.set(x, y, z, block)

    def dead_ground(self, inside, extent, palette, weights, decor=0.05):
        """Withered soil at y=0 wherever inside(x, z) > a noise threshold; nothing grows near her."""
        for x in range(-extent, extent + 1):
            for z in range(-extent, extent + 1):
                strength = inside(x, z)
                if strength <= 0 or self.get(x, 0, z) is not None:
                    continue
                if strength < 1 and smooth_noise(x, z, 3.0, self.seed) > strength:
                    continue
                soil = self.pick(palette, weights)
                self.set(x, 0, z, soil)
                if self.get(x, 1, z) is None:
                    if soil in ('coarse_dirt', 'dirt', 'rooted_dirt') and self.chance(decor):
                        self.set(x, 1, z, 'dead_bush')
                    else:
                        # Clear grass and flowers standing on the old surface.
                        self.set(x, 1, z, 'air')

    def save(self, name):
        ys = [p[1] for p in self.blocks]
        assert min(ys) == -FOUNDATION, f'{name}: lowest block is y={min(ys)}, expected {-FOUNDATION}'
        lo = [min(p[i] for p in self.blocks) for i in range(3)]
        hi = [max(p[i] for p in self.blocks) for i in range(3)]
        size = [hi[i] - lo[i] + 1 for i in range(3)]
        assert max(size) <= 48, f'{name}: {size} exceeds the 48-block template limit'
        palette, index, blocks = [], {}, []
        for pos in sorted(self.blocks):
            spec, nbt = self.blocks[pos]
            if spec not in index:
                index[spec] = len(palette)
                block_name, _, props = spec.partition('[')
                entry = {'Name': 'minecraft:' + block_name}
                if props:
                    entry['Properties'] = dict(p.split('=') for p in props.rstrip(']').split(','))
                palette.append(entry)
            block = {'pos': [pos[i] - lo[i] for i in range(3)], 'state': index[spec]}
            if nbt:
                block['nbt'] = nbt
            blocks.append(block)
        entities = [{'pos': [x - lo[0], y - lo[1], z - lo[2]],
                     'blockPos': [math.floor(x) - lo[0], math.floor(y) - lo[1], math.floor(z) - lo[2]],
                     'nbt': nbt} for x, y, z, nbt in self.entities]
        write_nbt(os.path.join(OUT, name + '.nbt'),
                  {'DataVersion': DATA_VERSION, 'size': size, 'palette': palette, 'blocks': blocks, 'entities': entities})
        print(f'{name}: {size[0]}x{size[1]}x{size[2]}, {len(blocks)} blocks, {len(entities)} entities, '
              f'{hi[1]} blocks above ground')
        return size


def chest(table, facing):
    return f'chest[facing={facing}]', {'id': 'minecraft:chest', 'LootTable': 'deathgoddess:chests/' + table}


def barrel(table):
    return 'barrel[facing=up]', {'id': 'minecraft:barrel', 'LootTable': 'deathgoddess:chests/' + table}


def weathered(rng):
    return rng.choices(['stone_bricks', 'cracked_stone_bricks', 'mossy_stone_bricks', 'cobblestone', 'andesite'],
                       [50, 20, 12, 12, 6])[0]


def sooty(soot):
    def choose(rng):
        if rng.random() < soot:
            return rng.choices(['blackstone', 'polished_blackstone_bricks', 'cracked_polished_blackstone_bricks',
                                'deepslate_bricks', 'cracked_deepslate_bricks', 'smooth_basalt'], [30, 22, 22, 12, 8, 6])[0]
        return weathered(rng)
    return choose


def rubble(b, cx, cz, radius, height, palette=('cobblestone', 'stone_bricks', 'blackstone', 'gravel', 'cracked_stone_bricks')):
    for x in range(cx - radius, cx + radius + 1):
        for z in range(cz - radius, cz + radius + 1):
            d = math.hypot(x - cx, z - cz) / radius
            top = round(height * (1 - d) + (smooth_noise(x, z, 1.5, b.seed + 5) - 0.5) * 1.6)
            for y in range(1, top + 1):
                if not b.solid(x, y, z):
                    b.set(x, y, z, b.rng.choice(palette))
            if top >= 1 and b.get(x, top + 1, z) is None and b.chance(0.3):
                b.set(x, top + 1, z, b.rng.choice(['cobblestone_slab', 'stone_brick_slab', 'blackstone_slab']))


DEAD_SOIL = (['coarse_dirt', 'dirt', 'gravel', 'rooted_dirt'], [60, 18, 10, 12])


# ---------------------------------------------------------------- 1. the burnt-out church
def burnt_church():
    b = Build(11)
    W, Z0, Z1, WALL = 12, 4, 24, 7          # nave: x 0..12, z 4..24, walls 7 high
    TX0, TX1, TOWER = 4, 8, 16              # tower: x 4..8, z 0..4

    def soot_at(y, tower=False):
        return min(0.95, 0.05 + y / 20) if tower else min(0.95, 0.15 + y / 9)

    # Floors and foundations.
    for x in range(0, W + 1):
        for z in range(0, Z1 + 1):
            in_nave = z >= Z0
            in_tower = TX0 <= x <= TX1 and z <= Z0
            if not (in_nave or in_tower):
                continue
            scorched = smooth_noise(x, z, 4.0, 3) > 0.55
            floor = b.pick(['blackstone', 'smooth_basalt', 'coarse_dirt'], [5, 3, 2]) if scorched else weathered(b.rng)
            b.set(x, 0, z, floor)
            b.foundation(x, z)
            b.fill(x, 1, z, x, 3, z, 'air')

    # Nave walls, with a ragged top edge and one collapsed stretch on the west side.
    def wall_top(x, z):
        loss = int(3.6 * smooth_noise(x * 3 + z, z, 2.5, 7))
        if x == 0 and 12 <= z <= 16:
            loss = 5 + (z % 2)
        return WALL - loss

    for z in range(Z0, Z1 + 1):
        for x in (0, W):
            for y in range(1, wall_top(x, z) + 1):
                b.set(x, y, z, sooty(soot_at(y)))
    for x in range(0, W + 1):
        for z in (Z0, Z1):
            for y in range(1, WALL + 1):
                b.set(x, y, z, sooty(soot_at(y)))
            # Gables: the front one has lost its peak, the back one still holds the window.
            for k in range(1, 7):
                if k <= x <= W - k and (z == Z1 or k <= 2 + int(3 * smooth_noise(x, 0, 2.0, 9))):
                    b.set(x, WALL + k, z, sooty(soot_at(WALL + k)))

    # Pointed windows down both sides; a few twisted bars remain.
    for z in (8, 12, 16, 20):
        for x in (0, W):
            for y in (3, 4, 5):
                if b.get(x, y, z) is not None:
                    b.set(x, y, z, 'iron_bars' if b.chance(0.3) else 'air')

    # The round window over the altar: an eye that the fire did not close.
    for dx in (-1, 0, 1):
        for dy in (-1, 0, 1):
            ring = 'polished_blackstone' if dx and dy else ('red_stained_glass' if b.chance(0.6) else 'air')
            b.set(6 + dx, 10 + dy, Z1, 'crying_obsidian' if dx == 0 and dy == 0 else ring)

    # Tower: hollow, broken off at the top, the bell still hanging from a charred beam.
    for x in range(TX0, TX1 + 1):
        for z in range(0, Z0 + 1):
            edge = x in (TX0, TX1) or z in (0, Z0)
            top = TOWER - int(5 * smooth_noise(x * 2, z * 2, 2.0, 21))
            for y in range(1, TOWER + 1):
                if edge and y <= top:
                    b.set(x, y, z, sooty(soot_at(y, True)))
                elif not edge and y > 3:
                    b.set(x, y, z, 'air')
    for y in (1, 2):
        b.set(6, y, 0, 'air')
        b.set(6, y, Z0, 'air')
    b.set(6, 3, Z0, 'air')
    for y in (6, 7, 11, 12):
        for x, z in ((6, 0), (TX0, 2), (TX1, 2)):
            if b.get(x, y, z) is not None:
                b.set(x, y, z, 'air')
    for x in range(TX0 + 1, TX1):
        b.set(x, 13, 2, 'basalt[axis=x]')
    b.set(6, 12, 2, 'bell[attachment=ceiling,facing=north]')

    # What is left of the roof: charred rafters, a ridge beam, a few tiles over the altar.
    for z in (7, 11, 15, 19, 22):
        keep_left, keep_right = b.rng.randint(0, 6), b.rng.randint(1, 6)
        if z >= 19:
            keep_left, keep_right = 6, 6
        for k in range(1, 7):
            if k <= keep_left and wall_top(0, z) == WALL:
                b.set(k, WALL + k, z, 'basalt[axis=x]')
            if k <= keep_right and wall_top(W, z) == WALL:
                b.set(W - k, WALL + k, z, 'basalt[axis=x]')
    for z in range(18, Z1):
        b.set(6, WALL + 6, z, 'basalt[axis=z]')
    for z in range(20, Z1):
        for k in range(1, 6):
            for x in (k, W - k):
                if b.chance(0.55):
                    b.set(x, WALL + k + 1, z, 'deepslate_tile_slab[type=bottom]')
    # Rafters that came down.
    for a, c in (((2, 1, 9), (5, 1, 13)), ((10, 1, 10), (7, 2, 15)), ((3, 1, 17), (8, 1, 18))):
        for x, y, z in line3d(a, c):
            b.set(x, y, z, 'basalt[axis=z]')

    # Pews, most of them burnt away; ash and the remains of the aisle carpet.
    for z in range(8, 20, 2):
        for x in (2, 3, 4, 8, 9, 10):
            if b.get(x, 1, z) not in (None, 'air'):
                continue
            roll = b.rng.random()
            if roll < 0.55:
                b.set(x, 1, z, 'dark_oak_stairs[facing=north,half=bottom,shape=straight]')
            elif roll < 0.7:
                b.set(x, 1, z, 'basalt[axis=x]')
    for x in range(1, W):
        for z in range(Z0 + 1, Z1):
            if b.get(x, 1, z) == 'air' and b.solid(x, 0, z):
                if x == 6 and z < 20 and b.chance(0.4):
                    b.set(x, 1, z, 'red_carpet')
                elif b.chance(0.09):
                    b.set(x, 1, z, b.rng.choice(['gray_carpet', 'black_carpet', 'light_gray_carpet']))

    # Dais and altar.
    for x in range(3, 10):
        b.set(x, 1, 21, 'stone_brick_slab[type=bottom]')
        for z in (22, 23):
            b.set(x, 1, z, sooty(0.5))
            b.set(x, 2, z, 'air')
    for x in (5, 6, 7):
        b.set(x, 2, 22, 'polished_blackstone')
    b.set(5, 3, 22, 'black_candle[candles=3,lit=false]')
    b.set(7, 3, 22, 'red_candle[candles=2,lit=true]')
    b.set(6, 3, 22, 'skeleton_skull[rotation=8]')
    b.set(9, 2, 23, *chest('burnt_church', 'north'))
    b.set(3, 2, 23, 'cobweb')
    b.set(6, 1, 19, 'lectern[facing=south]')
    b.set(4, 2, 22, 'bone_block[axis=x]')

    # Embers that never quite went out.
    for x, z in ((2, 6), (10, 20)):
        b.set(x, 0, z, 'blackstone')
        b.set(x, 1, z, 'campfire[lit=true,signal_fire=false,facing=north]')
        b.set(x + (1 if x < 6 else -1), 1, z, 'basalt[axis=z]')
    rubble(b, 0, 14, 3, 2.4)
    rubble(b, 6, 2, 1, 1.5)
    for y in (1, 2):
        b.set(6, y, 0, 'air')

    # Graveyard on the east side, one grave lying open.
    for x in range(14, 21):
        for z in range(6, 23):
            if (x in (14, 20) or z in (6, 22)) and b.chance(0.6) and not (x == 14 and 13 <= z <= 15):
                b.set(x, 1, z, b.rng.choice(['cobblestone_wall', 'mossy_cobblestone_wall']))
    for gx in (16, 18):
        for gz in (8, 11, 14, 17, 20):
            if (gx, gz) == (18, 14):
                for z in (gz, gz + 1):
                    b.set(gx, 0, z, 'air')
                    b.set(gx, -1, z, 'coarse_dirt')
                b.set(gx, -1, gz, 'bone_block[axis=z]')
                continue
            stone = b.pick(['cobblestone_wall', 'mossy_cobblestone_wall', 'stone_brick_wall', 'stone_brick_slab[type=bottom]'],
                           [3, 3, 3, 2])
            b.set(gx, 1, gz, stone)
            for z in (gz + 1, gz + 2):
                b.set(gx, 0, z, b.rng.choice(['coarse_dirt', 'rooted_dirt']))
                b.set(gx, 1, z, 'air')
            if b.chance(0.3):
                b.set(gx, 1, gz + 1, 'wither_rose')

    # A gravel path to the door, and dead earth all around.
    for z in range(-4, 0):
        for x in (5, 6, 7):
            if b.chance(0.8):
                b.set(x, 0, z, 'gravel')
                b.set(x, 1, z, 'air')

    def near(x, z):
        dx = max(-3 - x, 0, x - 23)
        dz = max(-5 - z, 0, z - 27)
        inside = -3 <= x <= 23 and -5 <= z <= 27
        edge = min(x + 3, 23 - x, z + 5, 27 - z)
        return 0 if not inside or dx or dz else min(1.0, 0.35 + edge * 0.3)

    b.dead_ground(near, 30, *DEAD_SOIL, decor=0.06)
    return b


# ---------------------------------------------------------------- 2. the ruined statue (19 blocks tall)
def statue_stone(y):
    def choose(rng):
        if rng.random() < max(0.0, 0.5 - y * 0.06):
            return rng.choice(['mossy_cobblestone', 'mossy_stone_bricks'])
        return rng.choices(['stone', 'andesite', 'cobblestone', 'stone_bricks', 'cracked_stone_bricks', 'tuff'],
                           [34, 24, 12, 14, 10, 6])[0]
    return choose


def ruined_statue():
    b = Build(23)
    # Plinth: two worn steps, an inscription nobody finished, a niche at the back.
    for x in range(-4, 5):
        for z in range(-4, 5):
            b.foundation(x, z)
            b.set(x, 0, z, 'cobblestone')
            b.set(x, 1, z, weathered)
            if max(abs(x), abs(z)) <= 3:
                b.set(x, 2, z, weathered)
            elif b.chance(0.45):
                b.set(x, 2, z, 'stone_brick_slab[type=bottom]')
    for x in range(-2, 3):
        b.set(x, 1, -4, 'chiseled_stone_bricks')
    b.set(-2, 2, -4, 'red_candle[candles=3,lit=false]')
    b.set(2, 2, -4, 'black_candle[candles=2,lit=false]')
    b.set(0, 2, -4, 'wither_rose' if False else 'red_candle[candles=1,lit=true]')
    b.set(0, 1, 4, *chest('ruined_statue', 'south'))
    b.clear(0, 2, 4)

    # Gown: eight layers narrowing to an impossible waist, with shadowed folds.
    for y in range(3, 11):
        t = (y - 3) / 7
        rx = 4.3 - 2.6 * t ** 0.8
        rz = 3.4 - 2.0 * t ** 0.8
        for x in range(-5, 6):
            for z in range(-4, 5):
                d = (x / rx) ** 2 + (z / rz) ** 2
                if d > 1.0:
                    continue
                surface = ((abs(x) + 1) / rx) ** 2 + (z / rz) ** 2 > 1.0 or (x / rx) ** 2 + ((abs(z) + 1) / rz) ** 2 > 1.0
                fold = surface and int(math.atan2(z, x) * 9 / math.pi) % 2 == 0
                b.set(x, y, z, 'tuff' if fold else statue_stone(y))
    # The crimson panel down the front of the dress, half weathered away.
    for y in range(3, 11):
        z = min(zz for (xx, yy, zz) in b.blocks if xx == 0 and yy == y)
        if b.chance(0.75):
            b.set(0, y, z, 'red_nether_bricks')

    # Torso and shoulders.
    for y in (11, 12, 13):
        for x in range(-1, 2):
            for z in (-1, 0):
                b.set(x, y, z, statue_stone(y))
    for x in (-2, 2):
        for z in (-1, 0):
            b.set(x, 13, z, statue_stone(13))

    # Head, entirely under the veil.
    for y in (14, 15, 16):
        for x in range(-1, 2):
            for z in range(-1, 2):
                if y == 16 and abs(x) == 1 and abs(z) == 1:
                    continue
                b.set(x, y, z, 'red_nether_bricks' if z == -1 else 'nether_bricks')
    for x, bottom in ((-1, 12), (0, 10), (1, 11)):
        for y in range(bottom, 16):
            b.set(x, y, -2, 'red_nether_bricks')
    for x in (-2, 2):
        b.set(x, 15, 0, 'nether_bricks')
        b.set(x, 14, 0, 'red_nether_brick_wall')

    # Crown of twisted black metal.
    for x in range(-1, 2):
        for z in range(-1, 2):
            if x or z:
                b.set(x, 17, z, 'polished_blackstone_wall')
    for x, z in ((-1, -1), (1, -1), (-1, 1), (1, 1), (0, -1)):
        b.set(x, 18, z, 'iron_bars')
    b.set(0, 19, -1, 'iron_bars')

    # The halo of eyes behind her head: a ring with four spokes, one arc fallen away.
    fallen = []
    for x in range(-4, 5):
        for y in range(11, 20):
            d = math.hypot(x, y - 15)
            angle = math.degrees(math.atan2(y - 15, x))
            ring = 3.4 <= d <= 4.45
            spoke = (x == 0 or y == 15) and 1.5 < d < 3.4
            if not (ring or spoke):
                continue
            if 15 < angle < 100 and d > 2.4:
                fallen.append((x, y))
                continue
            eye = (ring and (x, y) in ((-4, 15), (0, 11), (-3, 18), (3, 12), (-3, 12), (4, 15))) or (spoke and d < 2.6)
            b.set(x, y, 2, 'deepslate_redstone_ore' if eye else b.rng.choice(['blackstone', 'polished_blackstone']))

    # Six arms. Each is shoulder, elbow, hand; `keep` is how many blocks survive.
    def arm(side, points, keep=99, fingers=()):
        cells = []
        for a, c in zip(points, points[1:]):
            for cell in line3d(a, c):
                if cell not in cells:
                    cells.append(cell)
        for i, (x, y, z) in enumerate(cells[:keep]):
            joint = (x, y, z) == points[1]
            b.set(side * x, y, z, 'chiseled_stone_bricks' if joint else statue_stone(y))
        if keep >= len(cells):
            hx, hy, hz = cells[-1]
            for dx, dy, dz in fingers:
                b.set(side * (hx + dx), hy + dy, hz + dz, 'andesite_wall')
        return cells

    upper = [(3, 13, 0), (5, 15, 0), (7, 18, -1)]
    middle = [(2, 12, 0), (6, 12, -1), (9, 11, -2)]
    lower = [(2, 11, 0), (4, 9, -1), (5, 6, -2)]
    arm(1, upper, fingers=((0, 1, 0), (1, 0, 0)))
    arm(1, middle, fingers=((1, 0, 0), (0, -1, 0)))
    arm(1, lower, keep=2)                                    # broken off at the shoulder
    arm(-1, upper, keep=4)                                   # snapped at the elbow
    arm(-1, middle, fingers=((1, 0, 0), (0, -1, 0)))
    arm(-1, lower, fingers=((0, -1, 0), (0, -1, -1)))
    # Threads still hang from the hands that are left.
    for x, y, z, length in ((9, 10, -2, 3), (-9, 10, -2, 2), (7, 17, -1, 2)):
        for i in range(length):
            if b.get(x, y - i, z) is None:
                b.set(x, y - i, z, 'chain[axis=y]')

    # Where the pieces fell.
    for x, y, z in line3d((-10, 1, -3), (-6, 1, -6)):
        b.set(x, y, z, statue_stone(1))
    b.set(-11, 1, -3, 'andesite_wall')
    b.set(-6, 2, -6, 'chiseled_stone_bricks')
    for x, y, z in line3d((6, 1, -5), (8, 1, -7)):
        b.set(x, y, z, statue_stone(1))
    for i, (x, y) in enumerate(fallen[::2]):
        gx, gz = 4 + (i * 5) % 6, 4 + (i * 3) % 5
        b.set(gx, 1, gz, 'deepslate_redstone_ore' if i == 2 else 'blackstone')

    # The masons' scaffold, left where it stood.
    for x, z, top in ((6, 3, 8), (9, 3, 6), (6, 6, 7), (9, 6, 4)):
        for y in range(1, top + 1):
            if b.get(x, y, z) is None:
                b.set(x, y, z, 'spruce_fence')
    for x in range(6, 10):
        for z in range(3, 7):
            if b.get(x, 5, z) is None and b.chance(0.6):
                b.set(x, 5, z, 'spruce_slab[type=top]')

    # Weathering: a few stones missing, then the dead ring around her.
    for pos in [p for p, (name, _) in b.blocks.items() if 3 <= p[1] <= 13 and name in ('stone', 'andesite', 'cobblestone')]:
        if b.chance(0.035):
            b.clear(*pos)

    def near(x, z):
        d = math.hypot(x, z)
        return 0 if d > 13 else min(1.0, (13 - d) * 0.3)

    b.dead_ground(near, 14, *DEAD_SOIL, decor=0.07)
    for x, z in ((-2, -6), (1, -7), (3, -5), (-5, 5)):
        if b.get(x, 0, z) in ('coarse_dirt', 'dirt', 'rooted_dirt'):
            b.set(x, 1, z, 'wither_rose')
    for x, z, rot in ((-3, -8, 3), (7, 0, 11)):
        b.set(x, 1, z, f'skeleton_skull[rotation={rot}]')
    rubble(b, -7, 3, 2, 1.6, ('cobblestone', 'mossy_cobblestone', 'andesite', 'stone'))
    return b


# ---------------------------------------------------------------- 3. the abandoned Aztetian village
def skeleton(helmet='golden_helmet', chest_armor=None):
    armor = [{}, {}, {'id': 'minecraft:' + chest_armor, 'Count': Byte(1)} if chest_armor else {},
             {'id': 'minecraft:' + helmet, 'Count': Byte(1)}]
    return {'id': 'minecraft:skeleton', 'PersistenceRequired': Byte(1), 'ArmorItems': armor,
            'ArmorDropChances': [Float(0.0), Float(0.0), Float(0.0), Float(0.0)]}


def temple_stone(rng):
    return rng.choices(['stone_bricks', 'mossy_stone_bricks', 'cobblestone', 'cracked_stone_bricks', 'mossy_cobblestone'],
                       [40, 22, 18, 12, 8])[0]


def aztetian_village():
    b = Build(37)
    PZ = -10                                                  # pyramid centre is (0, PZ)
    PLAZA = (0, 7)

    # Stepped pyramid: three tiers, a painted band round the top of each.
    for tier, half in enumerate((7, 5, 3)):
        for x in range(-half, half + 1):
            for z in range(PZ - half, PZ + half + 1):
                edge = abs(x) == half or abs(z - PZ) == half
                corner = abs(x) == half and abs(z - PZ) == half
                for y in (1 + tier * 2, 2 + tier * 2):
                    if not edge:
                        block = 'cobblestone'
                    elif corner:
                        block = 'chiseled_stone_bricks'
                    elif y % 2 == 0:
                        block = ('orange_terracotta', 'chiseled_stone_bricks', 'red_terracotta', None)[(x + z) % 4]
                        block = block or temple_stone
                    else:
                        block = temple_stone
                    if edge and b.chance(0.04) and not corner:
                        continue
                    b.set(x, y, z, block)
                if tier == 0:
                    b.set(x, 0, z, 'cobblestone')
                    b.foundation(x, z)
    # The eye, carved into both flanks.
    for side in (-1, 1):
        b.set(side * 5, 4, PZ, 'deepslate_redstone_ore')
        for dz in (-1, 1):
            b.set(side * 5, 4, PZ + dz, 'polished_blackstone')
        b.set(side * 5, 3, PZ, 'polished_blackstone')

    # Stairway up the south face.
    for i in range(6):
        z, y = PZ + 9 - i, 1 + i
        for x in (-1, 0, 1):
            b.set(x, y, z, 'stone_brick_stairs[facing=north,half=bottom,shape=straight]')
            b.fill(x, 1, z, x, y - 1, z, 'cobblestone')
            b.fill(x, y + 1, z, x, y + 3, z, 'air')
        for x in (-2, 2):
            b.fill(x, 1, z, x, y, z, temple_stone)
        b.foundation(0, z)

    # Shrine on the summit: four pillars, half a roof, the altar and what was left on it.
    for x in (-2, 2):
        for z in (PZ - 2, PZ + 2):
            for y in (7, 8, 9):
                b.set(x, y, z, 'mossy_stone_bricks' if y == 7 else 'chiseled_stone_bricks')
    for x in range(-2, 3):
        for z in range(PZ - 2, PZ + 3):
            if b.chance(0.6) or (abs(x) == 2 and abs(z - PZ) == 2):
                b.set(x, 10, z, 'stone_brick_slab[type=bottom]')
    b.set(0, 7, PZ, 'chiseled_stone_bricks')
    b.set(0, 8, PZ, 'skeleton_skull[rotation=0]')
    b.set(-1, 7, PZ, 'raw_gold_block')
    b.set(1, 7, PZ, 'cauldron')
    b.set(-1, 7, PZ + 1, 'red_candle[candles=4,lit=false]')
    b.set(1, 7, PZ + 1, 'red_candle[candles=2,lit=false]')
    b.set(0, 7, PZ - 2, *chest('aztetian_temple', 'south'))
    b.entity(0, 7, PZ + 2, skeleton(chest_armor='golden_chestplate'))
    b.entity(1, 3, PZ + 7, skeleton())

    # Huts of mud brick under dried thatch, in every state of collapse.
    def hut(cx, cz, roof):
        door = min(((cx + dx, cz + dz) for dx in range(-3, 4) for dz in range(-3, 4) if 2.5 <= math.hypot(dx, dz) <= 3.5),
                   key=lambda p: math.hypot(p[0] - PLAZA[0], p[1] - PLAZA[1]))
        for dx in range(-4, 5):
            for dz in range(-4, 5):
                d = math.hypot(dx, dz)
                x, z = cx + dx, cz + dz
                if d <= 3.5:
                    b.set(x, 0, z, 'packed_mud')
                    b.foundation(x, z, 'packed_mud')
                if 2.5 <= d <= 3.5:
                    for y in (1, 2, 3):
                        if (x, z) == door and y < 3:
                            b.set(x, y, z, 'air')
                        elif not (y == 3 and b.chance(0.25)) and not (roof == 'gone' and y >= 2 and b.chance(0.5)):
                            b.set(x, y, z, b.pick(['mud_bricks', 'packed_mud'], [7, 3]))
                elif d < 2.5:
                    b.fill(x, 1, z, x, 3, z, 'air')
                for y, radius in ((4, 3.6), (5, 2.3), (6, 1.0)):
                    if d <= radius and roof != 'gone' and not (roof == 'half' and dx > 0) and b.chance(0.88):
                        b.set(x, y, z, 'hay_block' if b.chance(0.8) else 'spruce_planks')
        b.set(cx - 1, 1, cz + 1, 'yellow_carpet')
        b.set(cx, 1, cz + 1, 'yellow_carpet')
        b.set(cx + 1, 1, cz - 1, 'potted_dead_bush')

    huts = [(-13, 4, 'half'), (13, 5, 'intact'), (-10, 16, 'gone'), (10, 16, 'half'), (0, 19, 'intact')]
    for cx, cz, roof in huts:
        hut(cx, cz, roof)
    b.set(13, 1, 4, *barrel('aztetian_hut'))
    b.set(-1, 1, 20, *barrel('aztetian_hut'))
    b.entity(-13, 1, 4, skeleton())
    b.entity(1, 1, 19, skeleton())

    # The plaza: a cold fire pit, logs to sit on, skulls on poles, two painted totems.
    px, pz = PLAZA
    b.set(px, 0, pz, 'cobblestone')
    b.set(px, 1, pz, 'campfire[lit=false,signal_fire=false,facing=north]')
    for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        b.set(px + dx, 1, pz + dz, 'cobblestone_slab[type=bottom]')
    for (x, z), axis in (((px - 3, pz + 1), 'z'), ((px - 3, pz), 'z'), ((px + 1, pz + 3), 'x'), ((px + 2, pz + 3), 'x')):
        b.set(x, 1, z, f'jungle_log[axis={axis}]')
    poles = [(px + round(6 * math.cos(a)), pz + round(6 * math.sin(a))) for a in (0.5, 1.6, 2.7, 3.7, 4.9, 5.8)]
    for i, (x, z) in enumerate(poles + [(-3, PZ + 10), (3, PZ + 10)]):
        b.set(x, 0, z, 'coarse_dirt')
        b.set(x, 1, z, 'jungle_fence')
        b.set(x, 2, z, 'jungle_fence')
        b.set(x, 3, z, f'skeleton_skull[rotation={(i * 5) % 16}]')
    for x in (-6, 6):
        b.set(x, 0, -1, 'cobblestone')
        for y, block in enumerate(('mossy_stone_bricks', 'chiseled_stone_bricks', 'red_terracotta', 'chiseled_stone_bricks',
                                   'orange_terracotta'), start=1):
            b.set(x, y, -1, block)
    for x in (-1, 0, 1):
        for z in range(PZ + 10, pz - 1):
            if b.chance(0.7):
                b.set(x, 0, z, 'mud_bricks')
                b.set(x, 1, z, 'air')
    for x, z in ((px + 2, pz - 1), (px - 2, pz + 3), (px + 4, pz + 4)):
        b.entity(x, 1, z, skeleton())

    # A field where nothing came up, with its dry channel.
    for x in range(-19, -14):
        for z in range(9, 19):
            if (x + 19) % 3 == 2:
                b.set(x, 0, z, 'packed_mud')
                b.set(x, 1, z, 'air')
            else:
                b.set(x, 0, z, 'coarse_dirt')
                b.set(x, 1, z, 'dead_bush' if b.chance(0.4) else 'air')

    def near(x, z):
        d = math.hypot(x, z - 3)
        return 0 if d > 22 else min(1.0, (22 - d) * 0.25)

    b.dead_ground(near, 23, ['coarse_dirt', 'packed_mud', 'dirt', 'gravel', 'rooted_dirt'], [50, 20, 15, 5, 10], decor=0.04)
    # Bones where they fell.
    placed = 0
    while placed < 12:
        x, z = b.rng.randint(-16, 16), b.rng.randint(-2, 20)
        if b.get(x, 1, z) == 'air' and b.solid(x, 0, z):
            b.set(x, 1, z, b.rng.choice(['bone_block[axis=x]', 'bone_block[axis=z]', 'skeleton_skull[rotation=6]',
                                         'bone_block[axis=x]']))
            placed += 1
    return b


STRUCTURES = {'burnt_church': burnt_church, 'ruined_statue': ruined_statue, 'aztetian_village': aztetian_village}

# ---------------------------------------------------------------- preview
COLORS = {
    'stone': (125, 125, 125), 'andesite': (136, 136, 137), 'cobblestone': (122, 122, 122), 'tuff': (95, 97, 90),
    'blackstone': (42, 36, 41), 'basalt': (62, 62, 66), 'deepslate': (72, 72, 76), 'nether_brick': (44, 22, 26),
    'red_nether': (70, 8, 10), 'mossy': (106, 120, 90), 'coarse_dirt': (119, 86, 60), 'dirt': (134, 96, 67),
    'gravel': (130, 127, 125), 'mud': (140, 104, 80), 'hay': (166, 138, 38), 'terracotta': (160, 84, 40),
    'gold': (220, 170, 60), 'dark_oak': (60, 40, 20), 'spruce': (100, 76, 48), 'jungle': (150, 110, 78),
    'skull': (200, 200, 190), 'bone': (220, 215, 200), 'redstone': (150, 30, 30), 'chest': (160, 110, 40),
    'barrel': (130, 95, 55), 'dead_bush': (120, 90, 40), 'wither_rose': (30, 30, 20), 'carpet': (90, 20, 20),
    'candle': (190, 60, 60), 'campfire': (220, 120, 40), 'iron': (170, 170, 170), 'chain': (70, 70, 80),
    'obsidian': (60, 20, 100), 'glass': (150, 40, 40), 'bell': (230, 190, 70), 'cobweb': (230, 230, 230),
    'cauldron': (60, 60, 60), 'lectern': (150, 110, 70), 'potted': (120, 70, 50),
}


def color_of(name):
    for key, color in COLORS.items():
        if key in name:
            return color
    return (128, 128, 128)


def preview(b, path, cell=7):
    from PIL import Image, ImageDraw
    blocks = [(p, n) for p, (n, _) in b.blocks.items() if n != 'air']
    lo = [min(p[i] for p, _ in blocks) for i in range(3)]
    hi = [max(p[i] for p, _ in blocks) for i in range(3)]
    sx, sy, sz = (hi[i] - lo[i] + 1 for i in range(3))
    width, height = (sx + sz) * cell + 20, (sx + sz) * cell // 2 + sy * cell + 2 * cell + 20
    img = Image.new('RGB', (width, height), (150, 185, 120))
    draw = ImageDraw.Draw(img)
    # Looking from the south-west: draw far blocks first.
    for (x, y, z), name in sorted(blocks, key=lambda e: (-(e[0][0] + e[0][2]), e[0][1])):
        gx, gy, gz = x - lo[0], y - lo[1], hi[2] - z
        px = 10 + (gx + gz) * cell
        py = 10 + sy * cell + sx * cell // 2 + (gz - gx) * cell // 2 - gy * cell
        base = color_of(name)
        small = any(k in name for k in ('carpet', 'candle', 'dead_bush', 'rose', 'skull', 'bars', 'chain', 'fence', 'wall'))
        inset = cell // 3 if small else 0
        top = [(px + inset, py), (px + cell, py - cell // 2 + inset // 2), (px + 2 * cell - inset, py), (px + cell, py + cell // 2 - inset // 2)]
        draw.polygon(top, fill=tuple(min(255, int(c * 1.15)) for c in base))
        draw.polygon([(px + inset, py), (px + cell, py + cell // 2 - inset // 2), (px + cell, py + cell // 2 + cell),
                      (px + inset, py + cell)], fill=tuple(int(c * 0.8) for c in base))
        draw.polygon([(px + 2 * cell - inset, py), (px + cell, py + cell // 2 - inset // 2), (px + cell, py + cell // 2 + cell),
                      (px + 2 * cell - inset, py + cell)], fill=tuple(int(c * 0.62) for c in base))
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path)


def main():
    for name, make in STRUCTURES.items():
        b = make()
        b.save(name)
        if '--preview' in sys.argv:
            preview(b, os.path.join(ROOT, 'build/preview', f'structure_{name}.png'))


if __name__ == '__main__':
    main()
