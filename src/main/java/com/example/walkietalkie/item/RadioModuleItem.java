package com.example.walkietalkie.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class RadioModuleItem extends Item {

    public enum Type { MIC, SPEAKER, GENERIC }

    private final Type type;
    private final String debugLabel;

    public RadioModuleItem(Properties properties, Type type, String debugLabel) {
        super(properties);
        this.type = type;
        this.debugLabel = debugLabel;
    }

    public RadioModuleItem(Properties properties, boolean microphone, String debugLabel) {
        this(properties, microphone ? Type.MIC : Type.GENERIC, debugLabel);
    }

    public Type getModuleType() { return type; }
    public boolean isMicrophone() { return type == Type.MIC; }
    public boolean isSpeaker()    { return type == Type.SPEAKER; }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> lines, TooltipFlag flag) {
        if (debugLabel != null) {
            lines.add(Component.translatable("tooltip.walkietalkie.module_debug", debugLabel)
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
