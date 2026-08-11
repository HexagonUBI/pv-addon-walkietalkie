package com.example.walkietalkie.registry;

import com.example.walkietalkie.WalkieTalkieMod;
import com.example.walkietalkie.item.BatteryItem;
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

    public static final DeferredItem<BlockItem> TOOLBOX =
            REGISTER.registerSimpleBlockItem("toolbox", WTBlocks.TOOLBOX);

    public static final DeferredItem<BatteryItem> BATTERY =
            REGISTER.registerItem(
                    "battery",
                    BatteryItem::new,
                    new Item.Properties().stacksTo(1)
            );

    public static final DeferredItem<RadioModuleItem> MICROPHONE_MODULE =
            REGISTER.registerItem(
                    "microphone_module",
                    p -> new RadioModuleItem(p, RadioModuleItem.Type.MIC, null),
                    new Item.Properties().stacksTo(1)
            );

    public static final DeferredItem<RadioModuleItem> SPEAKER_MODULE =
            REGISTER.registerItem(
                    "speaker_module",
                    p -> new RadioModuleItem(p, RadioModuleItem.Type.SPEAKER, null),
                    new Item.Properties().stacksTo(1)
            );

    public static final DeferredItem<RadioModuleItem> INTERCEPTION_MODULE =
            REGISTER.registerItem(
                    "interception_module",
                    p -> new RadioModuleItem(p, RadioModuleItem.Type.INTERCEPTION, null),
                    new Item.Properties().stacksTo(1)
            );

    public static final DeferredItem<RadioModuleItem> DEBUG_MODULE_A =
            REGISTER.registerItem(
                    "debug_module_a",
                    p -> new RadioModuleItem(p, RadioModuleItem.Type.GENERIC, "A"),
                    new Item.Properties().stacksTo(1)
            );

    public static final DeferredItem<RadioModuleItem> DEBUG_MODULE_B =
            REGISTER.registerItem(
                    "debug_module_b",
                    p -> new RadioModuleItem(p, RadioModuleItem.Type.GENERIC, "B"),
                    new Item.Properties().stacksTo(1)
            );

    public static void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(WALKIE_TALKIE.get());
            event.accept(RADIO_STATION.get());
            event.accept(TOOLBOX.get());
            event.accept(BATTERY.get());
            event.accept(MICROPHONE_MODULE.get());
            event.accept(SPEAKER_MODULE.get());
            event.accept(INTERCEPTION_MODULE.get());
            event.accept(DEBUG_MODULE_A.get());
            event.accept(DEBUG_MODULE_B.get());
        }
    }

    private WTItems() {}
}
