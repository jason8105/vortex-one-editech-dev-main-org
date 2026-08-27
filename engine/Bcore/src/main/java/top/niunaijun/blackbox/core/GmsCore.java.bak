package top.niunaijun.blackbox.core;

import android.content.pm.PackageManager;
import android.util.Log;

import java.io.File;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.core.env.BEnvironment;
import top.niunaijun.blackbox.entity.pm.InstallResult;
import top.niunaijun.blackbox.utils.FileUtils;

/**
 * Modified for MicroG Asset Installation
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

    private static void uninstallPackages(Set<String> list, int userId) {
        BlackBoxCore blackBoxCore = BlackBoxCore.get();
        for (String packageName : list) {
            blackBoxCore.uninstallPackageAsUser(packageName, userId);
        }
    }

    // Option 1: Install MicroG and FakeStore directly from Assets instead of cloning host
    public static InstallResult installGApps(int userId) {
        InstallResult gmsResult = installMicroGFromAsset("microg.apk", GMS_PKG, userId);
        InstallResult vendingResult = installMicroGFromAsset("vending.apk", VENDING_PKG, userId);
        
        if (!gmsResult.success || !vendingResult.success) {
            Log.w(TAG, "Failed to install MicroG packages from assets.");
            InstallResult err = new InstallResult();
            err.success = false;
            err.msg = "MicroG Installation Failed";
            return err;
        }
        return gmsResult;
    }

    private static InstallResult installMicroGFromAsset(String assetName, String packageName, int userId) {
        BlackBoxCore core = BlackBoxCore.get();
        if (core.isInstalled(packageName, userId)) {
            InstallResult result = new InstallResult();
            result.success = true;
            result.packageName = packageName;
            return result;
        }
        try {
            File destFile = new File(BEnvironment.getCacheDir(), assetName);
            if (!destFile.exists()) {
                InputStream is = BlackBoxCore.getContext().getAssets().open(assetName);
                FileUtils.copyFile(is, destFile);
            }
            // Use the proper 2-argument method (File, userId)
            return core.installPackageAsUser(destFile, userId);
        } catch (Exception e) {
            Log.e(TAG, "Asset extraction failed for " + assetName, e);
            InstallResult err = new InstallResult();
            err.success = false;
            err.msg = e.getMessage();
            return err;
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
