package dev.kout2.census.registry;

import dev.kout2.census.CensusMod;
import dev.kout2.census.block.GravestoneBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(CensusMod.MODID);

    public static final DeferredBlock<GravestoneBlock> GRAVESTONE =
            BLOCKS.registerBlock("gravestone", GravestoneBlock::new,
                    (BlockBehaviour.Properties props) -> props.strength(1.5f, 6.0f).noOcclusion());

    private ModBlocks() {}

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
    }
}
