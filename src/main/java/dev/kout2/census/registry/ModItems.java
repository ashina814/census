package dev.kout2.census.registry;

import dev.kout2.census.CensusMod;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Item registry for Census mod.
 *
 * Phase 0: just the Census Book (placeholder, will hold family history in Phase 8).
 */
public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CensusMod.MODID);

    public static final DeferredItem<Item> CENSUS_BOOK =
            ITEMS.registerSimpleItem("census_book", props -> props.stacksTo(1));

    private ModItems() {}

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
