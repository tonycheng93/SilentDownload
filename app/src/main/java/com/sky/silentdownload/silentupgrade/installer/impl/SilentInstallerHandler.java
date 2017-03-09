//package com.sky.silentdownload.silentupgrade.installer.impl;
//
//import android.content.Context;
//import android.content.pm.PackageInfo;
//import android.content.pm.PackageManager;
//import android.os.Environment;
//import android.text.TextUtils;
//
//import com.sky.silentdownload.silentupgrade.installer.IInstallerListener;
//
//
///**
// * Created by lu on 17-2-18.
// */
//
//public class SilentInstallerHandler {
//    private IInstallerListener listener;
//    private Context mContext;
//
//    public SilentInstallerHandler(Context context) {
//        mContext = context;
//    }
//
//    public void setInstallerListener(IInstallerListener listener) {
//        this.listener = listener;
//    }
//
//    public void install(String archive) {
//        PackageInfo pkgInfo = null;
//        try {
//            PackageManager pm = mContext.getPackageManager();
//            pkgInfo = pm.getPackageArchiveInfo(archive, PackageManager.GET_ACTIVITIES);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        if (pkgInfo == null) {
//            if (listener != null)
//                listener.onInstallListener(IInstallerListener.INSTALL_ERROR,"","");
//        } else {
//            int preferlocation = Android.getInstallLocation(archive);
//            String shell;
//            switch (preferlocation) {
//                case Android.INSTALL_LOCATION_INTERNAL_ONLY: {
//                    StringBuilder command = new StringBuilder().append("pm install -r -f ").append(
//                            archive);
//                    shell = command.toString();
//                    break;
//                }
//                case Android.INSTALL_LOCATION_PREFER_EXTERNAL: {
//                    StringBuilder command;
//                    if (Android.needInstallSystem(archive)) {
//                        command = new StringBuilder().append("pm install -r -f ").append(
//                                archive);
//                        shell = command.toString();
//                    } else {
//                        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
//                            command = new StringBuilder().append("pm install -r -s ").append(
//                                    archive);
//                            shell = command.toString();
//                        } else {
//                            if (listener != null)
//                                listener.onInstallListener(IInstallerListener.INSTALL_ERROR, pkgInfo.packageName, IInstallerListener.RESULT_FAILED_SYSTEMUID_TO_SDCARD);
//                            return;
//                        }
//                    }
//                    break;
//                }
//                case Android.INSTALL_LOCATION_AUTO:
//                default: {
//                    StringBuilder command;
//                    if (Android.needInstallSystem( archive))
//                        command = new StringBuilder().append("pm install -r -f ").append(
//                                archive);
//                    else
//                        command = new StringBuilder().append("pm install -r ").append(
//                                archive);
//                    shell = command.toString();
//                    break;
//                }
//            }
//            int result;
//            String successMessage = null;
//            String errorMessage = null;
//            try {
//                ShellUtils.CommandResult commandResult = ShellUtils
//                        .execCommand(shell, true, true);
//                result = commandResult.result;
//                successMessage = commandResult.successMsg;
//                errorMessage = commandResult.errorMsg;
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//            if (!TextUtils.isEmpty(successMessage)) {
//                if (successMessage.toLowerCase().contains("success")) {
//                    if (listener != null)
//                        listener.onInstallListener(IInstallerListener.INSTALL_SUCCESS, pkgInfo.packageName, "");
//                }
//            } else if (!TextUtils.isEmpty(errorMessage)) {
//                String resultString = errorMessage;
//                if (errorMessage.toLowerCase().contains(IInstallerListener.RESULT_FAILED_INSUFFICIENT_STORAGE.toLowerCase())) {
//                    resultString = IInstallerListener.RESULT_FAILED_INSUFFICIENT_STORAGE;
//                }
//                if (errorMessage.toLowerCase().contains(IInstallerListener.RESULT_FAILED_INVALID_APK.toLowerCase())) {
//                    resultString = IInstallerListener.RESULT_FAILED_INVALID_APK;
//                }
//                if (errorMessage.toLowerCase().contains(IInstallerListener.RESULT_FAILED_INVALID_URI.toLowerCase())) {
//                    resultString = IInstallerListener.RESULT_FAILED_INVALID_URI;
//                }
//                if (listener != null)
//                    listener.onInstallListener(IInstallerListener.INSTALL_ERROR, pkgInfo.packageName, resultString);
//            }
//        }
//    }
//}
