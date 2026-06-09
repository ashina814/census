# Census

**An emergent social simulation for Minecraft 1.21.11 (NeoForge).**

Every villager in your world is a person. They have a name, a personality, a
family, and a memory. They remember what you did to them — and so do their
friends, and their children. Kill someone's father and a vengeful child may
hunt you down from the other side of the map. Stories aren't scripted here;
they *emerge* from a few interacting systems.

> Status: in active development toward a 1.0 release on CurseForge. The core
> simulation works end-to-end; see the roadmap below.

---

## What's in it

Each censused mob carries layered inner state, all persisted and (where it
matters) synced to the client:

- **Persona** — a Big Five (OCEAN) personality, a Markov-generated name, and
  derived traits (*brave*, *vengeful*, *kind* …). Personality is inherited.
- **Memory** — an importance-weighted stream of what happened to it (fed,
  harmed, witnessed a death …), pruned over time.
- **Emotion** — eight OCC emotions plus a slow PAD mood. The *same* event
  angers one villager and merely frightens another, depending on personality.
- **Reflection** — memories distil into lasting insight (*holds a grudge*,
  *deeply scarred*, *in mourning*) that colours long-term mood.
- **Reputation & gossip** — opinions of others that spread through the village,
  so a wrong you did to one villager becomes common knowledge.
- **Society** — villagers bond, court lifelong partners, and raise children;
  reproduction is relationship-driven, not bread-and-beds.
- **Behaviour** — a utility AI turns inner state into action: a mob flees a
  player it has come to fear or hate.
- **Narrative** — authored × procedural "tropes" fire on emergent state. The
  flagship: *the Avenger*.
- **World memory** — a world-level registry remembers every persona and family
  line, so causality reaches across distance, death and unloaded chunks.

Inspect any of it in-game: look at a mob for an at-a-glance HUD, right-click it
with the **Census Book** for a full card, or use
`/census who|memory|emotion|family|reputation`.

## How it's built

Grounded in published work on believable agents, adapted to run with no LLM and
a tight tick budget:

- Big Five (Costa & McCrae) personality
- OCC appraisal + PAD mood (FAtiMA lineage)
- Memory Stream + Reflection (Park et al., *Generative Agents*, 2023)
- Wildermyth-style authored-×-procedural tropes
- Utility AI for action selection

A standalone Monte-Carlo model (`scripts/sim.py`) mirrors the in-game constants
to predict balance (trait rarity, how many hits provoke a flee, population
growth) without running the game for hours.

## Roadmap

Done: config foundation · performance scheduler · world registry + distant
revenge · reflection.
Next: data-driven tropes & names (datapack-extensible) · richer utility AI ·
personas for all mobs · in-game GUI · full config & tests · CurseForge 1.0.

## Build & run

Requires JDK 21.

```bash
./gradlew runClient    # launch a dev client
./gradlew build        # produce the mod jar in build/libs/
```

See [`docs/TESTING.md`](docs/TESTING.md) for the in-world verification steps.

## Stack

Minecraft 1.21.11 · NeoForge 21.11.42 · Java 21 · Parchment mappings.

## License

MIT — see [`LICENSE`](LICENSE).
