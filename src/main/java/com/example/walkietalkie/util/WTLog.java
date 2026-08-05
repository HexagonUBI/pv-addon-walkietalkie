package com.example.walkietalkie.util;

import com.example.walkietalkie.config.WTServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class WTLog {

    private final Logger logger;

    private WTLog(String name) {
        this.logger = LoggerFactory.getLogger(name);
    }

    public static WTLog of(String name) {
        return new WTLog(name);
    }

    public static boolean isDebugEnabled() {
        try {
            return WTServerConfig.DEBUG_LOGGING.get();
        } catch (Throwable t) {
            return false;
        }
    }

    public void info(String message, Object... args) {
        if (isDebugEnabled()) logger.info(message, args);
    }

    public void warn(String message, Object... args) {
        if (isDebugEnabled()) logger.warn(message, args);
    }

    public void error(String message, Object... args) {
        logger.error(message, args);
    }
}
