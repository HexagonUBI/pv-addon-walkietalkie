package com.example.walkietalkie.client;

import com.example.walkietalkie.item.WalkieTalkieItem;
import com.example.walkietalkie.net.payload.ConfigureWalkieC2S;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * A simple, vanilla-styled options screen (like the pause-menu "Options" screens):
 * a numeric frequency field, an on/off toggle, and Done/Cancel buttons. There are no
 * item slots, so this is a plain {@link Screen} rather than a container menu.
 *
 * On "Done" it sends {@link ConfigureWalkieC2S} so the server writes the values onto the
 * actual item (the client copy of the stack is not authoritative).
 */
public class WalkieTalkieScreen extends Screen {

    private static final int FIELD_W = 200;
    private static final int FIELD_H = 20;

    private final InteractionHand hand;
    private final int initialFrequency;
    private boolean enabled;

    private EditBox frequencyField;

    public WalkieTalkieScreen(InteractionHand hand, ItemStack stack) {
        super(Component.translatable("screen.walkietalkie.title"));
        this.hand = hand;
        this.initialFrequency = WalkieTalkieItem.frequencyOf(stack);
        this.enabled = WalkieTalkieItem.isEnabled(stack);
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int y = this.height / 4 + 24;

        frequencyField = new EditBox(this.font, cx - FIELD_W / 2, y, FIELD_W, FIELD_H,
                Component.translatable("screen.walkietalkie.frequency"));
        frequencyField.setValue(Integer.toString(initialFrequency));
        frequencyField.setFilter(s -> s.matches("\\d{0,4}")); // up to 4 digits
        frequencyField.setMaxLength(4);
        addRenderableWidget(frequencyField);
        setInitialFocus(frequencyField);

        y += 36;
        addRenderableWidget(CycleButton.onOffBuilder(enabled)
                .create(cx - FIELD_W / 2, y, FIELD_W, FIELD_H,
                        Component.translatable("screen.walkietalkie.power"),
                        (button, value) -> this.enabled = value));

        y += 36;
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> save())
                .bounds(cx - FIELD_W / 2, y, FIELD_W / 2 - 4, FIELD_H)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> onClose())
                .bounds(cx + 4, y, FIELD_W / 2 - 4, FIELD_H)
                .build());
    }

    private void save() {
        int freq = parseFrequency();
        PacketDistributor.sendToServer(new ConfigureWalkieC2S(hand, freq, enabled));
        onClose();
    }

    private int parseFrequency() {
        try {
            return Math.max(0, Math.min(9999, Integer.parseInt(frequencyField.getValue())));
        } catch (NumberFormatException e) {
            return initialFrequency;
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick); // background + widgets
        graphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 4, 0xFFFFFF);
        graphics.drawString(this.font,
                Component.translatable("screen.walkietalkie.frequency"),
                this.width / 2 - FIELD_W / 2, this.height / 4 + 14, 0xA0A0A0);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
