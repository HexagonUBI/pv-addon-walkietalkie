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

public class RadioScreen extends AbstractContainerScreen<RadioMenu> {

    private static final ResourceLocation BTN_ON     = id("textures/gui/on.png");
    private static final ResourceLocation BTN_OFF    = id("textures/gui/off.png");
    private static final ResourceLocation BTN_ON_H   = id("textures/gui/on_s.png");
    private static final ResourceLocation BTN_OFF_H  = id("textures/gui/off_s.png");

    private static final ResourceLocation RS_BG      = id("textures/gui/radiostation_container.png");
    private static final ResourceLocation RS_HANDLE  = id("textures/gui/radiostation_slider_red.png");
    private static final ResourceLocation RS_ARR_L   = id("textures/gui/radio_precise_arrow_l.png");
    private static final ResourceLocation RS_ARR_R   = id("textures/gui/radio_precise_arrow_r.png");
    private static final ResourceLocation RS_ARR_LH  = id("textures/gui/radio_precise_arrow_l_h.png");
    private static final ResourceLocation RS_ARR_RH  = id("textures/gui/radio_precise_arrow_r_h.png");

    private static final ResourceLocation WT_BG      = id("textures/gui/walkie_talkie/container.png");
    private static final ResourceLocation WT_HANDLE  = id("textures/gui/walkie_talkie/slider.png");
    private static final ResourceLocation WT_ARR_L   = id("textures/gui/walkie_talkie/arrow_l.png");
    private static final ResourceLocation WT_ARR_R   = id("textures/gui/walkie_talkie/arrow_r.png");
    private static final ResourceLocation WT_ARR_LH  = id("textures/gui/walkie_talkie/arrow_l_h.png");
    private static final ResourceLocation WT_ARR_RH  = id("textures/gui/walkie_talkie/arrow_r_h.png");

    private static final int TRACK_X = 63, TRACK_Y = 19, TRACK_W = 104, TRACK_H = 16;
    private static final int HANDLE_W = 5, HANDLE_H = 16;
    private static final int ARR_L_X = 15, ARR_R_X = 49, ARR_Y = 23, ARR_W = 5, ARR_H = 8;
    private static final int TOG_W = 16, TOG_H = 16;
    private static final int MIC_BTN_X = 44, MIC_BTN_Y = 36;
    private static final int SPK_BTN_X = 44, SPK_BTN_Y = 54;

    private static final int FREQ_BOX_X = 20, FREQ_BOX_Y = 21, FREQ_BOX_W = 29, FREQ_BOX_H = 12;
    private static final int MHZ_LABEL_X = 8, MHZ_LABEL_Y = 8;
    private static final int FREQ_LABEL_X = 65, FREQ_LABEL_Y = 8;

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(WalkieTalkieMod.MOD_ID, path);
    }

    private boolean dragging = false;
    private int lastSentDeci = Integer.MIN_VALUE;
    private final boolean station;

    private final ResourceLocation bg, handleTex, arrL, arrR, arrLH, arrRH;

    public RadioScreen(RadioMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.station = menu.hasMicSlot();
        this.imageWidth  = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = -100;

        this.bg       = station ? RS_BG      : WT_BG;
        this.handleTex= station ? RS_HANDLE  : WT_HANDLE;
        this.arrL     = station ? RS_ARR_L   : WT_ARR_L;
        this.arrR     = station ? RS_ARR_R   : WT_ARR_R;
        this.arrLH    = station ? RS_ARR_LH  : WT_ARR_LH;
        this.arrRH    = station ? RS_ARR_RH  : WT_ARR_RH;
    }

    @Override
    protected void renderBg(GuiGraphics gfx, float dt, int mx, int my) {
        int x = leftPos, y = topPos;

        gfx.blit(bg, x, y, 0, 0, 176, 166, 176, 166);

        int hx = x + TRACK_X + handleOffset();
        gfx.blit(handleTex, hx, y + TRACK_Y, 0, 0, HANDLE_W, HANDLE_H, HANDLE_W, HANDLE_H);

        boolean lHov = inArrowL(mx, my), rHov = inArrowR(mx, my);
        gfx.blit(lHov ? arrLH : arrL, x + ARR_L_X, y + ARR_Y, 0, 0, ARR_W, ARR_H, ARR_W, ARR_H);
        gfx.blit(rHov ? arrRH : arrR, x + ARR_R_X, y + ARR_Y, 0, 0, ARR_W, ARR_H, ARR_W, ARR_H);

        if (station) {
            boolean micHov = inMicBtn(mx, my), spkHov = inSpkBtn(mx, my);
            ResourceLocation micTex = menu.isMicActive() ? (micHov ? BTN_ON_H : BTN_ON) : (micHov ? BTN_OFF_H : BTN_OFF);
            ResourceLocation spkTex = menu.isOutputActive() ? (spkHov ? BTN_ON_H : BTN_ON) : (spkHov ? BTN_OFF_H : BTN_OFF);
            gfx.blit(micTex, x + MIC_BTN_X, y + MIC_BTN_Y, 0, 0, TOG_W, TOG_H, TOG_W, TOG_H);
            gfx.blit(spkTex, x + SPK_BTN_X, y + SPK_BTN_Y, 0, 0, TOG_W, TOG_H, TOG_W, TOG_H);
        }
    }

    private int handleOffset() {
        int usable = TRACK_W - HANDLE_W;
        return Math.round(freqT() * usable);
    }

    @Override
    protected void renderLabels(GuiGraphics gfx, int mx, int my) {
        String freqStr = FrequencyUtil.format(menu.getFrequency());
        int fw = font.width(freqStr);
        int cx = FREQ_BOX_X + Math.max(0, (FREQ_BOX_W - fw) / 2);
        int cy = FREQ_BOX_Y + (FREQ_BOX_H - font.lineHeight) / 2 + 1;
        gfx.drawString(font, freqStr, cx, cy, 0xFF5E5B4A, false);

        gfx.drawString(font, Component.translatable("screen.walkietalkie.mhz"),
                MHZ_LABEL_X, MHZ_LABEL_Y, 0xFF4F3D2D, false);
        gfx.drawString(font, Component.translatable("screen.walkietalkie.frequency"),
                FREQ_LABEL_X, FREQ_LABEL_Y, 0xFF364246, false);
    }

    private float freqT() {
        float min = FrequencyUtil.min(), max = FrequencyUtil.max();
        if (max <= min) return 0F;
        float t = (menu.getFrequency() - min) / (max - min);
        return Math.max(0F, Math.min(1F, t));
    }

    private float freqFromSliderX(double mx) {
        int rel = (int) Math.round(mx) - (leftPos + TRACK_X);
        int usable = TRACK_W - HANDLE_W;
        float t = usable > 0 ? (rel - HANDLE_W / 2F) / usable : 0F;
        t = Math.max(0F, Math.min(1F, t));
        float raw = FrequencyUtil.min() + t * (FrequencyUtil.max() - FrequencyUtil.min());
        return FrequencyUtil.fromDeci(FrequencyUtil.toDeci(raw));
    }

    private boolean inTrack(double mx, double my) {
        return mx >= leftPos + TRACK_X && mx < leftPos + TRACK_X + TRACK_W
                && my >= topPos + TRACK_Y && my < topPos + TRACK_Y + TRACK_H;
    }

    private boolean inArrowL(double mx, double my) {
        return mx >= leftPos + ARR_L_X && mx < leftPos + ARR_L_X + ARR_W
                && my >= topPos + ARR_Y  && my < topPos + ARR_Y + ARR_H;
    }

    private boolean inArrowR(double mx, double my) {
        return mx >= leftPos + ARR_R_X && mx < leftPos + ARR_R_X + ARR_W
                && my >= topPos + ARR_Y  && my < topPos + ARR_Y + ARR_H;
    }

    private boolean inMicBtn(double mx, double my) {
        return station && mx >= leftPos + MIC_BTN_X && mx < leftPos + MIC_BTN_X + TOG_W
                && my >= topPos + MIC_BTN_Y && my < topPos + MIC_BTN_Y + TOG_H;
    }

    private boolean inSpkBtn(double mx, double my) {
        return station && mx >= leftPos + SPK_BTN_X && mx < leftPos + SPK_BTN_X + TOG_W
                && my >= topPos + SPK_BTN_Y && my < topPos + SPK_BTN_Y + TOG_H;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (btn == 0) {
            if (inTrack(mx, my)) { dragging = true; sendFreq(freqFromSliderX(mx)); return true; }
            if (inArrowL(mx, my)) { nudge(-1); return true; }
            if (inArrowR(mx, my)) { nudge(+1); return true; }
            if (inMicBtn(mx, my)) { send(menu.getFrequency(), menu.isEnabled(), !menu.isMicActive(), menu.isOutputActive()); return true; }
            if (inSpkBtn(mx, my)) { send(menu.getFrequency(), menu.isEnabled(), menu.isMicActive(), !menu.isOutputActive()); return true; }
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
        deci = Math.max(FrequencyUtil.toDeci(FrequencyUtil.min()),
               Math.min(FrequencyUtil.toDeci(FrequencyUtil.max()), deci));
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
