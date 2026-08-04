package com.example.walkietalkie.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public final class WTClientHooks {

    public static void openConfigScreen(InteractionHand hand) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        ItemStack stack = mc.player.getItemInHand(hand);
        mc.setScreen(new WalkieTalkieScreen(hand, stack));
    }

    private WTClientHooks() {}
}
