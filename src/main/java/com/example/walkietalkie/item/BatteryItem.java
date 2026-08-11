package com.example.walkietalkie.item;

import com.example.walkietalkie.registry.WTComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class BatteryItem extends Item {

    public static final int MAX_CHARGE = 100;

    private static final int BAR_COLOR = 0xC6262B;

    public BatteryItem(Properties properties) {
        super(properties);
    }

    public static int chargeOf(ItemStack stack) {
        return Mth.clamp(stack.getOrDefault(WTComponents.CHARGE.get(), MAX_CHARGE), 0, MAX_CHARGE);
    }

    public static void setCharge(ItemStack stack, int charge) {
        stack.set(WTComponents.CHARGE.get(), Mth.clamp(charge, 0, MAX_CHARGE));
    }

    public static float chargeFraction(ItemStack stack) {
        if (!(stack.getItem() instanceof BatteryItem)) return 0.0F;
        return chargeOf(stack) / (float) MAX_CHARGE;
    }

    public static int chargePercent(ItemStack stack) {
        return Math.round(chargeFraction(stack) * 100.0F);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return chargeOf(stack) < MAX_CHARGE;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(chargeFraction(stack) * 13.0F);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return BAR_COLOR;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.walkietalkie.battery_charge", chargePercent(stack))
                .withStyle(ChatFormatting.GOLD));
    }
}
