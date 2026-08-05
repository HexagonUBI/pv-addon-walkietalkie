package com.example.walkietalkie.block.entity;

import com.example.walkietalkie.item.RadioModuleItem;
import com.example.walkietalkie.menu.RadioContainerSource;
import com.example.walkietalkie.menu.RadioMenu;
import com.example.walkietalkie.registry.WTBlockEntities;
import com.example.walkietalkie.util.FrequencyUtil;
import com.example.walkietalkie.voice.WalkieVoiceServerAddon;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class RadioStationBlockEntity extends BlockEntity
        implements net.minecraft.world.Container, RadioContainerSource, MenuProvider {

    private static final int SLOT_COUNT = 4;

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private float   frequency    = 100.0F;
    private boolean enabled      = false;
    private boolean micActive    = false;
    private boolean outputActive = false;

    public RadioStationBlockEntity(BlockPos pos, BlockState state) {
        super(WTBlockEntities.RADIO_STATION.get(), pos, state);
    }

    private boolean hasMicModule()     { return slotHas(0, RadioModuleItem.Type.MIC); }
    private boolean hasSpeakerModule() { return slotHas(1, RadioModuleItem.Type.SPEAKER); }

    private boolean slotHas(int slot, RadioModuleItem.Type type) {
        ItemStack s = items.get(slot);
        return !s.isEmpty() && s.getItem() instanceof RadioModuleItem m && m.getModuleType() == type;
    }

    private void syncVoiceRelay() {
        if (!(level instanceof ServerLevel sl)) return;
        int deciFreq = FrequencyUtil.toDeci(FrequencyUtil.clamp(frequency));
        boolean listen = hasSpeakerModule() && outputActive;
        boolean speak  = hasMicModule() && micActive;
        WalkieVoiceServerAddon.onStationUpdated(sl, worldPosition, deciFreq, listen, speak);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        syncVoiceRelay();
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level instanceof ServerLevel sl) {
            WalkieVoiceServerAddon.onStationRemoved(sl, worldPosition);
        }
    }

    @Override public float getFrequency() { return FrequencyUtil.clamp(frequency); }
    @Override public void setFrequency(float f) {
        float clamped = FrequencyUtil.clamp(f);
        if (clamped == frequency) return;
        this.frequency = clamped; setChanged(); syncVoiceRelay();
    }
    @Override public boolean isEnabled() { return enabled; }
    @Override public void setEnabled(boolean v) {
        if (v == enabled) return;
        this.enabled = v; setChanged(); syncVoiceRelay();
    }
    @Override public boolean isMicActive() { return micActive; }
    @Override public void setMicActive(boolean v) {
        if (v == micActive) return;
        this.micActive = v; setChanged(); syncVoiceRelay();
    }
    @Override public boolean isOutputActive() { return outputActive; }
    @Override public void setOutputActive(boolean v) {
        if (v == outputActive) return;
        this.outputActive = v; setChanged(); syncVoiceRelay();
    }

    @Override
    public boolean stillValid(Player player) {
        if (level == null || level.getBlockEntity(worldPosition) != this) return false;
        if (player.level() != level) return true;
        return player.position().distanceToSqr(
                com.example.walkietalkie.compat.SableBridge.resolvePosition(level, worldPosition)) <= 64.0;
    }

    @Override public int getContainerSize() { return SLOT_COUNT; }
    @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getItem(int slot) { return items.get(slot); }
    @Override public ItemStack removeItem(int slot, int amount) {
        ItemStack r = ContainerHelper.removeItem(items, slot, amount);
        if (!r.isEmpty()) { setChanged(); syncVoiceRelay(); }
        return r;
    }
    @Override public ItemStack removeItemNoUpdate(int slot) { return ContainerHelper.takeItem(items, slot); }
    @Override public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        if (stack.getCount() > getMaxStackSize()) stack.setCount(getMaxStackSize());
        setChanged(); syncVoiceRelay();
    }
    @Override public int getMaxStackSize() { return 1; }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) {
        if (!(stack.getItem() instanceof RadioModuleItem m)) return false;
        return switch (slot) {
            case 0 -> m.isMicrophone();
            case 1 -> m.isSpeaker();
            default -> m.getModuleType() == RadioModuleItem.Type.GENERIC;
        };
    }
    @Override public void clearContent() { items.clear(); setChanged(); syncVoiceRelay(); }

    @Override public Component getDisplayName() {
        return Component.translatable("block.walkietalkie.radio_station");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return RadioMenu.forStation(id, inv, this, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider reg) {
        super.saveAdditional(tag, reg);
        tag.putFloat("frequency", frequency);
        tag.putBoolean("enabled", enabled);
        tag.putBoolean("micActive", micActive);
        tag.putBoolean("outputActive", outputActive);
        tag.put("items", ContainerHelper.saveAllItems(new CompoundTag(), items, reg));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider reg) {
        super.loadAdditional(tag, reg);
        this.frequency    = tag.getFloat("frequency");
        this.enabled      = tag.getBoolean("enabled");
        this.micActive    = tag.getBoolean("micActive");
        this.outputActive = tag.getBoolean("outputActive");
        this.items.clear();
        if (tag.contains("items")) ContainerHelper.loadAllItems(tag.getCompound("items"), items, reg);
    }
}
