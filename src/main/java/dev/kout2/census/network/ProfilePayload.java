package dev.kout2.census.network;

import dev.kout2.census.CensusMod;
import dev.kout2.census.emotion.EmotionalState;
import dev.kout2.census.persona.BigFive;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * A one-shot snapshot of a mob's inner life, sent server→client when a player
 * inspects it with the census book. Carries the server-only layers (memories,
 * opinion-of-you, reflection) that the client can't read from synced
 * attachments; the client opens the profile screen with it.
 *
 * @param fullName   display name
 * @param generation 0 for founders/animals (no lineage line shown)
 * @param personality the Big Five (traits are re-derived client-side)
 * @param emotion    acute emotions + mood (already decayed by the sender)
 * @param mindKey    translation key of the dominant reflection, or ""
 * @param opinion    the mob's opinion of the inspecting player
 * @param gameTime   sender's game time, for "n minutes ago" rendering
 * @param memories   most recent memories, newest first
 */
public record ProfilePayload(
        String fullName,
        int generation,
        BigFive personality,
        EmotionalState emotion,
        String mindKey,
        float opinion,
        long gameTime,
        List<MemoryLine> memories
) implements CustomPacketPayload {
    public record MemoryLine(String eventKey, long tick, float valence) {}

    public static final CustomPacketPayload.Type<ProfilePayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(CensusMod.MODID, "profile"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ProfilePayload> STREAM_CODEC =
            CustomPacketPayload.codec(ProfilePayload::write, ProfilePayload::read);

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeUtf(fullName);
        buf.writeVarInt(generation);
        BigFive.STREAM_CODEC.encode(buf, personality);
        EmotionalState.STREAM_CODEC.encode(buf, emotion);
        buf.writeUtf(mindKey);
        buf.writeFloat(opinion);
        buf.writeVarLong(gameTime);
        buf.writeVarInt(memories.size());
        for (MemoryLine line : memories) {
            buf.writeUtf(line.eventKey());
            buf.writeVarLong(line.tick());
            buf.writeFloat(line.valence());
        }
    }

    private static ProfilePayload read(RegistryFriendlyByteBuf buf) {
        String fullName = buf.readUtf();
        int generation = buf.readVarInt();
        BigFive personality = BigFive.STREAM_CODEC.decode(buf);
        EmotionalState emotion = EmotionalState.STREAM_CODEC.decode(buf);
        String mindKey = buf.readUtf();
        float opinion = buf.readFloat();
        long gameTime = buf.readVarLong();
        int count = buf.readVarInt();
        List<MemoryLine> memories = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            memories.add(new MemoryLine(buf.readUtf(), buf.readVarLong(), buf.readFloat()));
        }
        return new ProfilePayload(fullName, generation, personality, emotion,
                mindKey, opinion, gameTime, memories);
    }

    @Override
    public CustomPacketPayload.Type<ProfilePayload> type() {
        return TYPE;
    }
}
