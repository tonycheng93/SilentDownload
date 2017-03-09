package com.sky.silentdownload.silentupgrade.model.impl;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import com.sky.silentdownload.silentupgrade.model.IGetLocalAppInfo;
import com.sky.silentdownload.silentupgrade.model.data.LocalAppInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by BaoCheng on 2017/3/9.
 */

public class GetLocalAppInfoImpl implements IGetLocalAppInfo {

    private Context mContext;

    public GetLocalAppInfoImpl(Context context) {
        mContext = context;
    }

    @Override
    public List<LocalAppInfo> getLocalAppInfo() {
        PackageManager pm = mContext.getPackageManager();
        List<LocalAppInfo> localAppInfoList = new ArrayList<>();
        List<PackageInfo> packageInfoList = pm.getInstalledPackages(0);
        for (int i = 0; i < packageInfoList.size(); i++) {
            PackageInfo packageInfo = packageInfoList.get(i);
            LocalAppInfo localAppInfo = new LocalAppInfo();
            localAppInfo.packageName = packageInfo.packageName;
            localAppInfo.appVersionCode = String.valueOf(packageInfo.versionCode);
            localAppInfoList.add(localAppInfo);
        }
        return localAppInfoList;
    }
}
