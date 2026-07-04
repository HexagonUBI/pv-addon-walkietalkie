package com.example.walkietalkie.client;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/**
 * @deprecated Replaced by {@link RadioScreen} (opened via a real {@code AbstractContainerMenu},
 * embedded in the player's inventory screen, with a frequency slider + module slots instead of
 * this plain popup). Nothing in the mod calls this class anymore -- {@code WalkieTalkieItem.use}
 * now opens {@code RadioMenu} directly via {@code player.openMenu(...)}. Kept only so
 * {@link WTClientHooks} (also dead/unused) still compiles; safe to delete both files.
 */
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
