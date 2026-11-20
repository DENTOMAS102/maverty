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

She drops an **Eye of Oblivion**, chains and wither roses.

## Death magic

Everything she does follows the same three rules.

- **Players** (and other bosses) take damage. It is magic: armour does not reduce it, Protection and Resistance do.
- **Every other living thing dies at once.** Sheep, cows, villagers, golems, pets and hostile mobs caught in an
  attack are simply taken. So is anything that wanders within a few blocks of her.
- **Vegetation withers.** Grass, podzol, mycelium, moss and farmland turn to bare dirt; plants die back to dead
  bushes, a few flowers to wither roses; leaves fall, leaving bare trunks. This follows the `mobGriefing` game rule.

## The fight

400 health, 6 armour, immune to fire, knockback, wither and poison. She always knows where players are, and
holds at about twelve blocks: she is a caster.

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

## Working on her

| What | Where |
| --- | --- |
| The fight: attack choice, damage, timings | `src/main/java/com/dentomas/deathgoddess/entity/GoddessOfDeath.java` |
| The rules of her magic (damage, instant death, withering) | `entity/DeathMagic.java` |
| Ground sigils and thrown magic | `entity/DeathSigil.java`, `entity/DeathBolt.java` |
| Action list and durations | `entity/GoddessAction.java` |
| Keyframe animations | `client/model/GoddessAnimations.java` |
| Procedural motion (eyes, cloth, fingers, hover) | `client/model/GoddessModel.java` |
| Model geometry and all textures | `tools/gen_model.py` |
| Sounds | `tools/gen_sounds.py` |

An action's animation is exactly as long as the action, and `GoddessAnimations` poses arms absolutely (see the
comment at the top of that file), so new choreography is a few lines per attack.

The model is defined once in `tools/gen_model.py`, which writes both `GoddessModelLayer.java` and the texture
atlases so the UVs always match. The sounds are synthesised by `tools/gen_sounds.py`. Both are deterministic.

```
pip install numpy scipy pillow        # ffmpeg is also needed for the sounds
python3 tools/gen_model.py --preview  # also renders build/preview/sheet.png without starting the game
python3 tools/gen_sounds.py
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
