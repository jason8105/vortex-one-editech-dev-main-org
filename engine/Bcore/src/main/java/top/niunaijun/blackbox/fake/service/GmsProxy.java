package top.niunaijun.blackbox.fake.service;

import android.content.Context;
import android.os.IBinder;

import java.lang.reflect.Method;

import black.android.os.BRServiceManager;
import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.fake.hook.BinderInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.Slog;

/**
 * GMS proxy to handle GMS-specific issues like LevelDB locks, device ID problems, and authentication.
 */
public class GmsProxy extends BinderInvocationStub {
    public static final String TAG = "GmsProxy";

    public GmsProxy() {
        super(BRServiceManager.get().getService("gms"));
    }

    @Override
    protected Object getWho() {
        // Look for both standard GMS and MicroG RE (app.revanced.android.gms) binder services
        IBinder binder = BRServiceManager.get().getService("gms");
        if (binder == null) {
            binder = BRServiceManager.get().getService("app.revanced.android.gms");
        }
        if (binder == null) {
            Slog.e(TAG, "Failed to get gms service binder");
            return null;
        }
        try {
            Class<?> stubClass = Class.forName("com.google.android.gms.common.api.internal.IGmsServiceBroker$Stub");
            Method asInterfaceMethod = stubClass.getMethod("asInterface", IBinder.class);
            Object iface = asInterfaceMethod.invoke(null, binder);
            if (iface != null) {
                Slog.d(TAG, "Successfully obtained IGmsServiceBroker interface for MicroG RE");
                return iface;
            }
        } catch (Exception e) {
            Slog.e(TAG, "Failed to get IGmsServiceBroker interface", e);
        }
        return null;
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        replaceSystemService("gms");
        replaceSystemService("app.revanced.android.gms");
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    @ProxyMethod("getService")
    public static class GetService extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                if (args != null && args.length > 0) {
                    String currentCallingPkg = top.niunaijun.blackbox.app.BActivityThread.getAppProcessName();
                    if (currentCallingPkg == null || currentCallingPkg.isEmpty()) {
                        currentCallingPkg = BlackBoxCore.getHostPkg();
                    }

                    for (int i = 0; i < args.length; i++) {
                        Object arg = args[i];
                        if (arg instanceof String) {
                            String str = (String) arg;
                            // Map incoming GMS requests to MicroG RE's package name safely
                            if (str != null && (str.equals("com.google.android.gms") || str.equals("app.revanced.android.gms") || str.equals("com.android.vending"))) {
                                args[i] = currentCallingPkg;
                                Slog.d(TAG, "GmsProxy: Mapped package argument to " + currentCallingPkg);
                            }
                        }
                    }
                }
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(TAG, "GmsProxy: Handled exception in getService: " + e.getMessage());
                return null;
            }
        }
    }
}


    // Hook getServiceBroker to handle service broker issues
    @ProxyMethod("getServiceBroker")
    public static class GetServiceBroker extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.e(TAG, "GmsProxy: Error in getServiceBroker", e);
                // Return null to prevent crashes
                return null;
            }
        }
    }

    // Hook authenticate to handle authentication issues
    @ProxyMethod("authenticate")
    public static class Authenticate extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                Slog.d(TAG, "GmsProxy: Handling authenticate call");
                Object result = method.invoke(who, args);
                if (result == null) {
                    return createMockAuthResult();
                }
                return result;
            } catch (Exception e) {
                Slog.w(TAG, "GmsProxy: Authentication error, returning success", e);
                // Return a mock successful authentication result with fallback token
                return createMockAuthResult();
            }
        }
    }

    // Hook getAccount to handle account retrieval issues
    @ProxyMethod("getAccount")
    public static class GetAccount extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                Slog.d(TAG, "GmsProxy: Handling getAccount call");
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(TAG, "GmsProxy: GetAccount error, returning null", e);
                return null;
            }
        }
    }

    // Hook getToken to handle token retrieval issues
    @ProxyMethod("getToken")
    public static class GetToken extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                Slog.d(TAG, "GmsProxy: Handling getToken call");
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(TAG, "GmsProxy: GetToken error, returning mock token", e);
                return "mock_gms_token_" + System.currentTimeMillis();
            }
        }
    }

    // Hook invalidateToken to handle token invalidation
    @ProxyMethod("invalidateToken")
    public static class InvalidateToken extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                Slog.d(TAG, "GmsProxy: Handling invalidateToken call");
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(TAG, "GmsProxy: InvalidateToken error, ignoring", e);
                return null;
            }
        }
    }

    // Hook clearToken to handle token clearing
    @ProxyMethod("clearToken")
    public static class ClearToken extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            try {
                Slog.d(TAG, "GmsProxy: Handling clearToken call");
                return method.invoke(who, args);
            } catch (Exception e) {
                Slog.w(TAG, "GmsProxy: ClearToken error, ignoring", e);
                return null;
            }
        }
    }

    // Helper method to create a mock authentication result
    private static Object createMockAuthResult() {
        try {
            Class<?> bundleClass = Class.forName("android.os.Bundle");
            Object bundle = bundleClass.newInstance();
            // Inject fallback auth token if possible or return populated bundle
            try {
                java.lang.reflect.Method putStringMethod = bundleClass.getMethod("putString", String.class, String.class);
                putStringMethod.invoke(bundle, "authToken", top.niunaijun.blackbox.core.GmsCore.FALLBACK_AUTH_TOKEN);
                java.lang.reflect.Method putBoolMethod = bundleClass.getMethod("putBoolean", String.class, boolean.class);
                putBoolMethod.invoke(bundle, "authenticated", true);
            } catch (Exception ignored) {}
            return bundle;
        } catch (Exception e) {
            Slog.w(TAG, "Failed to create mock auth result", e);
            return null;
        }
    }
}
