package com.example.walkietalkie.compat;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import com.example.walkietalkie.util.WTLog;

import java.lang.reflect.Method;

public final class SableBridge {

    private static final WTLog LOGGER = WTLog.of("WalkieTalkie/Sable");

    private static final String CONTAINER_CLASS = "dev.ryanhcode.sable.api.sublevel.SubLevelContainer";

    private static boolean resolved;
    private static boolean available;

    private static Method getContainer;
    private static Method getPlot;
    private static Method getSubLevel;
    private static Method logicalPose;
    private static Method transformPosition;

    private SableBridge() {}

    private static synchronized void resolve() {
        if (resolved) return;
        resolved = true;
        try {
            Class<?> containerClass = Class.forName(CONTAINER_CLASS);
            getContainer = containerClass.getMethod("getContainer", Level.class);
            getPlot = containerClass.getMethod("getPlot", ChunkPos.class);

            Class<?> plotClass = getPlot.getReturnType();
            getSubLevel = plotClass.getMethod("getSubLevel");

            Class<?> subLevelClass = getSubLevel.getReturnType();
            logicalPose = subLevelClass.getMethod("logicalPose");

            Class<?> poseClass = logicalPose.getReturnType();
            transformPosition = poseClass.getMethod("transformPosition", Vec3.class);

            available = true;
            LOGGER.info("Sable detected - Radio Stations on sub-levels will use their real world position");
        } catch (ClassNotFoundException e) {
            LOGGER.info("Sable not installed - sub-level support disabled");
        } catch (Exception e) {
            LOGGER.warn("Sable found but its API did not match - sub-level support disabled", e);
        }
    }

    public static boolean isAvailable() {
        resolve();
        return available;
    }

    @Nullable
    public static Vec3 toWorldPosition(Level level, BlockPos pos) {
        resolve();
        if (!available || level == null) return null;
        try {
            Object container = getContainer.invoke(null, level);
            if (container == null) return null;

            Object plot = getPlot.invoke(container, new ChunkPos(pos));
            if (plot == null) return null;

            Object subLevel = getSubLevel.invoke(plot);
            if (subLevel == null) return null;

            Object pose = logicalPose.invoke(subLevel);
            if (pose == null) return null;

            Object result = transformPosition.invoke(pose,
                    new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5));
            return result instanceof Vec3 vec ? vec : null;
        } catch (Exception e) {
            available = false;
            LOGGER.warn("Sable sub-level lookup failed - disabling sub-level support", e);
            return null;
        }
    }

    public static Vec3 resolvePosition(Level level, BlockPos pos) {
        Vec3 world = toWorldPosition(level, pos);
        return world != null ? world : new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
    }
}
