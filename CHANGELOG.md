# Changelog

All notable changes to Census are documented here. Versioning follows
[SemVer](https://semver.org/).

## [1.0.0] — 2026-06-18

First public release. Every villager (plus tagged animals and hostiles) leads
an inner life that drives emergent stories.

### Added
- **Persona** — Big Five personality, Markov-generated names, derived traits;
  personality is inherited across villager generations.
- **Memory** — an importance-weighted, pruned stream of what happened to a mob.
- **Emotion** — eight OCC emotions and a slow PAD mood; the same event lands
  differently depending on personality.
- **Reflection** — memories distil into lasting insight (grudge, trauma,
  grief, contentment) that colours long-term mood.
- **Reputation & gossip** — opinions of others spread through the village.
- **Society** — bonds, lifelong courtship, and relationship-driven births.
- **Behaviour** — utility AI: mobs flee those they fear/hate and bring gifts to
  those they adore; sworn avengers hunt the killers of their kin.
- **World registry** — family and causality persist across death, distance and
  unloaded chunks (a distant child still avenges a slain parent).
- **Data-driven** — tropes (`census_tropes/`), utility actions
  (`census_actions/`) and censused species (entity-type tags) are all
  datapack-defined and `/reload`-able.
- **UI** — at-a-glance HUD on look; a full profile screen via the Census Book;
  emotion particles; gravestones inscribed for the fallen.
- **Commands** — `/census who|memory|emotion|family|reputation`.
- **Config** — server tunables for the social systems, population, particles,
  gravestones, name tags and reputation size.
- Localisation: English and Japanese.

[1.0.0]: https://github.com/ashina814/census/releases/tag/v1.0.0
