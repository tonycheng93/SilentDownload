package com.sky.silentdownload;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.text.TextUtils;

/**
 * Created by tonycheng on 2017/3/8.
 */

public class App extends Application {

    private static Context mContext;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();
    }

    public static String getVersionCode() {
        try {
            PackageManager packageManager = mContext.getPackageManager();
            PackageInfo info = packageManager.getPackageInfo(mContext.getPackageName(), 0);
            String version = info.versionName;
            if (!TextUtils.isEmpty(version)) {
                return version;
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }
}
