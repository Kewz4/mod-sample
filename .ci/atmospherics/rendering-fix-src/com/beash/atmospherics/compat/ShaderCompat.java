package com.beash.atmospherics.compat;

import java.lang.reflect.Method;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Optional Iris integration without a hard dependency on Iris.
 */
public final class ShaderCompat {
    private static volatile boolean resolutionAttempted;
    private static volatile Object irisApi;
    private static volatile Method isShaderPackInUse;

    private ShaderCompat() {
    }

    public static boolean isShaderPackInUse() {
        if (!FabricLoader.getInstance().isModLoaded("iris")) {
            return false;
        }

        try {
            resolveIrisApi();
            Object api = irisApi;
            Method method = isShaderPackInUse;
            return api != null && method != null && Boolean.TRUE.equals(method.invoke(api));
        } catch (Throwable ignored) {
            // Iris is optional. Falling back to the normal renderer is safer than crashing.
            return false;
        }
    }

    private static synchronized void resolveIrisApi() throws ReflectiveOperationException {
        if (resolutionAttempted) {
            return;
        }
        resolutionAttempted = true;

        ClassLoader loader = ShaderCompat.class.getClassLoader();
        Class<?> apiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi", false, loader);
        Method getInstance = apiClass.getMethod("getInstance");
        irisApi = getInstance.invoke(null);
        isShaderPackInUse = apiClass.getMethod("isShaderPackInUse");
    }
}
