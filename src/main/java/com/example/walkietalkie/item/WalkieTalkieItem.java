package com.example.walkietalkie.item;

import com.example.walkietalkie.client.WTClientHooks;
import com.example.walkietalkie.registry.WTComponents;
import com.example.walkietalkie.voice.RadioState;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * One item, three gestures, all derived from how long right-click is held:
 *
 *   • SHIFT + right-click  -> open the config screen (frequency + settings).
 *   • quick tap            -> toggle the radio on/off.
 *   • hold (> threshold)   -> push-to-talk on the current frequency.
 *
 * The "talk" gesture only marks server-side intent + frequency. The actual microphone
 * capture is started client-side by the Plasmo Voice client bridge while the item is
 * being used (see WalkieVoiceClientAddon).
 */
public class WalkieTalkieItem extends Item {

    /** Ticks the button must be held before it counts as "talk" rather than "tap". */
    public static final int HOLD_THRESHOLD = 6; // ~0.3s
    private static final int MAX_USE = 72000;   // effectively "hold forever", like a bow

    public WalkieTalkieItem(Properties properties) {
        super(properties);
    }

    public static int frequencyOf(ItemStack stack) {
        return stack.getOrDefault(WTComponents.FREQUENCY.get(), 0);
    }

    public static boolean isEnabled(ItemStack stack) {
        return stack.getOrDefault(WTComponents.ENABLED.get(), Boolean.FALSE);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // SHIFT -> config screen, do NOT begin "using".
        if (player.isSecondaryUseActive()) {
            if (level.isClientSide) {
                // Reached only on the client; class is never loaded on a dedicated server.
                WTClientHooks.openConfigScreen(hand);
            }
            return InteractionResultHolder.success(stack);
        }

        // Otherwise begin "using"; release decides tap-vs-hold.
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return MAX_USE;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE; // don't raise the item like food/bow
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
        int elapsed = getUseDuration(stack, entity) - remainingUseDuration;

        // The instant we cross from "tap" into "talk", record server-side that this
        // player is transmitting + on which frequency. The client bridge independently
        // sees isUsingItem() and starts mic capture.
        if (elapsed == HOLD_THRESHOLD
                && !level.isClientSide
                && entity instanceof ServerPlayer sp
                && isEnabled(stack)) {
            RadioState.get(sp.server).startTransmitting(sp, frequencyOf(stack));
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (level.isClientSide || !(entity instanceof ServerPlayer sp)) {
            return;
        }

        int elapsed = getUseDuration(stack, entity) - timeLeft;
        if (elapsed < HOLD_THRESHOLD) {
            // QUICK TAP -> toggle power.
            boolean now = !isEnabled(stack);
            stack.set(WTComponents.ENABLED.get(), now);
            RadioState.get(sp.server).refreshListeners(sp.server);
            sp.displayClientMessage(
                    Component.translatable(now ? "msg.walkietalkie.on" : "msg.walkietalkie.off")
                            .withStyle(now ? ChatFormatting.GREEN : ChatFormatting.GRAY),
                    true);
        } else {
            // Was talking -> stop.
            RadioState.get(sp.server).stopTransmitting(sp);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.walkietalkie.frequency", frequencyOf(stack))
                .withStyle(ChatFormatting.AQUA));
        lines.add(Component.translatable(isEnabled(stack) ? "tooltip.walkietalkie.on" : "tooltip.walkietalkie.off")
                .withStyle(isEnabled(stack) ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY));
    }
}
