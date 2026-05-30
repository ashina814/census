package dev.kout2.census.registry;

import dev.kout2.census.CensusMod;
import dev.kout2.census.block.GravestoneBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, CensusMod.MODID);

    public static final Supplier<BlockEntityType<GravestoneBlockEntity>> GRAVESTONE =
            BLOCK_ENTITIES.register("gravestone", () ->
                    new BlockEntityType<>(GravestoneBlockEntity::new, ModBlocks.GRAVESTONE.get()));

    private ModBlockEntities() {}

    public static void register(IEventBus bus) {
        BLOCK_ENTITIES.register(bus);
    }
}
