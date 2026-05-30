package dev.kout2.census.registry;

import dev.kout2.census.CensusMod;
import dev.kout2.census.emotion.EmotionalState;
import dev.kout2.census.memory.MemoryStream;
import dev.kout2.census.persona.Persona;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

/**
 * Data attachment registry. Each per-entity layer of Census is one
 * {@link AttachmentType}: Persona here, MemoryStream and EmotionalState in
 * later phases.
 */
public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, CensusMod.MODID);

    /**
     * Persona: persisted (serialize) and synced to clients (for name tags and,
     * later, emotion overlays). Default supplier returns the UNKNOWN placeholder;
     * presence is tested with {@code entity.hasData(PERSONA)} so non-censused
     * mobs are never treated as having a real persona.
     */
    public static final Supplier<AttachmentType<Persona>> PERSONA =
            ATTACHMENT_TYPES.register("persona", () ->
                    AttachmentType.<Persona>builder(() -> Persona.UNKNOWN)
                            .serialize(Persona.MAP_CODEC)
                            .sync(Persona.STREAM_CODEC)
                            .build());

    /**
     * MemoryStream: persisted but NOT synced (server-only). Behaviour and
     * narrative decisions read it server-side; the client never needs it.
     */
    public static final Supplier<AttachmentType<MemoryStream>> MEMORY =
            ATTACHMENT_TYPES.register("memory", () ->
                    AttachmentType.<MemoryStream>builder(MemoryStream::new)
                            .serialize(MemoryStream.MAP_CODEC)
                            .build());

    /**
     * EmotionalState: persisted AND synced (acute emotions + PAD mood). Synced
     * so the client can render facial/particle feedback in Phase 8.
     */
    public static final Supplier<AttachmentType<EmotionalState>> EMOTION =
            ATTACHMENT_TYPES.register("emotion", () ->
                    AttachmentType.<EmotionalState>builder(EmotionalState::new)
                            .serialize(EmotionalState.MAP_CODEC)
                            .sync(EmotionalState.STREAM_CODEC)
                            .build());

    private ModAttachments() {}

    public static void register(IEventBus bus) {
        ATTACHMENT_TYPES.register(bus);
    }
}
