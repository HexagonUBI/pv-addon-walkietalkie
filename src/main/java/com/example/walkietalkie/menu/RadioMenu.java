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

/**
 * One menu class, two flavors:
 *
 *   • {@link #forWalkie}  -- opened from the handheld item. 2 module slots, NO mic slot
 *     (per the design: the mic socket is purely decorative/locked on the handheld radio).
 *   • {@link #forStation} -- opened from a placed Radio Station block. Mic slot + 2
 *     module slots, all live.
 *
 * Frequency + power state sync to the client via {@link ContainerData} (the same
 * mechanism vanilla's furnace uses for burn time, beacon for its power level, etc.) --
 * two plain ints: [0] = deci-frequency, [1] = enabled (0/1). This is automatic,
 * built-in, per-tick-diffed sync; we don't need to hand-roll any S2C packets for it.
 *
 * Edits travel the other way (client -> server) via RadioMenuUpdateC2S, validated
 * against the player's currently-open menu in WTPayloads, and applied through
 * {@link #serverApply}.
 */
public final class RadioMenu extends AbstractContainerMenu {

    // ---- texture-space slot positions (see radio_configuration_screen.png) ----
    // Slot positions in screen-local space (relative to leftPos/topPos).
    // The 180px panel texture is blitted at leftPos-2 (see RadioScreen.PANEL_BLIT_DX=-2),
    // so a pixel at texture-x T maps to local-x = T - 2.
    // From pixel analysis of radio_configuration_screen.png:
    //   mic socket interior  : texture (26,41) → local (24, 41)
    //   module slot 1        : texture (64,46) → local (62, 46)
    //   module slot 2        : texture (82,46) → local (80, 46)
    // Player inventory uses standard 176px-wide layout with X=7 (9 slots × 18px = 162px, 7+162=169 < 176).
    private static final int MIC_SLOT_X = 24, MIC_SLOT_Y = 41;
    private static final int MODULE1_X  = 62, MODULE1_Y  = 46;
    private static final int MODULE2_X  = 80, MODULE2_Y  = 46;
    // Inventory rows start 17px below the panel (71+17=88) leaving room for the
    // "Inventory" label at y=76 and a small gap before the first row at y=88.
    private static final int PLAYER_INV_X = 7, PLAYER_INV_Y = 88;

    private final RadioContainerSource source;
    private final ContainerData data;
    private final boolean hasMicSlot;

    // ---- server-side factories (real source behind them) ----

    public static RadioMenu forWalkie(int containerId, Inventory inv, ItemRadioSource source) {
        return new RadioMenu(WTMenus.WALKIE_MENU.get(), containerId, inv, source, source, false);
    }

    public static RadioMenu forStation(int containerId, Inventory inv, Container moduleContainer, RadioContainerSource source) {
        return new RadioMenu(WTMenus.RADIO_STATION_MENU.get(), containerId, inv, source, moduleContainer, true);
    }

    // ---- client-side factories (registered directly as MenuType suppliers; no real
    //      source exists yet, just a placeholder until ContainerData/slots sync in) ----

    public static RadioMenu clientWalkie(int containerId, Inventory inv) {
        return new RadioMenu(WTMenus.WALKIE_MENU.get(), containerId, inv, RadioContainerSource.DUMMY, new SimpleContainer(3), false);
    }

    public static RadioMenu clientStation(int containerId, Inventory inv) {
        return new RadioMenu(WTMenus.RADIO_STATION_MENU.get(), containerId, inv, RadioContainerSource.DUMMY, new SimpleContainer(3), true);
    }

    private RadioMenu(
            MenuType<?> type,
            int containerId,
            Inventory inv,
            RadioContainerSource source,
            Container moduleContainer,
            boolean hasMicSlot
    ) {
        super(type, containerId);
        this.source = source;
        this.hasMicSlot = hasMicSlot;
        checkContainerSize(moduleContainer, 3);

        if (hasMicSlot) {
            this.addSlot(new Slot(moduleContainer, 0, MIC_SLOT_X, MIC_SLOT_Y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return stack.getItem() instanceof RadioModuleItem module && module.isMicrophone();
                }
            });
        }
        this.addSlot(new Slot(moduleContainer, 1, MODULE1_X, MODULE1_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof RadioModuleItem module && !module.isMicrophone();
            }
        });
        this.addSlot(new Slot(moduleContainer, 2, MODULE2_X, MODULE2_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof RadioModuleItem module && !module.isMicrophone();
            }
        });

        // Player main inventory (3 rows × 9 slots)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inv, col + row * 9 + 9,
                        PLAYER_INV_X + col * 18,
                        PLAYER_INV_Y + row * 18));
            }
        }
        // Hotbar (9 slots, 58px below the top of the inv rows)
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inv, col,
                    PLAYER_INV_X + col * 18,
                    PLAYER_INV_Y + 58));
        }

        this.data = new SimpleContainerData(3);
        this.addDataSlots(data);
        data.set(0, FrequencyUtil.toDeci(source.getFrequency()));
        data.set(1, source.isEnabled() ? 1 : 0);
        data.set(2, source.isMicActive() ? 1 : 0);
    }

    public boolean hasMicSlot() {
        return hasMicSlot;
    }

    /** Current synced frequency -- safe to call from the client (this is what the screen reads). */
    public float getFrequency() {
        return FrequencyUtil.fromDeci(data.get(0));
    }

    /** Current synced power state -- safe to call from the client. */
    public boolean isEnabled() {
        return data.get(1) != 0;
    }

    /** Whether mic-transmit is active -- safe to call from the client. Only meaningful on stations. */
    public boolean isMicActive() {
        return data.get(2) != 0;
    }

    /**
     * Applies a client-requested change. Server-side only.
     */
    public void serverApply(int deciFrequency, boolean enabled, boolean micActive) {
        float clamped = FrequencyUtil.clamp(FrequencyUtil.fromDeci(deciFrequency));
        source.setFrequency(clamped);
        source.setEnabled(enabled);
        source.setMicActive(micActive);
        data.set(0, FrequencyUtil.toDeci(clamped));
        data.set(1, enabled ? 1 : 0);
        data.set(2, micActive ? 1 : 0);
    }

    @Override
    public boolean stillValid(Player player) {
        return source.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack copy = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) return copy;

        ItemStack moving = slot.getItem();
        copy = moving.copy();
        int moduleSlots = hasMicSlot ? 3 : 2;

        if (index < moduleSlots) {
            // Module area -> player inventory.
            if (!this.moveItemStackTo(moving, moduleSlots, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            // Player inventory -> first matching module/mic slot that accepts it.
            if (!this.moveItemStackTo(moving, 0, moduleSlots, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (moving.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return copy;
    }
}
