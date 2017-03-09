package com.sky.silentdownload.silentupgrade.downloader.data;

/**
 * Created by BaoCheng on 2017/2/22.
 */

public class ApkDownloadInfo extends DownloadInfo {
    private static final String APK_KEY = "apk_packageName";

    public static final String getApkFromDownloadInfo(DownloadInfo info) {
        return info.getParam(APK_KEY);
    }


    public void setApk(String apk) {
        putParam(APK_KEY, apk);
    }

    public String getApk() {
        return getParam(APK_KEY);
    }
}
