# Census

**Emergent social simulation for Minecraft 1.21.11 (NeoForge)**

Every mob in your world has a name, a personality, memories, and a family.
They remember what you did to them. Their descendants remember what you did
to their grandparents. Stories emerge.

## Status

Phase 0 — scaffolding. The mod loads and registers a placeholder
`Census Book` item. Persona arrives in Phase 1.

Full design: `~/.claude/plans/streamed-tinkering-pizza.md`

## Stack

- Minecraft 1.21.11
- NeoForge 21.11.42
- Java 21
- Parchment mappings (2025.12.20)
- ModDevGradle 2.0.141

## Build

```
./gradlew build
./gradlew runClient
```

## Architecture (target)

```
Persona         entity attachment (synced)        Big Five + identity + reputations
MemoryStream    entity attachment (server-only)   observations + reflections
EmotionalState  entity attachment (synced)        OCC emotions + PAD mood
CensusRegistry  level SavedData                   family graph + history
NarrativeEngine data-driven                       Wildermyth-style tropes
```

## References

- Park et al. 2023 "Generative Agents" (Stanford)
- Ortony, Clore, Collins 1988 (OCC emotion model)
- Costa & McCrae (Big Five / NEO-PI)
- Dias & Paiva (FAtiMA appraisal architecture)
- Wildermyth (Worldwalker Games) — trope binding
- Crusader Kings — dynasty simulation
- Dwarf Fortress / RimWorld — emergent narrative

## License

MIT
