package dev.kout2.census.block;

import dev.kout2.census.registry.ModBlockEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Stores the epitaph of whoever lies here: their name, generation, the day they
 * died, and who (if anyone) did the deed.
 */
public class GravestoneBlockEntity extends BlockEntity {
    private String name = "";
    private int generation = 0;
    private long deathDay = 0L;
    private String killer = "";   // empty == died of natural causes

    public GravestoneBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GRAVESTONE.get(), pos, state);
    }

    public void inscribe(String name, int generation, long deathDay, String killer) {
        this.name = name;
        this.generation = generation;
        this.deathDay = deathDay;
        this.killer = killer;
        setChanged();
    }

    /** The full epitaph shown on right-click. */
    public Component epitaph() {
        MutableComponent text = Component.empty()
                .append(line(ChatFormatting.GRAY, Component.translatable("census.grave.here")))
                .append(line(ChatFormatting.GOLD, nameLine()))
                .append(line(ChatFormatting.GRAY, Component.translatable("census.grave.died", deathDay)));
        if (!killer.isEmpty()) {
            text.append(line(ChatFormatting.DARK_RED,
                    Component.translatable("census.grave.slain_by", killer)));
        }
        return text;
    }

    private Component nameLine() {
        MutableComponent n = Component.literal(name);
        if (generation > 0) {
            n.append(" ").append(Component.translatable("census.gen", generation));
        }
        return n;
    }

    private static MutableComponent line(ChatFormatting color, Component content) {
        return Component.empty().append(content).append("\n").withStyle(color);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putString("name", name);
        output.putInt("generation", generation);
        output.putLong("deathDay", deathDay);
        output.putString("killer", killer);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.name = input.getStringOr("name", "");
        this.generation = input.getIntOr("generation", 0);
        this.deathDay = input.getLongOr("deathDay", 0L);
        this.killer = input.getStringOr("killer", "");
    }
}
