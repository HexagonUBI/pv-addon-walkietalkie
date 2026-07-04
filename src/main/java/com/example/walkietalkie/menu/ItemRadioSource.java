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

/**
 * Backs the radio menu when it's opened from a handheld walkie-talkie (as opposed to a
 * placed Radio Station block).
 *
 * The walkie-talkie ItemStack isn't a stable object reference -- if we held onto the
 * ItemStack instance from the moment the menu opened, edits would vanish the moment the
 * player's inventory stack got copied/replaced elsewhere (this happens more often than
 * you'd expect: stacking, NBT comparisons, etc). So instead we remember WHERE the item
 * is (player + hand) and re-fetch it fresh on every read/write, exactly like
 * ConfigureWalkieC2S used to.
 *
 * Module/mic slot contents are mirrored from -- and written back to -- the stack's
 * vanilla DataComponents.CONTAINER component (the same component Bundles use), following
 * the standard NeoForge "item-backed container" pattern.
 */
public final class ItemRadioSource extends SimpleContainer implements RadioContainerSource {

    private final ServerPlayer player;
    private final InteractionHand hand;

    public ItemRadioSource(ServerPlayer player, InteractionHand hand) {
        super(3); // [0]=mic (unused/locked from the item context), [1..2]=modules
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
        stack.set(WTComponents.FREQUENCY.get(), frequency);
        if (WalkieTalkieItem.isEnabled(stack)) {
            // Tuning to a new channel while live changes which listeners hear it.
            RadioState.get(player.server).refreshListeners(player.server);
        }
    }

    @Override
    public boolean isMicActive() { return false; }

    @Override
    public void setMicActive(boolean active) { /* not applicable on handheld radio */ }

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
