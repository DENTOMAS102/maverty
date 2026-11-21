# Goddess of Death

A boss mod for **Minecraft Forge 1.19.2** (Forge 43.x, Java 17). No other mods are required: the model and
animations use the vanilla keyframe system, the same one the Warden uses.

She is a veiled, six-armed marionette almost four blocks tall, hung from threads that fade into the dark above
her, with a halo of independently moving eyes behind her head.

## Summoning

- **Mourning Effigy**: craft it and use it on the ground.

  ```
  C   C      C = Chain             R = Wither Rose
  R S R      S = Wither Skeleton Skull
  E O E      E = Eye of Ender      O = Crying Obsidian
  ```

- Creative: the **Goddess of Death Spawn Egg**, or `/summon deathgoddess:goddess_of_death`.

She drops an **Eye of Oblivion**, chains, wither roses and a tome.

## Death magic

Everything she does follows the same three rules.

- **Players** (and other bosses) take damage. It is magic: armour does not reduce it, Protection and Resistance do.
- **Every other living thing dies at once.** Sheep, cows, villagers, golems, pets and hostile mobs caught in an
  attack are simply taken. So is anything that wanders within a few blocks of her.
- **Vegetation withers.** Grass, podzol, mycelium, moss and farmland turn to bare dirt; plants die back to dead
  bushes, a few flowers to wither roses; leaves fall, leaving bare trunks. This follows the `mobGriefing` game rule.

## The fight

She is tuned to be harder than the Ender Dragon, the final boss: **600 health** (the dragon has 200), 12 armour,
no single hit can take more than 22 from her, and every attack below does 1.5 times the listed damage. All of
that is in the config (see below). She is immune to fire, knockback, wither, poison, drowning and suffocation,
and slowly mends if she is left without a target. She always knows where players are, and holds at about twelve
blocks: she is a caster.

Every attack gives **one to two seconds of warning**. Ground magic is drawn first as a sigil that shows exactly
where it will land and fills until it strikes; thrown magic is slow, or hangs in the air before it flies. She
never repeats any of her last four attacks, so there is no rotation to learn, only tells.

| Attack | Tell | What it does |
| --- | --- | --- |
| Grave Bloom | Seals under and around you (1.4 s) | Pillars erupt from a ring of graves. 10 damage. |
| Severance | A long strip from her to you (1.3 s) | A blade wave tears along it, then a second one, re-aimed. 12 damage. |
| Death Knell | Rings spreading out from her (1.2 s each) | Each toll strikes the next ring outward; step into the one that has just gone off. 10 damage. |
| Black Rain | Many small seals, some leading you (1.2 s) | Death falls from the sky for two seconds. 7 damage. |
| Cross of Oblivion | Two crossing strips through you (1.4 s) | Both erupt along their whole length. 11 damage. |
| Marked for Death | An eye sigil that follows you (2 s) | It tracks for a second, then locks where you stand for the last one. 12 damage. |
| Crescent Volley | Arms sweep wide (1 s) | A fan of crescents skims the ground and cuts through everything. 9 damage. |
| Soul Lances | Lances form above her and turn to aim (1.3 s) | They fly one after another at where you were. Keep moving. 7 damage each. |
| Watching Eyes | Eyes hang in the air (0.7 s) | They home in. 7 damage and Wither II. They can be struck down. |
| Gaze of Oblivion | She rises, arms spread (2 s) | Everyone the halo can see takes 14 damage with Darkness and Slowness. Break line of sight. |
| Puppet Strings | Threads reach for you (1 s) | You are dragged to her feet. |
| Marionette Sweep | She glides in, turning (1 s) | Close range only. Six arms rake a half circle. 11 damage. |
| Veil Step | She thins to a thread | No damage. Her answer to a target that runs or hides: she reappears nine blocks behind you. |

At half health she shrieks, withers everything around her and enters **phase two**: shorter pauses, 20% more
damage, more of everything (fans of five blades, two crosses, eight lances), and three more attacks.

| Attack | Tell | What it does |
| --- | --- | --- |
| Lattice | Parallel walls with gaps as wide as the walls (1.4 s) | The walls erupt, then the gaps do. Stand in a gap, then step into a spent wall. 10 damage. |
| Danse Macabre | A music box; she begins to turn (1 s) | Two full turns scattering slow spirals of orbs. 6 damage each. |
| Requiem | A seal covering the whole arena, with two pale refuges (2 s) | Everything outside a refuge takes 22 damage. One refuge is always a short run from her target. |

## The eye that watches

Once players have killed **three different Lords** in a world, the Goddess takes notice. From then on, rarely
and without warning, by day or by night, a great eye opens in the air above one player and watches them for
about seven seconds. While it does, that player's view closes in from every side, narrows and shakes, and they
hear their own heartbeat. Then it closes. It does no damage.

- It only picks a player who is not expecting it: alive, not in creative, at more than half health, and with no
  fighting for the last 45 seconds, no mob hunting them and no Goddess nearby. If nobody qualifies it waits for a
  quiet moment rather than skipping the visit.
- Visits are 25 to 70 minutes apart; the first one after the third Lord falls comes sooner.
- **Lords** are the entity types in the tag `deathgoddess:lords`. It lists the 26 bosses the Wild Blood quest book
  marks as Lords, plus the Wither. Mods that are not installed are skipped; add or remove entries with a datapack.

`/deathgoddess lords` shows how many have fallen, `/deathgoddess lords set <n>` sets it for testing, and
`/deathgoddess watch [players]` opens the eye at once (operators only).

## Ruins

Three structures generate in the Overworld. The ground around each one is dead.

| Structure | Where | What is there |
| --- | --- | --- |
| Burnt-out church | Forests, taiga, plains | Sooted walls, a broken bell tower, charred rafters, a graveyard with one open grave. The chest holds *The Last Sermon*. |
| Ruined statue | Hills, mountains, badlands, taiga, plains, swamps | The Goddess in stone, exactly 19 blocks tall: veiled, crowned, with a halo of eyes and six arms, two of them broken off. The chest in the plinth holds *The Stonecutter's Account*. |
| Abandoned Aztetian village | Jungles, savannas, badlands | A stepped temple with a shrine on top, mud-brick huts, skulls on poles, a field where nothing grew, and its people: seven skeletons in golden helmets. The temple chest holds *The Emptied Village*. |

`/locate structure deathgoddess:burnt_church` (or `ruined_statue`, `aztetian_village`) finds the nearest one.

## Tomes

Ten readable books about her: prayers, a sermon, treatises on the death of gods and on false gods, and the
accounts of the people who built the ruins. They are written in English and Russian and follow the game language.
Each ruin holds its own volume, and any generated loot chest in the world, vanilla or modded, has a 5% chance of
holding a random one. The texts are in `tools/tomes.json`.

## Achievements

- **Vanilla advancements:** once the Ender Dragon is dead, an eye appears after it in the End tab, titled `???`
  with an unreadable description. Killing the Goddess completes it.
- **Wild Blood quest book (FTB Quests):** `python3 modpack/install_quest.py <game folder>` adds her to the
  "Угроза Лордов" chapter as a lone eye at the top of the tree. Title, subtitle and description are struck out;
  the quest completes when she is killed. The script keeps a backup and can be run twice safely.

## Config

`config/deathgoddess-common.toml` is created on first launch.

| Setting | Default | Meaning |
| --- | --- | --- |
| `goddess.maxHealth` | 600 | Her health. |
| `goddess.armor`, `goddess.armorToughness` | 12, 6 | Her armour. |
| `goddess.damageMultiplier` | 1.5 | Multiplies the damage of all her attacks. |
| `goddess.damageCap` | 22 | Most damage one hit can do to her. 0 switches the cap off. |
| `watchingEye.enabled` | true | Switch the eye off entirely. |
| `watchingEye.lordsRequired` | 3 | Different Lords that must have been killed first. |
| `watchingEye.minMinutes`, `maxMinutes` | 25, 70 | Time between visits. |
| `watchingEye.calmSeconds` | 45 | How long a player must have been out of combat. |
| `tomes.chanceInChests` | 0.05 | Chance of a tome in any generated chest. |

## Working on her

| What | Where |
| --- | --- |
| The fight: attack choice, damage, timings | `src/main/java/com/dentomas/deathgoddess/entity/GoddessOfDeath.java` |
| The rules of her magic (damage, instant death, withering) | `entity/DeathMagic.java` |
| Ground sigils and thrown magic | `entity/DeathSigil.java`, `entity/DeathBolt.java` |
| Action list and durations | `entity/GoddessAction.java` |
| Keyframe animations | `client/model/GoddessAnimations.java` |
| Procedural motion (eyes, cloth, fingers, hover) | `client/model/GoddessModel.java` |
| The watching eye: counting Lords, choosing the moment | `world/WatchDirector.java`, `entity/OmenEye.java` |
| What the watched player sees | `client/WatchedEffects.java` |
| Model geometry and all textures | `tools/gen_model.py` |
| Sounds | `tools/gen_sounds.py` |
| The three ruins, block by block | `tools/gen_structures.py` |
| Language files, loot, tags, worldgen, advancement | `tools/gen_resources.py` |

An action's animation is exactly as long as the action, and `GoddessAnimations` poses arms absolutely (see the
comment at the top of that file), so new choreography is a few lines per attack.

The model is defined once in `tools/gen_model.py`, which writes both `GoddessModelLayer.java` and the texture
atlases so the UVs always match. The sounds are synthesised by `tools/gen_sounds.py`. Both are deterministic.

```
pip install numpy scipy pillow        # ffmpeg is also needed for the sounds
python3 tools/gen_model.py --preview  # also renders build/preview/sheet.png without starting the game
python3 tools/gen_sounds.py
python3 tools/gen_structures.py --preview
python3 tools/gen_resources.py
```

To preview a single action in game, stand in creative mode (she then has no target and aims straight ahead)
and run:

```
/data merge entity @e[type=deathgoddess:goddess_of_death,limit=1] {Action:"requiem"}
```

The names are `sweep`, `eyes`, `gaze`, `strings`, `veil_step`, `grave_bloom`, `severance`, `death_knell`,
`black_rain`, `cross`, `mark`, `lattice`, `crescents`, `soul_lances`, `danse_macabre`, `requiem`, `spawn` and
`phase`. `/data get entity ... CurrentAction` shows what she is doing.

## Building

```
./gradlew build      # jar in build/libs/
./gradlew runClient
```
