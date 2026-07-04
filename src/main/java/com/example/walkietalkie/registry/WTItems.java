package com.example.walkietalkie.registry;

import com.example.walkietalkie.WalkieTalkieMod;
import com.example.walkietalkie.item.RadioModuleItem;
import com.example.walkietalkie.item.WalkieTalkieItem;
import net.minecraft.world.item.BlockItem;
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

    public static final DeferredItem<BlockItem> RADIO_STATION =
            REGISTER.registerSimpleBlockItem("radio_station", WTBlocks.RADIO_STATION);

    // ---- modules ----
    // Generic module slots accept any of these EXCEPT the microphone; the dedicated mic
    // slot accepts ONLY the microphone. "Few debug modules to test the slots work" --
    // these don't grant any feature yet, they just exist and are insertable.

    public static final DeferredItem<RadioModuleItem> MICROPHONE_MODULE =
            REGISTER.registerItem(
                    "microphone_module",
                    p -> new RadioModuleItem(p, true, null),
                    new Item.Properties().stacksTo(1)
            );

    public static final DeferredItem<RadioModuleItem> DEBUG_MODULE_A =
            REGISTER.registerItem(
                    "debug_module_a",
                    p -> new RadioModuleItem(p, false, "A"),
                    new Item.Properties().stacksTo(1)
            );

    public static final DeferredItem<RadioModuleItem> DEBUG_MODULE_B =
            REGISTER.registerItem(
                    "debug_module_b",
                    p -> new RadioModuleItem(p, false, "B"),
                    new Item.Properties().stacksTo(1)
            );

    public static void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(WALKIE_TALKIE.get());
            event.accept(RADIO_STATION.get());
            event.accept(MICROPHONE_MODULE.get());
            event.accept(DEBUG_MODULE_A.get());
            event.accept(DEBUG_MODULE_B.get());
        }
    }

    private WTItems() {}
}
