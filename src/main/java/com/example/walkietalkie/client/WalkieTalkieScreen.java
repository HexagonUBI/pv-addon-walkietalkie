package com.example.walkietalkie.client;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

@Deprecated
public class WalkieTalkieScreen extends Screen {

    public WalkieTalkieScreen(InteractionHand hand, ItemStack stack) {
        super(Component.translatable("screen.walkietalkie.title"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
