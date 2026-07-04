package com.example.walkietalkie.registry;

import com.example.walkietalkie.WalkieTalkieMod;
import com.example.walkietalkie.block.entity.RadioStationBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class WTBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> REGISTER =
            DeferredRegister.create(net.minecraft.core.registries.Registries.BLOCK_ENTITY_TYPE, WalkieTalkieMod.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RadioStationBlockEntity>> RADIO_STATION =
            REGISTER.register("radio_station", () -> BlockEntityType.Builder.of(
                    RadioStationBlockEntity::new, WTBlocks.RADIO_STATION.get()
            ).build(null));

    private WTBlockEntities() {}
}
