package com.example.walkietalkie.block.entity;

import com.example.walkietalkie.item.RadioModuleItem;
import com.example.walkietalkie.menu.RadioContainerSource;
import com.example.walkietalkie.menu.RadioMenu;
import com.example.walkietalkie.registry.WTBlockEntities;
import com.example.walkietalkie.util.FrequencyUtil;
import com.example.walkietalkie.util.WTLog;
import com.example.walkietalkie.voice.WalkieVoiceServerAddon;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import org.jetbrains.annotations.Nullable;

public class RadioStationBlockEntity extends BlockEntity
        implements net.minecraft.world.Container, RadioContainerSource, MenuProvider {

    private static final WTLog LOGGER = WTLog.of("WalkieTalkie/Station");

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

    private boolean hasInterceptionModule() {
        return slotHas(2, RadioModuleItem.Type.INTERCEPTION) || slotHas(3, RadioModuleItem.Type.INTERCEPTION);
    }

    private boolean slotHas(int slot, RadioModuleItem.Type type) {
        ItemStack s = items.get(slot);
        return !s.isEmpty() && s.getItem() instanceof RadioModuleItem m && m.getModuleType() == type;
    }

    public boolean isSpeakerActive() { return hasSpeakerModule() && outputActive; }

    private void syncVoiceRelay() {
        if (!(level instanceof ServerLevel sl)) return;
        int deciFreq = FrequencyUtil.toDeci(FrequencyUtil.clamp(frequency));
        boolean listen = isSpeakerActive();
        boolean speak  = hasMicModule() && micActive;
        WalkieVoiceServerAddon.onStationUpdated(sl, worldPosition, deciFreq, listen, speak, hasInterceptionModule());
    }

    private void onStateChanged() {
        setChanged();
        syncVoiceRelay();
        if (level == null || level.isClientSide) return;
        BlockState state = getBlockState();
        level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        LOGGER.info("Station {} state pushed to clients: speaker={} mic={} freq={}",
                worldPosition, isSpeakerActive(), micActive, FrequencyUtil.clamp(frequency));
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, RadioStationBlockEntity be) {
        ClientBuzz.set(pos, be.isSpeakerActive());
    }

    @Override
    public void onLoad() {
        super.onLoad();
        syncVoiceRelay();
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && level.isClientSide) {
            ClientBuzz.set(worldPosition, false);
            return;
        }
        if (level instanceof ServerLevel sl) {
            WalkieVoiceServerAddon.onStationRemoved(sl, worldPosition);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider reg) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, reg);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private static final class ClientBuzz {
        static void set(BlockPos pos, boolean active) {
            if (FMLEnvironment.dist != Dist.CLIENT) return;
            com.example.walkietalkie.client.WTClientSounds.setStationBuzz(pos, active);
        }
    }

    @Override public float getFrequency() { return FrequencyUtil.clamp(frequency); }
    @Override public void setFrequency(float f) {
        float clamped = FrequencyUtil.clamp(f);
        if (clamped == frequency) return;
        this.frequency = clamped; onStateChanged();
    }
    @Override public boolean isEnabled() { return enabled; }
    @Override public void setEnabled(boolean v) {
        if (v == enabled) return;
        this.enabled = v; onStateChanged();
    }
    @Override public boolean isMicActive() { return micActive; }
    @Override public void setMicActive(boolean v) {
        if (v == micActive) return;
        this.micActive = v; onStateChanged();
    }
    @Override public boolean isOutputActive() { return outputActive; }
    @Override public void setOutputActive(boolean v) {
        if (v == outputActive) return;
        this.outputActive = v; onStateChanged();
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
        if (!r.isEmpty()) onStateChanged();
        return r;
    }
    @Override public ItemStack removeItemNoUpdate(int slot) { return ContainerHelper.takeItem(items, slot); }
    @Override public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        if (stack.getCount() > getMaxStackSize()) stack.setCount(getMaxStackSize());
        onStateChanged();
    }
    @Override public int getMaxStackSize() { return 1; }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) {
        if (!(stack.getItem() instanceof RadioModuleItem m)) return false;
        return switch (slot) {
            case 0 -> m.isMicrophone();
            case 1 -> m.isSpeaker();
            default -> m.fitsModuleSlot();
        };
    }
    @Override public void clearContent() { items.clear(); onStateChanged(); }

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
