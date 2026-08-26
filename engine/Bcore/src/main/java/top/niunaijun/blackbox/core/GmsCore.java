package top.niunaijun.blackbox.core;

import android.content.pm.PackageManager;

import java.util.HashSet;
import java.util.Set;
import android.util.Log;

import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.entity.pm.InstallResult;

/**
 * updated by alex5402 on 4/9/21.
 * * ∧＿∧
 * (`･ω･∥
 * 丶　つ０
 * しーＪ
 * 
 */
public class GmsCore {
    private static final String TAG = "GmsCore";

    private static final HashSet<String> GOOGLE_APP = new HashSet<>();
    private static final HashSet<String> GOOGLE_SERVICE = new HashSet<>();
    public static final String GMS_PKG = "com.google.android.gms";
    public static final String GSF_PKG = "com.google.android.gsf";
    public static final String VENDING_PKG = "com.android.vending";
    public static final String FALLBACK_AUTH_TOKEN = "fallback_gms_token_" + System.currentTimeMillis();

    static {
        GOOGLE_APP.add(VENDING_PKG);
        GOOGLE_APP.add("com.google.android.play.games");
        GOOGLE_APP.add("com.google.android.wearable.app");
        GOOGLE_APP.add("com.google.android.wearable.app.cn");

        // GMS must install at first
        GOOGLE_SERVICE.add(GMS_PKG);
        GOOGLE_SERVICE.add(GSF_PKG);
        GOOGLE_SERVICE.add("com.google.android.gsf.login");
        GOOGLE_SERVICE.add("com.google.android.backuptransport");
        GOOGLE_SERVICE.add("com.google.android.backup");
        GOOGLE_SERVICE.add("com.google.android.configupdater");
        GOOGLE_SERVICE.add("com.google.android.syncadapters.contacts");
        GOOGLE_SERVICE.add("com.google.android.feedback");
        GOOGLE_SERVICE.add("com.google.android.onetimeinitializer");
        GOOGLE_SERVICE.add("com.google.android.partnersetup");
        GOOGLE_SERVICE.add("com.google.android.setupwizard");
        GOOGLE_SERVICE.add("com.google.android.syncadapters.calendar");
    }

    public static boolean isGoogleService(String packageName) {
        return GOOGLE_SERVICE.contains(packageName);
    }

    public static boolean isGoogleAppOrService(String str) {
        return GOOGLE_APP.contains(str) || GOOGLE_SERVICE.contains(str);
    }

    private static InstallResult installPackages(Set<String> list, int userId) {
        BlackBoxCore blackBoxCore = BlackBoxCore.get();
        for (String packageName : list) {
            if (blackBoxCore.isInstalled(packageName, userId)) {
                continue;
            }
            try {
                BlackBoxCore.getContext().getPackageManager().getApplicationInfo(packageName, 0);
            } catch (PackageManager.NameNotFoundException e) {
                // Ignore
                continue;
            }
            InstallResult installResult = blackBoxCore.installPackageAsUser(packageName, userId);
            if (!installResult.success) {
                return installResult;
            }
        }
        return new InstallResult();
    }

    private static void uninstallPackages(Set<String> list, int userId) {
        BlackBoxCore blackBoxCore = BlackBoxCore.get();
        for (String packageName : list) {
            blackBoxCore.uninstallPackageAsUser(packageName, userId);
        }
    }

    public static InstallResult installGApps(int userId) {
        // Option 1: Install MicroG and FakeStore directly from Assets instead of cloning host
        InstallResult gmsResult = installMicroGFromAsset("microg.apk", GMS_PKG, userId);
        InstallResult vendingResult = installMicroGFromAsset("vending.apk", VENDING_PKG, userId);
        
        if (!gmsResult.success || !vendingResult.success) {
            Log.w(TAG, "Failed to install MicroG packages from assets.");
            return new InstallResult().installError("MicroG Installation Failed");
        }
        return gmsResult;
    }

    private static InstallResult installMicroGFromAsset(String assetName, String packageName, int userId) {
        BlackBoxCore core = BlackBoxCore.get();
        if (core.isInstalled(packageName, userId)) {
            return new InstallResult().installSuccess(packageName);
        }
        try {
            java.io.File destFile = new java.io.File(top.niunaijun.blackbox.core.env.BEnvironment.getCacheDir(), assetName);
            if (!destFile.exists()) {
                java.io.InputStream is = BlackBoxCore.getContext().getAssets().open(assetName);
                top.niunaijun.blackbox.utils.FileUtils.copyFile(is, destFile);
            }
            top.niunaijun.blackbox.entity.pm.InstallOption option = top.niunaijun.blackbox.entity.pm.InstallOption.installBySystem();
            return core.installPackageAsUser(destFile.getAbsolutePath(), option, userId);
        } catch (Exception e) {
            Log.e(TAG, "Asset extraction failed for " + assetName, e);
            return new InstallResult().installError(e.getMessage());
        }
    }


    public static void uninstallGApps(int userId) {
        uninstallPackages(GOOGLE_SERVICE, userId);
        uninstallPackages(GOOGLE_APP, userId);
    }

    public static void remove(String packageName) {
        GOOGLE_SERVICE.remove(packageName);
        GOOGLE_APP.remove(packageName);
    }


    public static boolean isSupportGms() {
        try {
            BlackBoxCore.getPackageManager().getPackageInfo(GMS_PKG, 0);
            return true;
        } catch (PackageManager.NameNotFoundException ignored) {
        }
        return false;
    }

    public static boolean isInstalledGoogleService(int userId) {
        return BlackBoxCore.get().isInstalled(GMS_PKG, userId);
    }
}