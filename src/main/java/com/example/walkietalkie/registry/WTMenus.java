package com.example.walkietalkie.registry;

import com.example.walkietalkie.WalkieTalkieMod;
import com.example.walkietalkie.menu.RadioMenu;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Two MenuTypes, not one, even though both are backed by {@link RadioMenu}: the client
 * factory a MenuType calls back into has no way to know in advance "should this instance
 * have a mic slot or not" -- and slot COUNT has to match between client and server before
 * any syncing happens. So the two contexts (handheld item vs placed block) get distinct
 * MenuTypes, each baking in its own slot count via which RadioMenu factory it points at.
 * Same trick vanilla uses for GENERIC_9x1 vs GENERIC_9x3 chest variants.
 */
public final class WTMenus {

    public static final DeferredRegister<MenuType<?>> REGISTER =
            DeferredRegister.create(net.minecraft.core.registries.Registries.MENU, WalkieTalkieMod.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<RadioMenu>> WALKIE_MENU =
            REGISTER.register("walkie_menu", () ->
                    new MenuType<>(RadioMenu::clientWalkie, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<MenuType<?>, MenuType<RadioMenu>> RADIO_STATION_MENU =
            REGISTER.register("radio_station_menu", () ->
                    new MenuType<>(RadioMenu::clientStation, FeatureFlags.DEFAULT_FLAGS));

    private WTMenus() {}
}
