package com.example.walkietalkie.client;

import com.example.walkietalkie.WalkieTalkieMod;
import com.example.walkietalkie.menu.RadioMenu;
import com.example.walkietalkie.net.payload.RadioMenuUpdateC2S;
import com.example.walkietalkie.util.FrequencyUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Screen for both the handheld walkie-talkie and the placed Radio Station.
 *
 * Layout (coords relative to leftPos/topPos):
 *   y=0..71  : radio config panel (180px texture blitted at leftPos-2)
 *   y=76     : "Inventory" label
 *   y=88..163: player inventory rows (3×9) + hotbar
 *
 * Controls:
 *   - Slider (drag): coarse frequency tuning
 *   - [<] [>] buttons: ±0.1 MHz fine tuning
 *   - Mouse wheel over slider: ±0.1 MHz
 *   - On/Off button
 *   - Mic button (stations with mic slot only)
 */
public class RadioScreen extends AbstractContainerScreen<RadioMenu> {

    // textures
    private static final ResourceLocation PANEL   = id("textures/gui/radio_configuration_screen.png");
    private static final ResourceLocation BTN_OFF = id("textures/gui/radio_configuration_screen_off.png");
    private static final ResourceLocation BTN_ON  = id("textures/gui/radio_configuration_screen_on.png");
    private static final ResourceLocation SLIDER_HANDLE = id("textures/gui/radio_configuration_screen_slider.png");
    private static final ResourceLocation VANILLA_INV =
            ResourceLocation.withDefaultNamespace("textures/gui/container/inventory.png");

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(WalkieTalkieMod.MOD_ID, path);
    }

    // panel
    private static final int PANEL_W = 180, PANEL_H = 71;
    private static final int PANEL_DX = -2; // shift left to centre 180px panel in 176px screen

    // slider track (texture-space, adjusted for PANEL_DX)
    private static final int TRACK_X = 4, TRACK_Y = 18, TRACK_W = 94, TRACK_H = 12;
    private static final int HANDLE_W = 6, HANDLE_H = 12;

    // on/off button (texture-space adjusted by PANEL_DX)
    private static final int BTN_X = 103, BTN_Y = 17, BTN_W = 23, BTN_H = 14;

    // fine-tune arrow buttons: [<] left of frequency label, [>] right of it
    private static final int ARROW_W = 10, ARROW_H = 10;
    private static final int ARROW_L_X = 4,   ARROW_Y = 5;  // [<]
    private static final int ARROW_R_X = 130,  /*same y*/     ARROW_RY = 5; // [>]

    // mic button (station only, right side of panel near power button)
    private static final int MIC_BTN_X = 103, MIC_BTN_Y = 34, MIC_BTN_W = 23, MIC_BTN_H = 14;

    // player inventory
    private static final int INV_Y  = 88;
    private static final int INV_LX = 7;
    // vanilla inventory.png crop: V=83, H=76 = 3 rows(54) + gap(4) + hotbar(18)
    private static final int INV_VU = 7, INV_VV = 83, INV_VW = 162, INV_VH = 76;
    private static final int LABEL_Y = 76; // 12px above INV_Y

    // state
    private boolean draggingSlider = false;
    private int lastSentDeci = Integer.MIN_VALUE;

    public RadioScreen(RadioMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        this.imageWidth = 176;
        this.imageHeight = INV_Y + INV_VH; // 88 + 76 = 164
        this.inventoryLabelY = LABEL_Y;
    }

    @Override
    protected void renderBg(GuiGraphics gfx, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos, y = this.topPos;

        // Radio panel (180px, centred in 176px screen)
        gfx.blit(PANEL, x + PANEL_DX, y, 0, 0, PANEL_W, PANEL_H, PANEL_W, PANEL_H);

        // Slider handle
        gfx.blit(SLIDER_HANDLE,
                x + TRACK_X + handleOffset(), y + TRACK_Y,
                0, 0, HANDLE_W, HANDLE_H, HANDLE_W, HANDLE_H);

        // On/Off button
        gfx.blit(menu.isEnabled() ? BTN_ON : BTN_OFF,
                x + BTN_X, y + BTN_Y, 0, 0, BTN_W, BTN_H, BTN_W, BTN_H);

        // Mic button (station only)
        if (menu.hasMicSlot()) {
            boolean micOn = menu.isMicActive();
            gfx.blit(micOn ? BTN_ON : BTN_OFF,
                    x + MIC_BTN_X, y + MIC_BTN_Y, 0, 0, MIC_BTN_W, MIC_BTN_H, MIC_BTN_W, MIC_BTN_H);
        }

        // Fine-tune buttons [<] [>]
        gfx.fill(x + ARROW_L_X,  y + ARROW_Y, x + ARROW_L_X + ARROW_W,  y + ARROW_Y + ARROW_H, 0xFF555555);
        gfx.fill(x + ARROW_R_X, y + ARROW_RY, x + ARROW_R_X + ARROW_W, y + ARROW_RY + ARROW_H, 0xFF555555);
        gfx.drawString(this.font, "<", x + ARROW_L_X + 2, y + ARROW_Y + 1, 0xFFFFFFFF, false);
        gfx.drawString(this.font, ">", x + ARROW_R_X + 2, y + ARROW_RY + 1, 0xFFFFFFFF, false);

        // Player inventory slot backgrounds (borrowed from inventory.png for texture-pack compat)
        gfx.blit(VANILLA_INV, x + INV_LX, y + INV_Y, INV_VU, INV_VV, INV_VW, INV_VH, 176, 166);
    }

    @Override
    protected void renderLabels(GuiGraphics gfx, int mouseX, int mouseY) {
        // Frequency display with MHz suffix
        Component freqLabel = Component.translatable(
                "screen.walkietalkie.frequency_value",
                FrequencyUtil.format(menu.getFrequency()) + " MHz");
        gfx.drawString(this.font, freqLabel, 15 + (-PANEL_DX), 6, 0xFFA0FFC0, false);

        // Mic label (station only)
        if (menu.hasMicSlot()) {
            gfx.drawString(this.font,
                    Component.translatable("screen.walkietalkie.mic"),
                    MIC_BTN_X - 20, MIC_BTN_Y + 3, 0xFFA0FFC0, false);
        }

        // "Inventory" above player inventory rows
        gfx.drawString(this.font, this.playerInventoryTitle, INV_LX, LABEL_Y, 0x404040, false);
    }

    // ---- slider math ----

    private int handleOffset() {
        float min = FrequencyUtil.min(), max = FrequencyUtil.max();
        float t = max > min ? (menu.getFrequency() - min) / (max - min) : 0F;
        t = Math.max(0F, Math.min(1F, t));
        return Math.round(t * (TRACK_W - HANDLE_W));
    }

    private float freqForSliderX(double mx) {
        int rel = (int) Math.round(mx) - (this.leftPos + TRACK_X);
        int usable = TRACK_W - HANDLE_W;
        float t = usable > 0 ? (rel - HANDLE_W / 2F) / usable : 0F;
        t = Math.max(0F, Math.min(1F, t));
        float raw = FrequencyUtil.min() + t * (FrequencyUtil.max() - FrequencyUtil.min());
        return FrequencyUtil.fromDeci(FrequencyUtil.toDeci(raw));
    }

    // ---- hit-test helpers ----

    private boolean inTrack(double mx, double my) {
        return mx >= leftPos + TRACK_X && mx < leftPos + TRACK_X + TRACK_W
                && my >= topPos + TRACK_Y && my < topPos + TRACK_Y + TRACK_H;
    }

    private boolean inButton(double mx, double my) {
        return mx >= leftPos + BTN_X && mx < leftPos + BTN_X + BTN_W
                && my >= topPos + BTN_Y && my < topPos + BTN_Y + BTN_H;
    }

    private boolean inMicButton(double mx, double my) {
        return menu.hasMicSlot()
                && mx >= leftPos + MIC_BTN_X && mx < leftPos + MIC_BTN_X + MIC_BTN_W
                && my >= topPos + MIC_BTN_Y && my < topPos + MIC_BTN_Y + MIC_BTN_H;
    }

    private boolean inArrowLeft(double mx, double my) {
        return mx >= leftPos + ARROW_L_X && mx < leftPos + ARROW_L_X + ARROW_W
                && my >= topPos + ARROW_Y && my < topPos + ARROW_Y + ARROW_H;
    }

    private boolean inArrowRight(double mx, double my) {
        return mx >= leftPos + ARROW_R_X && mx < leftPos + ARROW_R_X + ARROW_W
                && my >= topPos + ARROW_RY && my < topPos + ARROW_RY + ARROW_H;
    }

    // ---- input ----

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (btn == 0) {
            if (inTrack(mx, my)) {
                draggingSlider = true;
                sendFreq(freqForSliderX(mx));
                return true;
            }
            if (inButton(mx, my)) {
                send(FrequencyUtil.toDeci(menu.getFrequency()), !menu.isEnabled(), menu.isMicActive());
                return true;
            }
            if (inMicButton(mx, my)) {
                send(FrequencyUtil.toDeci(menu.getFrequency()), menu.isEnabled(), !menu.isMicActive());
                return true;
            }
            if (inArrowLeft(mx, my)) {
                nudgeFreq(-1);
                return true;
            }
            if (inArrowRight(mx, my)) {
                nudgeFreq(+1);
                return true;
            }
        }
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        if (draggingSlider && btn == 0) { sendFreq(freqForSliderX(mx)); return true; }
        return super.mouseDragged(mx, my, btn, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        if (btn == 0) draggingSlider = false;
        return super.mouseReleased(mx, my, btn);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        if (inTrack(mx, my) || (mx >= leftPos + TRACK_X && mx < leftPos + TRACK_X + TRACK_W
                && my >= topPos && my < topPos + PANEL_H)) {
            nudgeFreq(scrollY > 0 ? +1 : -1);
            return true;
        }
        return super.mouseScrolled(mx, my, scrollX, scrollY);
    }

    private void nudgeFreq(int direction) {
        int deci = FrequencyUtil.toDeci(menu.getFrequency()) + direction;
        deci = Math.max(FrequencyUtil.toDeci(FrequencyUtil.min()),
                Math.min(FrequencyUtil.toDeci(FrequencyUtil.max()), deci));
        if (deci != lastSentDeci) send(deci, menu.isEnabled(), menu.isMicActive());
    }

    private void sendFreq(float freq) {
        int deci = FrequencyUtil.toDeci(freq);
        if (deci != lastSentDeci) send(deci, menu.isEnabled(), menu.isMicActive());
    }

    private void send(int deciFrequency, boolean enabled, boolean micActive) {
        lastSentDeci = deciFrequency;
        PacketDistributor.sendToServer(
                new RadioMenuUpdateC2S(menu.containerId, deciFrequency, enabled, micActive));
    }
}
