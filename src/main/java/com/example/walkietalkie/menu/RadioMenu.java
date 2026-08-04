package com.example.walkietalkie.menu;

import com.example.walkietalkie.item.RadioModuleItem;
import com.example.walkietalkie.registry.WTMenus;
import com.example.walkietalkie.util.FrequencyUtil;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

public final class RadioMenu extends AbstractContainerMenu {

    private static final int MOD1_X   = 134, MOD1_Y   = 54;
    private static final int MOD2_X   = 152, MOD2_Y   = 54;
    private static final int MIC_X    = 26,  MIC_Y    = 36;
    private static final int SPEAK_X  = 26,  SPEAK_Y  = 54;
    private static final int INV_X    = 8,   INV_Y    = 84;

    private final RadioContainerSource source;
    private final ContainerData data;
    private final boolean hasMicSlot;

    public static RadioMenu forWalkie(int id, Inventory inv, ItemRadioSource src) {
        return new RadioMenu(WTMenus.WALKIE_MENU.get(), id, inv, src, src, false);
    }

    public static RadioMenu forStation(int id, Inventory inv, Container modules, RadioContainerSource src) {
        return new RadioMenu(WTMenus.RADIO_STATION_MENU.get(), id, inv, src, modules, true);
    }

    public static RadioMenu clientWalkie(int id, Inventory inv) {
        return new RadioMenu(WTMenus.WALKIE_MENU.get(), id, inv, RadioContainerSource.DUMMY, new SimpleContainer(3), false);
    }

    public static RadioMenu clientStation(int id, Inventory inv) {
        return new RadioMenu(WTMenus.RADIO_STATION_MENU.get(), id, inv, RadioContainerSource.DUMMY, new SimpleContainer(4), true);
    }

    private RadioMenu(MenuType<?> type, int id, Inventory inv,
                      RadioContainerSource source, Container modules, boolean hasMicSlot) {
        super(type, id);
        this.source = source;
        this.hasMicSlot = hasMicSlot;

        int moduleSlotCount = hasMicSlot ? 4 : 2;
        checkContainerSize(modules, moduleSlotCount);

        if (hasMicSlot) {
            addSlot(new Slot(modules, 0, MIC_X, MIC_Y) {
                @Override public boolean mayPlace(ItemStack s) {
                    return s.getItem() instanceof RadioModuleItem m && m.isMicrophone();
                }
            });
            addSlot(new Slot(modules, 1, SPEAK_X, SPEAK_Y) {
                @Override public boolean mayPlace(ItemStack s) {
                    return s.getItem() instanceof RadioModuleItem m && m.isSpeaker();
                }
            });
            addSlot(new Slot(modules, 2, MOD1_X, MOD1_Y) {
                @Override public boolean mayPlace(ItemStack s) {
                    return s.getItem() instanceof RadioModuleItem m
                            && m.getModuleType() == RadioModuleItem.Type.GENERIC;
                }
            });
            addSlot(new Slot(modules, 3, MOD2_X, MOD2_Y) {
                @Override public boolean mayPlace(ItemStack s) {
                    return s.getItem() instanceof RadioModuleItem m
                            && m.getModuleType() == RadioModuleItem.Type.GENERIC;
                }
            });
        } else {
            addSlot(new Slot(modules, 1, MOD1_X, MOD1_Y) {
                @Override public boolean mayPlace(ItemStack s) {
                    return s.getItem() instanceof RadioModuleItem m
                            && m.getModuleType() == RadioModuleItem.Type.GENERIC;
                }
            });
            addSlot(new Slot(modules, 2, MOD2_X, MOD2_Y) {
                @Override public boolean mayPlace(ItemStack s) {
                    return s.getItem() instanceof RadioModuleItem m
                            && m.getModuleType() == RadioModuleItem.Type.GENERIC;
                }
            });
        }
        addPlayerSlots(inv, INV_X, INV_Y);

        this.data = new SimpleContainerData(4);
        addDataSlots(data);
        data.set(0, FrequencyUtil.toDeci(source.getFrequency()));
        data.set(1, source.isEnabled()      ? 1 : 0);
        data.set(2, source.isMicActive()    ? 1 : 0);
        data.set(3, source.isOutputActive() ? 1 : 0);
    }

    private void addPlayerSlots(Inventory inv, int x, int y) {
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(inv, col + row * 9 + 9, x + col * 18, y + row * 18));
        for (int col = 0; col < 9; col++)
            addSlot(new Slot(inv, col, x + col * 18, y + 58));
    }

    public boolean hasMicSlot()    { return hasMicSlot; }
    public float  getFrequency()   { return FrequencyUtil.fromDeci(data.get(0)); }
    public boolean isEnabled()     { return data.get(1) != 0; }
    public boolean isMicActive()   { return data.get(2) != 0; }
    public boolean isOutputActive(){ return data.get(3) != 0; }

    public void serverApply(int deciFreq, boolean enabled, boolean micActive, boolean outputActive) {
        float clamped = FrequencyUtil.clamp(FrequencyUtil.fromDeci(deciFreq));
        source.setFrequency(clamped);
        source.setEnabled(enabled);
        source.setMicActive(micActive);
        source.setOutputActive(outputActive);
        data.set(0, FrequencyUtil.toDeci(clamped));
        data.set(1, enabled      ? 1 : 0);
        data.set(2, micActive    ? 1 : 0);
        data.set(3, outputActive ? 1 : 0);
    }

    @Override
    public boolean stillValid(Player player) { return source.stillValid(player); }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack copy = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) return copy;
        ItemStack moving = slot.getItem();
        copy = moving.copy();
        int moduleSlots = hasMicSlot ? 4 : 2;
        if (index < moduleSlots) {
            if (!this.moveItemStackTo(moving, moduleSlots, this.slots.size(), true)) return ItemStack.EMPTY;
        } else {
            if (!this.moveItemStackTo(moving, 0, moduleSlots, false)) return ItemStack.EMPTY;
        }
        if (moving.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }
}
