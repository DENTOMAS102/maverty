#!/usr/bin/env python3
"""Writes every JSON resource of the mod: language files (English and Russian), sounds.json, item
models, loot tables, tags, worldgen, advancements and loot modifiers.

    python3 tools/gen_resources.py

The tome texts come from tools/tomes.json. Output is deterministic.
"""
import json
import os
import re

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, 'src/main/resources')
A = f'{RES}/assets/deathgoddess'
D = f'{RES}/data/deathgoddess'
MOD = 'deathgoddess'


def write(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8') as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write('\n')


# ---------------------------------------------------------------- sounds
FAR = {'spawn', 'death', 'phase', 'gaze_charge', 'gaze_release', 'knell', 'danse', 'requiem_charge', 'requiem_release',
       'cast_sigil', 'cast_blade', 'sigil_erupt'}
VARIANTS = {'ambient': 3, 'hurt': 2, 'sigil_erupt': 2}
# name: (English subtitle, Russian subtitle)
SOUNDS = {
    'ambient': ('Goddess of Death whispers', 'Богиня Смерти шепчет'),
    'hurt': ('Porcelain cracks', 'Фарфор трескается'),
    'death': ('Goddess of Death comes apart', 'Богиня Смерти распадается'),
    'spawn': ('A bell tolls', 'Бьёт колокол'),
    'glide': ('Fabric drags', 'Шуршит ткань'),
    'sweep': ('Six arms rake', 'Взмах шести рук'),
    'eye_cast': ('An eye tears free', 'Глаз отрывается'),
    'eye_hit': ('An eye bursts', 'Глаз лопается'),
    'gaze_charge': ('The halo opens', 'Ореол раскрывается'),
    'gaze_release': ('The halo sees you', 'Ореол видит тебя'),
    'strings': ('Threads pull taut', 'Нити натягиваются'),
    'veil_step': ('Goddess of Death vanishes', 'Богиня Смерти исчезает'),
    'phase': ('Goddess of Death shrieks', 'Богиня Смерти кричит'),
    'cast_sigil': ('Goddess of Death intones', 'Звучит заклинание'),
    'cast_blade': ('A blade is drawn', 'Клинок обнажён'),
    'sigil_erupt': ('A sigil erupts', 'Печать извергается'),
    'blade_wave': ('The air is cut', 'Воздух рассечён'),
    'knell': ('A knell tolls', 'Погребальный звон'),
    'rain_fall': ('Black rain falls', 'Падает чёрный дождь'),
    'lance_form': ('A lance takes shape', 'Копьё обретает форму'),
    'lance_fire': ('A lance flies', 'Копьё летит'),
    'danse': ('A music box plays', 'Играет музыкальная шкатулка'),
    'requiem_charge': ('A requiem swells', 'Реквием нарастает'),
    'requiem_release': ('The requiem ends', 'Реквием окончен'),
    'reap': ('A life is taken', 'Жизнь отнята'),
    'wither': ('Leaves wither', 'Листва увядает'),
    'watched': ('Something is watching', 'Что-то смотрит'),
}

sounds = {}
for name in SOUNDS:
    files = [f'{name}{i + 1}' for i in range(VARIANTS[name])] if name in VARIANTS else [name]
    entries = []
    for file in files:
        entry = {'name': f'{MOD}:goddess/{file}'}
        if name in FAR:
            entry['attenuation_distance'] = 48
        entries.append(entry if len(entry) > 1 else entry['name'])
    sounds[f'entity.goddess_of_death.{name}'] = {'subtitle': f'subtitles.{MOD}.goddess_of_death.{name}', 'sounds': entries}
write(f'{A}/sounds.json', sounds)

# ---------------------------------------------------------------- language
# key: (English, Russian)
TEXT = {
    'entity.deathgoddess.goddess_of_death': ('Goddess of Death', 'Богиня Смерти'),
    'entity.deathgoddess.watching_eye': ('Watching Eye', 'Следящий глаз'),
    'entity.deathgoddess.death_sigil': ('Death Sigil', 'Печать смерти'),
    'entity.deathgoddess.death_crescent': ('Death Crescent', 'Полумесяц смерти'),
    'entity.deathgoddess.death_orb': ('Death Orb', 'Сфера смерти'),
    'entity.deathgoddess.soul_lance': ('Soul Lance', 'Копьё душ'),
    'entity.deathgoddess.omen_eye': ('The Eye', 'Око'),
    'death.attack.deathgoddess.death_magic': ('%1$s was unmade by %2$s', '%1$s погиб от магии смерти: %2$s'),
    'death.attack.deathgoddess.death_magic.item': ('%1$s was unmade by %2$s using %3$s',
                                                  '%1$s погиб от магии смерти: %2$s, %3$s'),
    'item.deathgoddess.goddess_of_death_spawn_egg': ('Goddess of Death Spawn Egg', 'Яйцо призыва Богини Смерти'),
    'item.deathgoddess.mourning_effigy': ('Mourning Effigy', 'Траурная кукла'),
    'item.deathgoddess.mourning_effigy.tooltip': ('Set it down, and she will come.', 'Положи её, и она придёт.'),
    'item.deathgoddess.mourning_effigy.peaceful': ('She does not answer in a peaceful world.',
                                                  'В мирном мире она не отзывается.'),
    'item.deathgoddess.mourning_effigy.no_room': ('There is not enough room for her here.', 'Здесь для неё слишком тесно.'),
    'item.deathgoddess.eye_of_oblivion': ('Eye of Oblivion', 'Око Забвения'),
    'item.deathgoddess.tome': ('Tome', 'Том'),
    'commands.deathgoddess.watch': ('The eye opens above %s player(s)', 'Око открывается над игроками: %s'),
    'commands.deathgoddess.lords': ('Lords slain: %s of the %s she waits for. %s', 'Убито Лордов: %s из %s. %s'),
    'advancements.deathgoddess.goddess.title': ('???', '???'),
    'advancements.deathgoddess.goddess.description': ('She was watching all along.', 'Она смотрела всё это время.'),
}
for name, (en, ru) in SOUNDS.items():
    TEXT[f'subtitles.{MOD}.goddess_of_death.{name}'] = (en, ru)

tomes = json.load(open(os.path.join(ROOT, 'tools/tomes.json'), encoding='utf-8'))
tome_ids = [t['id'] for t in tomes]
java = open(os.path.join(ROOT, 'src/main/java/com/dentomas/deathgoddess/item/TomeItem.java')).read()
java_ids = re.findall(r'"([a-z_]+)"', java[java.index('TOMES = List.of('):java.index(');', java.index('TOMES = List.of('))])
assert java_ids == tome_ids, f'TomeItem.TOMES {java_ids} does not match tools/tomes.json {tome_ids}'

# The book screen shows 14 lines of 114 pixels. Estimate glyph widths and refuse pages that would overflow.
NARROW = {**dict.fromkeys("i.,:;!'|", 2), 'l': 3, '`': 3, **dict.fromkeys('tI[] "()*', 4), **dict.fromkeys('fk<>', 5)}


def page_lines(page):
    total = 0
    for paragraph in page.split('\n'):
        lines, used = 1, 0
        for word in paragraph.split(' '):
            width = sum(NARROW.get(ch, 6) for ch in word)
            if used and used + 4 + width > 114:
                lines, used = lines + 1, width
            else:
                used += (4 if used else 0) + width
        total += lines
    return total


for tome in tomes:
    for code in ('en', 'ru'):
        for number, page in enumerate(tome[code]['pages'], start=1):
            assert page_lines(page) <= 14, f'{tome["id"]} {code} page {number} needs {page_lines(page)} lines (14 fit)'
    assert len(tome['en']['pages']) == len(tome['ru']['pages']), tome['id']

lang = {'en': {k: v[0] for k, v in TEXT.items()}, 'ru': {k: v[1] for k, v in TEXT.items()}}
for tome in tomes:
    for code in ('en', 'ru'):
        book = tome[code]
        prefix = f'tome.{MOD}.{tome["id"]}'
        lang[code][f'{prefix}.title'] = book['title']
        lang[code][f'{prefix}.author'] = book['author']
        for i, page in enumerate(book['pages'], start=1):
            lang[code][f'{prefix}.page.{i}'] = page
write(f'{A}/lang/en_us.json', lang['en'])
write(f'{A}/lang/ru_ru.json', lang['ru'])

# ---------------------------------------------------------------- item models
write(f'{A}/models/item/goddess_of_death_spawn_egg.json', {'parent': 'minecraft:item/template_spawn_egg'})
for item in ('mourning_effigy', 'eye_of_oblivion', 'tome'):
    write(f'{A}/models/item/{item}.json',
          {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'{MOD}:item/{item}'}})


# ---------------------------------------------------------------- loot
def item(name, weight=1, count=None):
    entry = {'type': 'minecraft:item', 'name': name if ':' in name else 'minecraft:' + name, 'weight': weight}
    if count:
        entry['functions'] = [{'function': 'minecraft:set_count',
                               'count': {'type': 'minecraft:uniform', 'min': count[0], 'max': count[1]}}]
    return entry


def tome(tome_id, weight=1):
    assert tome_id in tome_ids, tome_id
    return {'type': 'minecraft:item', 'name': f'{MOD}:tome', 'weight': weight,
            'functions': [{'function': 'minecraft:set_nbt', 'tag': '{Tome:"%s"}' % tome_id}]}


def pool(entries, rolls=1, chance=None):
    result = {'rolls': rolls if isinstance(rolls, int) else {'type': 'minecraft:uniform', 'min': rolls[0], 'max': rolls[1]},
              'entries': entries}
    if chance is not None:
        result['conditions'] = [{'condition': 'minecraft:random_chance', 'chance': chance}]
    return result


def chest_table(name, pools):
    write(f'{D}/loot_tables/chests/{name}.json', {'type': 'minecraft:chest', 'pools': pools})


write(f'{D}/loot_tables/entities/goddess_of_death.json', {
    'type': 'minecraft:entity',
    'pools': [pool([item(f'{MOD}:eye_of_oblivion')]), pool([item('chain', count=(4, 9))]),
              pool([item('wither_rose', count=(2, 5))]), pool([tome('puppet_and_puppeteer')])],
})
chest_table('burnt_church', [
    pool([tome('the_last_sermon')]),
    pool([item('coal', 10, (1, 4)), item('charcoal', 10, (1, 4)), item('bone', 8, (1, 3)), item('candle', 6, (1, 2)),
          item('iron_nugget', 6, (2, 6)), item('gold_nugget', 5, (1, 5)), item('book', 5), item('paper', 5, (1, 3)),
          item('chain', 4, (1, 2)), item('emerald', 3, (1, 2)), item('golden_apple', 1)], rolls=(3, 5)),
    pool([tome('prayer_of_the_veil'), tome('of_false_gods'), tome('on_the_death_of_a_god')], chance=0.3),
])
chest_table('ruined_statue', [
    pool([tome('the_stonecutters_account')]),
    pool([item('chain', 6, (1, 3)), item('iron_ingot', 6, (1, 3)), item('redstone', 5, (2, 6)), item('candle', 5, (1, 2)),
          item('bone', 5, (1, 3)), item('emerald', 4, (1, 3)), item('iron_pickaxe', 3), item('amethyst_shard', 3, (1, 3)),
          item('diamond', 1)], rolls=(3, 5)),
    pool([tome('litany_of_six_hands'), tome('puppet_and_puppeteer'), tome('she_counts_the_lords')], chance=0.3),
])
chest_table('aztetian_temple', [
    pool([tome('the_emptied_village')]),
    pool([item('gold_ingot', 8, (1, 4)), item('gold_nugget', 8, (3, 9)), item('bone', 8, (2, 5)), item('raw_gold', 6, (1, 3)),
          item('cocoa_beans', 6, (2, 6)), item('arrow', 6, (3, 8)), item('emerald', 4, (1, 3)), item('melon_seeds', 3, (1, 3)),
          item('bamboo', 3, (1, 4)), item('golden_apple', 2), item('diamond', 1)], rolls=(4, 6)),
    pool([tome('prayer_of_the_veil'), tome('prayer_against_being_seen')], chance=0.35),
])
chest_table('aztetian_hut', [
    pool([item('bone', 8, (1, 3)), item('string', 6, (1, 3)), item('cocoa_beans', 6, (1, 4)), item('bowl', 5),
          item('wheat_seeds', 5, (1, 4)), item('stick', 5, (1, 3)), item('arrow', 4, (1, 5)), item('gold_nugget', 3, (1, 4))],
         rolls=(2, 4)),
    pool([tome('prayer_against_being_seen'), tome('prayer_of_the_veil')], chance=0.12),
])

# Tomes turn up in any generated chest; the chance is in the config.
write(f'{D}/loot_modifiers/tomes_in_chests.json', {'type': f'{MOD}:tomes_in_chests', 'conditions': []})
write(f'{RES}/data/forge/loot_modifiers/global_loot_modifiers.json',
      {'replace': False, 'entries': [f'{MOD}:tomes_in_chests']})

write(f'{D}/recipes/mourning_effigy.json', {
    'type': 'minecraft:crafting_shaped',
    'pattern': ['C C', 'RSR', 'EOE'],
    'key': {
        'C': {'item': 'minecraft:chain'},
        'R': {'item': 'minecraft:wither_rose'},
        'S': {'item': 'minecraft:wither_skeleton_skull'},
        'E': {'item': 'minecraft:ender_eye'},
        'O': {'item': 'minecraft:crying_obsidian'},
    },
    'result': {'item': f'{MOD}:mourning_effigy'},
})

# ---------------------------------------------------------------- the Lords
# Every boss the Wild Blood quest book marks as a Lord. Entries from mods that are not installed are skipped.
LORDS = ['minecraft:ender_dragon', 'minecraft:warden', 'whisperwoods:hirschgeist', 'va:patron_of_animals', 'va:basilisk',
         'va:nightmarish_observer', 'graveyard:lich', 'cataclysm:netherite_monstrosity', 'cataclysm:the_harbinger',
         'cataclysm:the_leviathan', 'cataclysm:ender_guardian', 'cataclysm:ignis', 'cataclysm:ancient_remnant',
         'iter_rpg:sorrowsealed', 'aquamirae:captain_cornelia', 'bosses_of_mass_destruction:lich',
         'bosses_of_mass_destruction:void_blossom', 'bosses_of_mass_destruction:gauntlet',
         'bosses_of_mass_destruction:obsidilith', 'fungal_frenzy:etherfang', 'fungal_frenzy:shroomster',
         'fungal_frenzy:crimson_mycophant', 'fungal_frenzy:cordyceps', 'irons_spellbooks:dead_king',
         'eeeabsmobs:nameless_guardian', 'divine_eden:apostle_eden', 'minecraft:wither']
write(f'{D}/tags/entity_types/lords.json', {
    'replace': False,
    'values': [lord if lord.startswith('minecraft:') else {'id': lord, 'required': False} for lord in LORDS],
})
# She is a boss to other mods as well.
write(f'{RES}/data/forge/tags/entity_types/bosses.json', {'replace': False, 'values': [f'{MOD}:goddess_of_death']})

# ---------------------------------------------------------------- worldgen
FOUNDATION = 2      # must match tools/gen_structures.py
optional = lambda tag: {'id': tag, 'required': False}
# name: (biomes, spacing and separation in chunks, salt)
STRUCTURES = {
    'burnt_church': (['#minecraft:is_forest', '#minecraft:is_taiga', 'minecraft:plains', 'minecraft:meadow',
                      optional('#forge:is_plains')], 40, 16, 1318420711),
    'ruined_statue': (['#minecraft:is_hill', '#minecraft:is_mountain', '#minecraft:is_badlands', '#minecraft:is_taiga',
                       'minecraft:plains', 'minecraft:sunflower_plains', 'minecraft:swamp', optional('#forge:is_plains'),
                       optional('#forge:is_swamp')], 52, 22, 1318420712),
    'aztetian_village': (['#minecraft:is_jungle', '#minecraft:is_savanna', '#minecraft:is_badlands'], 44, 18, 1318420713),
}
for name, (biomes, spacing, separation, salt) in STRUCTURES.items():
    write(f'{D}/tags/worldgen/biome/has_structure/{name}.json', {'replace': False, 'values': biomes})
    write(f'{D}/worldgen/template_pool/{name}.json', {
        'name': f'{MOD}:{name}',
        'fallback': 'minecraft:empty',
        'elements': [{'weight': 1, 'element': {'element_type': 'minecraft:single_pool_element', 'location': f'{MOD}:{name}',
                                               'projection': 'rigid', 'processors': 'minecraft:empty'}}],
    })
    write(f'{D}/worldgen/structure/{name}.json', {
        'type': 'minecraft:jigsaw',
        'biomes': f'#{MOD}:has_structure/{name}',
        'step': 'surface_structures',
        'spawn_overrides': {},
        'terrain_adaptation': 'beard_thin',
        'start_pool': f'{MOD}:{name}',
        'size': 1,
        'start_height': {'absolute': -FOUNDATION},
        'project_start_to_heightmap': 'WORLD_SURFACE_WG',
        'max_distance_from_center': 80,
        'use_expansion_hack': False,
    })
    write(f'{D}/worldgen/structure_set/{name}.json', {
        'structures': [{'structure': f'{MOD}:{name}', 'weight': 1}],
        'placement': {'type': 'minecraft:random_spread', 'spacing': spacing, 'separation': separation, 'salt': salt},
    })

# ---------------------------------------------------------------- advancement
# Shown in the End tab once the dragon is dead: an eye, and nothing else to go on.
write(f'{D}/advancements/goddess_of_death.json', {
    'parent': 'minecraft:end/kill_dragon',
    'display': {
        'icon': {'item': f'{MOD}:eye_of_oblivion'},
        'title': {'translate': 'advancements.deathgoddess.goddess.title', 'color': 'dark_red'},
        'description': {'translate': 'advancements.deathgoddess.goddess.description', 'obfuscated': True},
        'frame': 'challenge',
        'show_toast': True,
        'announce_to_chat': False,
        'hidden': False,
    },
    'criteria': {
        'killed': {'trigger': 'minecraft:player_killed_entity',
                   'conditions': {'entity': [{'condition': 'minecraft:entity_properties', 'entity': 'this',
                                              'predicate': {'type': f'{MOD}:goddess_of_death'}}]}},
    },
    'rewards': {'experience': 500},
})
print(f'resources written: {len(lang["en"])} language keys, {len(tomes)} tomes, {len(LORDS)} lords')
