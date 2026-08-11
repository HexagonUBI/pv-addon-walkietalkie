package com.example.walkietalkie.client;

import com.example.walkietalkie.WalkieTalkieMod;
import com.example.walkietalkie.item.BatteryItem;
import com.example.walkietalkie.menu.RadioMenu;
import com.example.walkietalkie.net.payload.RadioMenuUpdateC2S;
import com.example.walkietalkie.util.FrequencyUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

public class RadioScreen extends AbstractContainerScreen<RadioMenu> {

    private static final ResourceLocation BTN_ON     = id("textures/gui/on.png");
    private static final ResourceLocation BTN_OFF    = id("textures/gui/off.png");
    private static final ResourceLocation BTN_ON_H   = id("textures/gui/on_s.png");
    private static final ResourceLocation BTN_OFF_H  = id("textures/gui/off_s.png");

    private static final ResourceLocation RS_BG      = id("textures/gui/radiostation_container.png");
    private static final ResourceLocation RS_HANDLE  = id("textures/gui/radiostation_slider_red.png");
    private static final ResourceLocation RS_HANDLE_H= id("textures/gui/radiostation_slider_red_h.png");
    private static final ResourceLocation RS_ARR_L   = id("textures/gui/radio_precise_arrow_l.png");
    private static final ResourceLocation RS_ARR_R   = id("textures/gui/radio_precise_arrow_r.png");
    private static final ResourceLocation RS_ARR_LH  = id("textures/gui/radio_precise_arrow_l_h.png");
    private static final ResourceLocation RS_ARR_RH  = id("textures/gui/radio_precise_arrow_r_h.png");

    private static final ResourceLocation WT_BG      = id("textures/gui/walkie_talkie/modern/walkietalkie_container.png");
    private static final ResourceLocation WT_HANDLE  = id("textures/gui/walkie_talkie/modern/frequency_select.png");
    private static final ResourceLocation WT_HANDLE_H= id("textures/gui/walkie_talkie/modern/frequency_select_h.png");
    private static final ResourceLocation WT_ARR_L   = id("textures/gui/walkie_talkie/modern/radio_precise_arrow_l.png");
    private static final ResourceLocation WT_ARR_R   = id("textures/gui/walkie_talkie/modern/radio_precise_arrow_r.png");
    private static final ResourceLocation WT_ARR_LH  = id("textures/gui/walkie_talkie/modern/radio_precise_arrow_l_h.png");
    private static final ResourceLocation WT_ARR_RH  = id("textures/gui/walkie_talkie/modern/radio_precise_arrow_r_h.png");
    private static final ResourceLocation WT_BAR_EMPTY = id("textures/gui/walkie_talkie/modern/power_bar_empty.png");
    private static final ResourceLocation WT_BAR_FULL  = id("textures/gui/walkie_talkie/modern/power_bar_full.png");

    private static final int IMAGE_W = 176, IMAGE_H = 166;

    private static final int RS_SHEET_W = 176, RS_SHEET_H = 166;
    private static final int WT_SHEET_W = 256, WT_SHEET_H = 256;

    private static final int RS_TRACK_X = 63, RS_TRACK_Y = 19, RS_TRACK_W = 104;
    private static final int RS_ARR_L_X = 15, RS_ARR_R_X = 49, RS_ARR_Y = 23;
    private static final int RS_FREQ_X = 20, RS_FREQ_Y = 21, RS_FREQ_W = 29, RS_FREQ_H = 12;
    private static final int RS_FREQ_COLOR = 0xFF5E5B4A;

    private static final int WT_TRACK_X = 35, WT_TRACK_Y = 18, WT_TRACK_W = 106;
    private static final int WT_HANDLE_DY = 1;
    private static final int WT_TITLE_X = 88, WT_TITLE_Y = 6;
    private static final int WT_TITLE_COLOR = 0xFFA8ABB5;
    private static final int WT_ARR_L_X = 36, WT_ARR_R_X = 71, WT_ARR_Y = 40;
    private static final int WT_FREQ_X = 35, WT_FREQ_Y = 36, WT_FREQ_W = 42, WT_FREQ_H = 16;
    private static final int WT_FREQ_COLOR = 0xFF511516;
    private static final int WT_PWR_X = 79, WT_PWR_Y = 36;
    private static final int WT_BAR_X = 34, WT_BAR_Y = 58, WT_BAR_W = 108, WT_BAR_H = 8;

    private static final int TRACK_H = 16;
    private static final int HANDLE_W = 5, HANDLE_H = 16;
    private static final int ARR_W = 5, ARR_H = 8;
    private static final int TOG_W = 16, TOG_H = 16;

    private static final int MIC_BTN_X = 44, MIC_BTN_Y = 36;
    private static final int SPK_BTN_X = 44, SPK_BTN_Y = 54;

    private static final int MHZ_LABEL_X = 13, MHZ_LABEL_Y = 8;
    private static final int FREQ_LABEL_X = 65, FREQ_LABEL_Y = 8;

    private static final int BAR_SEGMENTS = 10;
    private static final int[] BAR_SEGMENT_WIDTH = {8, 19, 30, 41, 52, 64, 75, 86, 97, 108};

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(WalkieTalkieMod.MOD_ID, path);
    }

    private boolean dragging = false;
    private int lastSentDeci = Integer.MIN_VALUE;
    private final boolean station;

    private final ResourceLocation bg, handleTex, handleTexH, arrL, arrR, arrLH, arrRH;
    private final int sheetW, sheetH;
    private final int trackX, trackY, trackW, handleDy;
    private final int arrLX, arrRX, arrY;
    private final int freqX, freqY, freqW, freqH, freqColor;

    public RadioScreen(RadioMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.station = menu.hasMicSlot();
        this.imageWidth  = IMAGE_W;
        this.imageHeight = IMAGE_H;
        this.inventoryLabelY = -100;

        this.bg         = station ? RS_BG       : WT_BG;
        this.handleTex  = station ? RS_HANDLE   : WT_HANDLE;
        this.handleTexH = station ? RS_HANDLE_H : WT_HANDLE_H;
        this.arrL       = station ? RS_ARR_L    : WT_ARR_L;
        this.arrR       = station ? RS_ARR_R    : WT_ARR_R;
        this.arrLH      = station ? RS_ARR_LH   : WT_ARR_LH;
        this.arrRH      = station ? RS_ARR_RH   : WT_ARR_RH;

        this.sheetW = station ? RS_SHEET_W : WT_SHEET_W;
        this.sheetH = station ? RS_SHEET_H : WT_SHEET_H;

        this.trackX = station ? RS_TRACK_X : WT_TRACK_X;
        this.trackY = station ? RS_TRACK_Y : WT_TRACK_Y;
        this.trackW = station ? RS_TRACK_W : WT_TRACK_W;
        this.handleDy = station ? 0 : WT_HANDLE_DY;

        this.arrLX = station ? RS_ARR_L_X : WT_ARR_L_X;
        this.arrRX = station ? RS_ARR_R_X : WT_ARR_R_X;
        this.arrY  = station ? RS_ARR_Y   : WT_ARR_Y;

        this.freqX     = station ? RS_FREQ_X     : WT_FREQ_X;
        this.freqY     = station ? RS_FREQ_Y     : WT_FREQ_Y;
        this.freqW     = station ? RS_FREQ_W     : WT_FREQ_W;
        this.freqH     = station ? RS_FREQ_H     : WT_FREQ_H;
        this.freqColor = station ? RS_FREQ_COLOR : WT_FREQ_COLOR;
    }

    @Override
    public void render(GuiGraphics gfx, int mx, int my, float partialTick) {
        super.render(gfx, mx, my, partialTick);
        this.renderTooltip(gfx, mx, my);

        if (!station && hoveredSlot == null) {
            if (inPowerBtn(mx, my)) {
                gfx.renderTooltip(font, Component.translatable(
                        menu.isEnabled() ? "tooltip.walkietalkie.on" : "tooltip.walkietalkie.off"), mx, my);
            } else if (inChargeBar(mx, my)) {
                ItemStack battery = menu.getBatteryStack();
                gfx.renderTooltip(font, battery.isEmpty()
                        ? Component.translatable("screen.walkietalkie.battery_missing")
                        : Component.translatable("screen.walkietalkie.battery", BatteryItem.chargePercent(battery)),
                        mx, my);
            }
        }
    }

    @Override
    protected void renderBg(GuiGraphics gfx, float dt, int mx, int my) {
        int x = leftPos, y = topPos;

        gfx.blit(bg, x, y, 0, 0, IMAGE_W, IMAGE_H, sheetW, sheetH);

        int hx = x + trackX + handleOffset();
        boolean handleHov = dragging || inTrack(mx, my);
        gfx.blit(handleHov ? handleTexH : handleTex, hx, y + trackY + handleDy, 0, 0, HANDLE_W, HANDLE_H, HANDLE_W, HANDLE_H);

        boolean lHov = inArrowL(mx, my), rHov = inArrowR(mx, my);
        gfx.blit(lHov ? arrLH : arrL, x + arrLX, y + arrY, 0, 0, ARR_W, ARR_H, ARR_W, ARR_H);
        gfx.blit(rHov ? arrRH : arrR, x + arrRX, y + arrY, 0, 0, ARR_W, ARR_H, ARR_W, ARR_H);

        if (station) {
            boolean micHov = inMicBtn(mx, my), spkHov = inSpkBtn(mx, my);
            ResourceLocation micTex = menu.isMicActive() ? (micHov ? BTN_ON_H : BTN_ON) : (micHov ? BTN_OFF_H : BTN_OFF);
            ResourceLocation spkTex = menu.isOutputActive() ? (spkHov ? BTN_ON_H : BTN_ON) : (spkHov ? BTN_OFF_H : BTN_OFF);
            gfx.blit(micTex, x + MIC_BTN_X, y + MIC_BTN_Y, 0, 0, TOG_W, TOG_H, TOG_W, TOG_H);
            gfx.blit(spkTex, x + SPK_BTN_X, y + SPK_BTN_Y, 0, 0, TOG_W, TOG_H, TOG_W, TOG_H);
        } else {
            gfx.blit(WT_BAR_EMPTY, x + WT_BAR_X, y + WT_BAR_Y, 0, 0, WT_BAR_W, WT_BAR_H, WT_BAR_W, WT_BAR_H);
            int lit = litSegments();
            if (lit > 0) {
                int w = BAR_SEGMENT_WIDTH[lit - 1];
                gfx.blit(WT_BAR_FULL, x + WT_BAR_X, y + WT_BAR_Y, 0, 0, w, WT_BAR_H, WT_BAR_W, WT_BAR_H);
            }

            boolean pwrHov = inPowerBtn(mx, my);
            ResourceLocation pwrTex = menu.isEnabled() ? (pwrHov ? BTN_ON_H : BTN_ON) : (pwrHov ? BTN_OFF_H : BTN_OFF);
            gfx.blit(pwrTex, x + WT_PWR_X, y + WT_PWR_Y, 0, 0, TOG_W, TOG_H, TOG_W, TOG_H);
        }
    }

    private int litSegments() {
        float frac = BatteryItem.chargeFraction(menu.getBatteryStack());
        if (frac <= 0F) return 0;
        return Math.max(1, Math.min(BAR_SEGMENTS, (int) (frac * BAR_SEGMENTS)));
    }

    private int handleOffset() {
        int usable = trackW - HANDLE_W;
        return Math.round(freqT() * usable);
    }

    @Override
    protected void renderLabels(GuiGraphics gfx, int mx, int my) {
        String freqStr = FrequencyUtil.format(menu.getFrequency());
        int fw = font.width(freqStr);
        int cx = freqX + Math.max(0, (freqW - fw) / 2);
        int cy = freqY + (freqH - font.lineHeight) / 2 + 1;
        gfx.drawString(font, freqStr, cx, cy, freqColor, false);

        if (station) {
            gfx.drawString(font, Component.translatable("screen.walkietalkie.mhz"),
                    MHZ_LABEL_X, MHZ_LABEL_Y, 0xFF4F3D2D, false);
            gfx.drawString(font, Component.translatable("screen.walkietalkie.frequency"),
                    FREQ_LABEL_X, FREQ_LABEL_Y, 0xFF364246, false);
        } else {
            Component heading = Component.translatable("screen.walkietalkie.configuration");
            gfx.drawString(font, heading, WT_TITLE_X - font.width(heading) / 2, WT_TITLE_Y,
                    WT_TITLE_COLOR, false);
        }
    }

    private float freqT() {
        float min = menu.getMinFrequency(), max = menu.getMaxFrequency();
        if (max <= min) return 0F;
        float t = (menu.getFrequency() - min) / (max - min);
        return Math.max(0F, Math.min(1F, t));
    }

    private float freqFromSliderX(double mx) {
        int rel = (int) Math.round(mx) - (leftPos + trackX);
        int usable = trackW - HANDLE_W;
        float t = usable > 0 ? (rel - HANDLE_W / 2F) / usable : 0F;
        t = Math.max(0F, Math.min(1F, t));
        float min = menu.getMinFrequency(), max = menu.getMaxFrequency();
        float raw = min + t * (max - min);
        return FrequencyUtil.fromDeci(FrequencyUtil.toDeci(raw));
    }

    private boolean inTrack(double mx, double my) {
        return mx >= leftPos + trackX && mx < leftPos + trackX + trackW
                && my >= topPos + trackY && my < topPos + trackY + TRACK_H;
    }

    private boolean inArrowL(double mx, double my) {
        return mx >= leftPos + arrLX && mx < leftPos + arrLX + ARR_W
                && my >= topPos + arrY  && my < topPos + arrY + ARR_H;
    }

    private boolean inArrowR(double mx, double my) {
        return mx >= leftPos + arrRX && mx < leftPos + arrRX + ARR_W
                && my >= topPos + arrY  && my < topPos + arrY + ARR_H;
    }

    private boolean inMicBtn(double mx, double my) {
        return station && mx >= leftPos + MIC_BTN_X && mx < leftPos + MIC_BTN_X + TOG_W
                && my >= topPos + MIC_BTN_Y && my < topPos + MIC_BTN_Y + TOG_H;
    }

    private boolean inSpkBtn(double mx, double my) {
        return station && mx >= leftPos + SPK_BTN_X && mx < leftPos + SPK_BTN_X + TOG_W
                && my >= topPos + SPK_BTN_Y && my < topPos + SPK_BTN_Y + TOG_H;
    }

    private boolean inPowerBtn(double mx, double my) {
        return !station && mx >= leftPos + WT_PWR_X && mx < leftPos + WT_PWR_X + TOG_W
                && my >= topPos + WT_PWR_Y && my < topPos + WT_PWR_Y + TOG_H;
    }

    private boolean inChargeBar(double mx, double my) {
        return !station && mx >= leftPos + WT_BAR_X && mx < leftPos + WT_BAR_X + WT_BAR_W
                && my >= topPos + WT_BAR_Y && my < topPos + WT_BAR_Y + WT_BAR_H;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (btn == 0) {
            if (inTrack(mx, my)) { dragging = true; sendFreq(freqFromSliderX(mx)); return true; }
            if (inArrowL(mx, my)) { nudge(-1); return true; }
            if (inArrowR(mx, my)) { nudge(+1); return true; }
            if (inMicBtn(mx, my)) { send(menu.getFrequency(), menu.isEnabled(), !menu.isMicActive(), menu.isOutputActive()); return true; }
            if (inSpkBtn(mx, my)) { send(menu.getFrequency(), menu.isEnabled(), menu.isMicActive(), !menu.isOutputActive()); return true; }
            if (inPowerBtn(mx, my)) { send(menu.getFrequency(), !menu.isEnabled(), menu.isMicActive(), menu.isOutputActive()); return true; }
        }
        return super.mouseClicked(mx, my, btn);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        if (dragging && btn == 0) {
            sendFreq(freqFromSliderX(mx));
            return true;
        }
        return super.mouseDragged(mx, my, btn, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        if (btn == 0) dragging = false;
        return super.mouseReleased(mx, my, btn);
    }

    private void nudge(int dir) {
        int deci = FrequencyUtil.toDeci(menu.getFrequency()) + dir;
        deci = Math.max(FrequencyUtil.toDeci(menu.getMinFrequency()),
               Math.min(FrequencyUtil.toDeci(menu.getMaxFrequency()), deci));
        if (deci != lastSentDeci)
            send(FrequencyUtil.fromDeci(deci), menu.isEnabled(), menu.isMicActive(), menu.isOutputActive());
    }

    private void sendFreq(float freq) {
        int deci = FrequencyUtil.toDeci(freq);
        if (deci != lastSentDeci) send(freq, menu.isEnabled(), menu.isMicActive(), menu.isOutputActive());
    }

    private void send(float freq, boolean enabled, boolean mic, boolean spk) {
        int deci = FrequencyUtil.toDeci(freq);
        lastSentDeci = deci;
        PacketDistributor.sendToServer(
                new RadioMenuUpdateC2S(menu.containerId, deci, enabled, mic, spk));
    }
}
