package dev.kout2.census.census;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.kout2.census.lineage.Lineage;
import dev.kout2.census.persona.Persona;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * World-level memory of every persona that has ever lived — anchored to the
 * overworld's data storage so it spans dimensions and survives the death and
 * unload of the entities themselves.
 *
 * Its first job is reach: a parent's death can be felt by a child on the far
 * side of the world (or in an unloaded chunk) because the family tree lives
 * here, not on the entities. Grief for absent children is queued and delivered
 * when they are next processed. Later milestones grow this into a full family
 * graph + history log.
 */
public class CensusRegistry extends SavedData {
    private static final Codec<UUID> UUID_STRING =
            Codec.STRING.xmap(UUID::fromString, UUID::toString);

    /** A persona's permanent record, independent of its (mortal, unloadable) entity. */
    public record PersonaRecord(UUID id, String name, int generation,
                                Optional<UUID> parentA, Optional<UUID> parentB,
                                boolean alive, long deathDay) {
        public static final Codec<PersonaRecord> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                UUID_STRING.fieldOf("id").forGetter(PersonaRecord::id),
                Codec.STRING.fieldOf("name").forGetter(PersonaRecord::name),
                Codec.INT.fieldOf("generation").forGetter(PersonaRecord::generation),
                UUID_STRING.optionalFieldOf("parentA").forGetter(PersonaRecord::parentA),
                UUID_STRING.optionalFieldOf("parentB").forGetter(PersonaRecord::parentB),
                Codec.BOOL.fieldOf("alive").forGetter(PersonaRecord::alive),
                Codec.LONG.fieldOf("deathDay").forGetter(PersonaRecord::deathDay)
        ).apply(inst, PersonaRecord::new));
    }

    /** A blamed killing waiting to be delivered to an absent grieving relative. */
    public record PendingGrief(Optional<UUID> killer, long tick) {
        public static final Codec<PendingGrief> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                UUID_STRING.optionalFieldOf("killer").forGetter(PendingGrief::killer),
                Codec.LONG.fieldOf("tick").forGetter(PendingGrief::tick)
        ).apply(inst, PendingGrief::new));
    }

    public static final Codec<CensusRegistry> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            PersonaRecord.CODEC.listOf().optionalFieldOf("records", List.of())
                    .forGetter(r -> List.copyOf(r.records.values())),
            Codec.unboundedMap(UUID_STRING, PendingGrief.CODEC.listOf()).optionalFieldOf("pendingGrief", Map.of())
                    .forGetter(r -> r.pendingGrief)
    ).apply(inst, CensusRegistry::new));

    public static final SavedDataType<CensusRegistry> TYPE =
            new SavedDataType<>("census_registry", CensusRegistry::new, CODEC);

    private final Map<UUID, PersonaRecord> records = new HashMap<>();
    private final Map<UUID, List<PendingGrief>> pendingGrief = new HashMap<>();
    /** parent persona id -> child persona ids; derived from records, not serialized. */
    private final Map<UUID, Set<UUID>> childIndex = new HashMap<>();

    public CensusRegistry() {}

    private CensusRegistry(List<PersonaRecord> recordList, Map<UUID, List<PendingGrief>> pending) {
        for (PersonaRecord r : recordList) {
            records.put(r.id(), r);
        }
        pending.forEach((k, v) -> pendingGrief.put(k, new ArrayList<>(v)));
        rebuildChildIndex();
    }

    private void rebuildChildIndex() {
        childIndex.clear();
        for (PersonaRecord r : records.values()) {
            r.parentA().ifPresent(p -> childIndex.computeIfAbsent(p, k -> new HashSet<>()).add(r.id()));
            r.parentB().ifPresent(p -> childIndex.computeIfAbsent(p, k -> new HashSet<>()).add(r.id()));
        }
    }

    /** The overworld-anchored registry for this server. */
    public static CensusRegistry get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    /** Record (or refresh) a persona and its parent edges. */
    public void register(Persona persona, Lineage lineage) {
        records.put(persona.id(), new PersonaRecord(persona.id(), persona.fullName(),
                lineage.generation(), lineage.parentA(), lineage.parentB(), true, 0L));
        lineage.parentA().ifPresent(p -> childIndex.computeIfAbsent(p, k -> new HashSet<>()).add(persona.id()));
        lineage.parentB().ifPresent(p -> childIndex.computeIfAbsent(p, k -> new HashSet<>()).add(persona.id()));
        setDirty();
    }

    public void recordDeath(UUID personaId, long deathDay) {
        PersonaRecord r = records.get(personaId);
        if (r != null && r.alive()) {
            records.put(personaId, new PersonaRecord(r.id(), r.name(), r.generation(),
                    r.parentA(), r.parentB(), false, deathDay));
            setDirty();
        }
    }

    public Set<UUID> childrenOf(UUID personaId) {
        return childIndex.getOrDefault(personaId, Set.of());
    }

    public void enqueueGrief(UUID childPersonaId, Optional<UUID> killer, long tick) {
        pendingGrief.computeIfAbsent(childPersonaId, k -> new ArrayList<>())
                .add(new PendingGrief(killer, tick));
        setDirty();
    }

    /** Removes and returns any grief queued for this persona (empty if none). */
    public List<PendingGrief> drainGrief(UUID personaId) {
        List<PendingGrief> grief = pendingGrief.remove(personaId);
        if (grief == null) {
            return List.of();
        }
        setDirty();
        return grief;
    }

    public boolean hasPendingGrief() {
        return !pendingGrief.isEmpty();
    }
}
