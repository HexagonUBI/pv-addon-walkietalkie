package com.example.walkietalkie.registry;

import com.example.walkietalkie.WalkieTalkieMod;
import com.example.walkietalkie.block.RadioStationBlock;
import com.example.walkietalkie.block.ToolboxBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class WTBlocks {

    public static final DeferredRegister.Blocks REGISTER =
            DeferredRegister.createBlocks(WalkieTalkieMod.MOD_ID);

    public static final DeferredBlock<RadioStationBlock> RADIO_STATION =
            REGISTER.register("radio_station", () -> new RadioStationBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_BLACK)
                            .strength(2.0F, 6.0F)
                            .sound(SoundType.NETHERITE_BLOCK)
                            .noOcclusion()
            ));

    public static final DeferredBlock<ToolboxBlock> TOOLBOX =
            REGISTER.register("toolbox", () -> new ToolboxBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_RED)
                            .strength(2.0F, 6.0F)
                            .sound(SoundType.METAL)
                            .noOcclusion()
            ));

    private WTBlocks() {}
}
