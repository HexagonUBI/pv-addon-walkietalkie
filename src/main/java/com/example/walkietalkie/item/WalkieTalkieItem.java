package com.example.walkietalkie.item;

import com.example.walkietalkie.menu.ItemRadioSource;
import com.example.walkietalkie.menu.RadioMenu;
import com.example.walkietalkie.net.payload.StaticStateS2C;
import com.example.walkietalkie.net.payload.ToggleWalkieC2S;
import com.example.walkietalkie.registry.WTComponents;
import com.example.walkietalkie.registry.WTSounds;
import com.example.walkietalkie.util.FrequencyUtil;
import com.example.walkietalkie.voice.RadioState;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.UUID;

public class WalkieTalkieItem extends Item {

    public static final int HOLD_THRESHOLD = 6;
    private static final int MAX_USE = 72000;

    private static final float DEFAULT_FREQUENCY = 100.0F;

    public WalkieTalkieItem(Properties properties) {
        super(properties);
    }

    public static float frequencyOf(ItemStack stack) {
        return FrequencyUtil.clamp(stack.getOrDefault(WTComponents.FREQUENCY.get(), DEFAULT_FREQUENCY));
    }

    public static boolean isEnabled(ItemStack stack) {
        return stack.getOrDefault(WTComponents.ENABLED.get(), Boolean.FALSE);
    }

    public static void togglePower(ItemStack stack, ServerPlayer sp) {
        setEnabled(stack, sp, !isEnabled(stack));
    }

    public static void setEnabled(ItemStack stack, ServerPlayer sp, boolean now) {
        boolean was = isEnabled(stack);
        stack.set(WTComponents.ENABLED.get(), now);
        if (was == now) return;

        RadioState.get(sp.server).refreshListeners(sp.server);
        sp.displayClientMessage(
                Component.translatable(now ? "msg.walkietalkie.on" : "msg.walkietalkie.off")
                        .withStyle(now ? ChatFormatting.GREEN : ChatFormatting.GRAY),
                true);

        SoundEvent click = (now ? WTSounds.TOGGLE_ON : WTSounds.TOGGLE_OFF).get();
        float volume = RadioState.get(sp.server).sfxVolumeOf(sp.getUUID());
        sp.level().playSound(null, sp.getX(), sp.getY(), sp.getZ(), click, SoundSource.PLAYERS, volume, 1.0F);
    }

    private static void notifyFrequency(MinecraftServer server, int deciFrequency, SoundEvent sound) {
        RadioState state = RadioState.get(server);
        for (UUID uuid : state.listenersFor(deciFrequency)) {
            ServerPlayer listener = server.getPlayerList().getPlayer(uuid);
            if (listener != null) {
                listener.playNotifySound(sound, SoundSource.PLAYERS, state.sfxVolumeOf(uuid), 1.0F);
            }
        }
    }

    public static void broadcastStaticState(MinecraftServer server, int deciFrequency, boolean active) {
        StaticStateS2C packet = new StaticStateS2C(deciFrequency, active);
        RadioState state = RadioState.get(server);
        for (UUID uuid : state.listenersFor(deciFrequency)) {
            ServerPlayer listener = server.getPlayerList().getPlayer(uuid);
            if (listener != null) {
                PacketDistributor.sendToPlayer(listener, packet);
            }
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player.isSecondaryUseActive()) {
            if (!level.isClientSide && player instanceof ServerPlayer sp) {
                sp.openMenu(new SimpleMenuProvider(
                        (containerId, inv, p) -> RadioMenu.forWalkie(containerId, inv, new ItemRadioSource(sp, hand)),
                        Component.translatable("screen.walkietalkie.title")
                ));
            }
            return InteractionResultHolder.success(stack);
        }

        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return MAX_USE;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
        int elapsed = getUseDuration(stack, entity) - remainingUseDuration;

        if (elapsed == HOLD_THRESHOLD
                && !level.isClientSide
                && entity instanceof ServerPlayer sp
                && isEnabled(stack)) {
            int deciFreq = FrequencyUtil.toDeci(frequencyOf(stack));
            RadioState.get(sp.server).startTransmitting(sp, deciFreq);
            notifyFrequency(sp.server, deciFreq, WTSounds.TALK_START.get());
            broadcastStaticState(sp.server, deciFreq, true);
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (level.isClientSide || !(entity instanceof ServerPlayer sp)) {
            return;
        }

        int elapsed = getUseDuration(stack, entity) - timeLeft;
        if (elapsed < HOLD_THRESHOLD) {
            togglePower(stack, sp);
        } else {
            int deciFreq = FrequencyUtil.toDeci(frequencyOf(stack));
            RadioState.get(sp.server).stopTransmitting(sp);
            notifyFrequency(sp.server, deciFreq, WTSounds.TALK_STOP.get());
            broadcastStaticState(sp.server, deciFreq, false);
        }
    }

    @Override
    public boolean overrideOtherStackedOnMe(
            ItemStack stack,
            ItemStack carried,
            Slot slot,
            ClickAction action,
            Player player,
            SlotAccess carriedSlotAccess
    ) {
        if (action != ClickAction.SECONDARY || !carried.isEmpty() || !slot.allowModification(player)) {
            return false;
        }
        if (player instanceof ServerPlayer sp) {
            togglePower(stack, sp);
        } else if (player.isCreative() && isCreativeInventoryOpen()) {
            stack.set(WTComponents.ENABLED.get(), !isEnabled(stack));
            PacketDistributor.sendToServer(new ToggleWalkieC2S(slot.getContainerSlot()));
        }
        return true;
    }

    private static boolean isCreativeInventoryOpen() {
        if (FMLEnvironment.dist != Dist.CLIENT) return false;
        return ClientScreenCheck.isCreativeInventoryOpen();
    }

    private static final class ClientScreenCheck {
        private static boolean isCreativeInventoryOpen() {
            return net.minecraft.client.Minecraft.getInstance().screen
                    instanceof net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.walkietalkie.frequency", FrequencyUtil.format(frequencyOf(stack)))
                .withStyle(ChatFormatting.AQUA));
        lines.add(Component.translatable(isEnabled(stack) ? "tooltip.walkietalkie.on" : "tooltip.walkietalkie.off")
                .withStyle(isEnabled(stack) ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY));
    }
}
