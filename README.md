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

## The fight

350 health, 6 armour, immune to fire, knockback, wither and poison. She always knows where players are.

| Action | What it does |
| --- | --- |
| Marionette Sweep | Right arms rake a half circle in front of her (11 damage, knockback). |
| Watching Eyes | Three eyes tear free of the halo and home in (7 damage, Wither II). They can be struck down. |
| Gaze of Oblivion | A long, loud build-up, then everyone the halo can see takes 14 damage with Darkness and Slowness. Break line of sight. |
| Puppet Strings | Threads drag a distant target to her feet. |
| Veil Step | Phase two only: she thins to a thread and reappears behind her target. |

At half health she shrieks, knocks everything back and enters phase two: faster, shorter pauses, a second
sweep from the left arms, six eyes per volley, a stronger gaze, and glowing wounds across her body.

## Working on her

| What | Where |
| --- | --- |
| Behaviour, damage numbers, timings | `src/main/java/com/dentomas/deathgoddess/entity/GoddessOfDeath.java` |
| Keyframe animations | `client/model/GoddessAnimations.java` |
| Procedural motion (eyes, cloth, fingers, hover) | `client/model/GoddessModel.java` |
| Model geometry and textures | `tools/gen_model.py` |
| Sounds | `tools/gen_sounds.py` |

The model is defined once in `tools/gen_model.py`, which writes both `GoddessModelLayer.java` and the texture
atlases so the UVs always match. The sounds are synthesised by `tools/gen_sounds.py`. Both are deterministic.

```
pip install numpy scipy pillow        # ffmpeg is also needed for the sounds
python3 tools/gen_model.py --preview  # also renders build/preview/sheet.png without starting the game
python3 tools/gen_sounds.py
```

To preview a single action in game, stand in creative mode (so she has no target) and run:

```
/data merge entity @e[type=deathgoddess:goddess_of_death,limit=1] {Action:4}
```

`1` spawn, `2` sweep, `3` eyes, `4` gaze, `5` strings, `6` veil step, `7` phase change.

## Building

```
./gradlew build      # jar in build/libs/
./gradlew runClient
```
