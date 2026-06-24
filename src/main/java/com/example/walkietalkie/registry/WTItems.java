package com.example.walkietalkie.registry;

import com.example.walkietalkie.WalkieTalkieMod;
import com.example.walkietalkie.item.WalkieTalkieItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class WTItems {

    public static final DeferredRegister.Items REGISTER =
            DeferredRegister.createItems(WalkieTalkieMod.MOD_ID);

    public static final DeferredItem<WalkieTalkieItem> WALKIE_TALKIE =
            REGISTER.registerItem(
                    "walkie_talkie",
                    WalkieTalkieItem::new,
                    new Item.Properties().stacksTo(1)
            );

    public static void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(WALKIE_TALKIE.get());
        }
    }

    private WTItems() {}
}
