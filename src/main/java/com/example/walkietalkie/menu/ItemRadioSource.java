package com.example.walkietalkie.menu;

import com.example.walkietalkie.item.WalkieTalkieItem;
import com.example.walkietalkie.registry.WTComponents;
import com.example.walkietalkie.voice.RadioState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

public final class ItemRadioSource extends SimpleContainer implements RadioContainerSource {

    private final ServerPlayer player;
    private final InteractionHand hand;

    public ItemRadioSource(ServerPlayer player, InteractionHand hand) {
        super(3);
        this.player = player;
        this.hand = hand;

        ItemContainerContents contents = stack().getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        contents.copyInto(this.getItems());
    }

    private ItemStack stack() {
        return player.getItemInHand(hand);
    }

    @Override
    public void setChanged() {
        super.setChanged();
        ItemStack stack = stack();
        if (stack.getItem() instanceof WalkieTalkieItem) {
            stack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(this.getItems()));
        }
    }

    @Override
    public float getFrequency() {
        return WalkieTalkieItem.frequencyOf(stack());
    }

    @Override
    public void setFrequency(float frequency) {
        ItemStack stack = stack();
        if (frequency == WalkieTalkieItem.frequencyOf(stack)) return;
        stack.set(WTComponents.FREQUENCY.get(), frequency);
        if (WalkieTalkieItem.isEnabled(stack)) {
            RadioState.get(player.server).refreshListeners(player.server);
        }
    }

    @Override public boolean isMicActive() { return false; }
    @Override public void setMicActive(boolean active) {}
    @Override public boolean isOutputActive() { return false; }
    @Override public void setOutputActive(boolean active) {}

    @Override
    public boolean isEnabled() {
        return WalkieTalkieItem.isEnabled(stack());
    }

    @Override
    public void setEnabled(boolean enabled) {
        WalkieTalkieItem.setEnabled(stack(), player, enabled);
    }

    @Override
    public boolean stillValid(Player p) {
        return p == player && stack().getItem() instanceof WalkieTalkieItem;
    }
}
