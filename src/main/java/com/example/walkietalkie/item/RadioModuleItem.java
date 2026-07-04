package com.example.walkietalkie.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Base item type for anything that can be inserted into a radio's module slots
 * (walkie-talkie or Radio Station). The two generic module slots accept any
 * RadioModuleItem EXCEPT a microphone; the dedicated microphone slot accepts ONLY
 * a microphone. See RadioMenu's slot predicates.
 *
 * These don't do anything yet beyond existing and being insertable -- this is the
 * "few debug modules to test that the slots work" scope. Real module behavior
 * (actually granting features once installed) is a follow-up.
 */
public class RadioModuleItem extends Item {

    private final boolean microphone;
    private final String debugLabel; // null for non-debug modules (currently only the mic)

    public RadioModuleItem(Properties properties, boolean microphone, String debugLabel) {
        super(properties);
        this.microphone = microphone;
        this.debugLabel = debugLabel;
    }

    public boolean isMicrophone() {
        return microphone;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> lines, TooltipFlag flag) {
        if (debugLabel != null) {
            lines.add(Component.translatable("tooltip.walkietalkie.module_debug", debugLabel)
                    .withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
        }
    }
}
