package com.example.walkietalkie.registry;

import com.example.walkietalkie.WalkieTalkieMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class WTSounds {

    public static final DeferredRegister<SoundEvent> REGISTER =
            DeferredRegister.create(Registries.SOUND_EVENT, WalkieTalkieMod.MOD_ID);

    public static final DeferredHolder<SoundEvent, SoundEvent> TOGGLE_ON = register("toggle_on");
    public static final DeferredHolder<SoundEvent, SoundEvent> TOGGLE_OFF = register("toggle_off");
    public static final DeferredHolder<SoundEvent, SoundEvent> TALK_START = register("talk_start");
    public static final DeferredHolder<SoundEvent, SoundEvent> TALK_STOP = register("talk_stop");
    public static final DeferredHolder<SoundEvent, SoundEvent> RADIO_STATIC = register("radio_static");

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(WalkieTalkieMod.MOD_ID, name);
        return REGISTER.register(name, () -> SoundEvent.createVariableRangeEvent(id));
    }

    private WTSounds() {}
}
