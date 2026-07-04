package com.example.walkietalkie.block.entity;

import com.example.walkietalkie.item.RadioModuleItem;
import com.example.walkietalkie.menu.RadioContainerSource;
import com.example.walkietalkie.menu.RadioMenu;
import com.example.walkietalkie.registry.WTBlockEntities;
import com.example.walkietalkie.util.FrequencyUtil;
import com.example.walkietalkie.voice.WalkieVoiceServerAddon;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Radio Station block entity: same frequency/power/module concept as the handheld
 * walkie-talkie, but placed in the world.
 *
 * Audio relay: when the station is powered on AND slot 0 holds a microphone module,
 * it registers itself as an active station in {@link WalkieVoiceServerAddon} (via
 * {@link WalkieVoiceServerAddon#onStationUpdated}). The voice addon then creates a
 * positional {@code ServerStaticSource} at the block's position so nearby players can
 * hear audio being transmitted on the station's frequency -- even if they have no
 * walkie-talkie of their own.
 *
 * For speaking INTO the station: that path goes through the normal walkie-talkie
 * activation -- a player transmitting on the same frequency naturally gets relayed here.
 * A dedicated "press-to-talk near the station" gesture is a future extension.
 */
public class RadioStationBlockEntity extends BlockEntity
        implements net.minecraft.world.Container, RadioContainerSource, MenuProvider {

    private static final int SLOT_COUNT = 3; // [0]=mic, [1..2]=modules

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private float frequency = 100.0F;
    private boolean enabled = false;
    /** True when the player has activated the mic-transmit button in the station GUI. */
    private boolean micActive = false;

    public RadioStationBlockEntity(BlockPos pos, BlockState state) {
        super(WTBlockEntities.RADIO_STATION.get(), pos, state);
    }

    // ---- voice relay gate ----

    /**
     * Calls the voice addon to create or destroy a positional static audio source at
     * this block's position. Called whenever enabled, frequency, mic module, or micActive changes.
     *
     * Behaviour:
     *   - Station ALWAYS listens (plays to nearby players) when enabled, regardless of mic module.
     *   - Station speaks INTO the frequency (captures nearby PV audio) only when
     *     enabled + mic module present + micActive = true.
     */
    private void syncVoiceRelay() {
        if (!(level instanceof ServerLevel sl)) return;
        int deciFreq = FrequencyUtil.toDeci(FrequencyUtil.clamp(frequency));
        // "listen" = true when the station is powered on (always, no mic needed)
        boolean listen = enabled;
        // "speak" = true only when mic module is present AND mic button is active
        boolean speak = enabled && hasMicModule() && micActive;
        WalkieVoiceServerAddon.onStationUpdated(sl, worldPosition, deciFreq, listen, speak);
    }

    private boolean hasMicModule() {
        ItemStack micSlot = items.get(0);
        return !micSlot.isEmpty()
                && micSlot.getItem() instanceof RadioModuleItem m
                && m.isMicrophone();
    }

    // ---- RadioContainerSource ----

    @Override
    public float getFrequency() { return FrequencyUtil.clamp(frequency); }

    @Override
    public void setFrequency(float frequency) {
        this.frequency = FrequencyUtil.clamp(frequency);
        setChanged();
        syncVoiceRelay();
    }

    @Override
    public boolean isEnabled() { return enabled; }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        setChanged();
        syncVoiceRelay();
    }

    @Override
    public boolean isMicActive() { return micActive; }

    @Override
    public void setMicActive(boolean active) {
        this.micActive = active;
        setChanged();
        syncVoiceRelay();
    }

    @Override
    public boolean stillValid(Player player) {
        if (level == null || level.getBlockEntity(worldPosition) != this) return false;
        return player.position().distanceToSqr(
                worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5
        ) <= 64.0;
    }

    // ---- Container ----

    @Override public int getContainerSize() { return SLOT_COUNT; }
    @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getItem(int slot) { return items.get(slot); }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack r = ContainerHelper.removeItem(items, slot, amount);
        if (!r.isEmpty()) { setChanged(); syncVoiceRelay(); }
        return r;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        if (stack.getCount() > getMaxStackSize()) stack.setCount(getMaxStackSize());
        setChanged();
        syncVoiceRelay(); // mic module inserted/removed -> update relay
    }

    @Override
    public int getMaxStackSize() { return 1; }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (!(stack.getItem() instanceof RadioModuleItem m)) return false;
        return slot == 0 ? m.isMicrophone() : !m.isMicrophone();
    }

    @Override
    public void clearContent() { items.clear(); setChanged(); syncVoiceRelay(); }

    // ---- MenuProvider ----

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.walkietalkie.radio_station");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inv, Player player) {
        return RadioMenu.forStation(containerId, inv, this, this);
    }

    // ---- persistence ----

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putFloat("frequency", frequency);
        tag.putBoolean("enabled", enabled);
        tag.putBoolean("micActive", micActive);
        tag.put("items", ContainerHelper.saveAllItems(new CompoundTag(), items, registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.frequency = tag.getFloat("frequency");
        this.enabled = tag.getBoolean("enabled");
        this.micActive = tag.getBoolean("micActive");
        this.items.clear();
        if (tag.contains("items")) {
            ContainerHelper.loadAllItems(tag.getCompound("items"), items, registries);
        }
        syncVoiceRelay();
    }
}
