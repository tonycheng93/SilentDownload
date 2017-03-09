package com.sky.silentdownload.silentupgrade.model.impl;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.alibaba.fastjson.JSONObject;
import com.jkyeo.basicparamsinterceptor.BasicParamsInterceptor;
import com.sky.silentdownload.silentupgrade.IGetSilentUpgradeData;
import com.sky.silentdownload.silentupgrade.downloader.data.ApkDownloadInfo;
import com.sky.silentdownload.silentupgrade.downloader.data.DownloadInfo;
import com.sky.silentdownload.silentupgrade.model.IGetLocalAppInfo;
import com.sky.silentdownload.silentupgrade.model.data.ApkInfo;
import com.sky.silentdownload.silentupgrade.model.data.AppUpdateInfo;
import com.sky.silentdownload.silentupgrade.model.data.BaseUpdateData;
import com.sky.silentdownload.silentupgrade.model.data.LocalAppInfo;

import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by BaoCheng on 2017/3/9.
 */

public class GetSilentUpgradeDataImpl implements IGetSilentUpgradeData {

    private static final String TAG = "GetSilentUpgradeDataImp";

    private IGetLocalAppInfo mGetLocalAppInfo;

    private static Context mContext;


    private static final String REQUEST_PARAM = "pkgInfo";
    private static final String BASE_URL = "http://172.20.132.181:8081/appstorev3/appVersionWithPolicy.html";
    private static final String REQUEST_APK_PARAM = "pkg";
    private static final String BASE_APK_URl = "http://172.20.132.181:8080/TCAppstore_INTERFACE/gamecenterv3/download.html";

    private GetSilentUpgradeDataImpl(Context context) {
        mContext = context;
        mGetLocalAppInfo = new GetLocalAppInfoImpl(context);
    }

    private static class SingletonHolder {
        private static final GetSilentUpgradeDataImpl instance = new GetSilentUpgradeDataImpl(mContext);
    }

    public static final GetSilentUpgradeDataImpl getInstance(Context context) {
        return SingletonHolder.instance;
    }

    @Override
    public List<DownloadInfo> getSilentUpgradeData() {
        List<DownloadInfo> downloadInfoList = new ArrayList<>();
        try {
            ApkDownloadInfo apkDownloadInfo = new ApkDownloadInfo();
            final List<AppUpdateInfo> serverAppInfo = loadAppUpdateInfo();
            if (serverAppInfo != null && serverAppInfo.size() > 0) {
                for (int i = 0; i < serverAppInfo.size(); i++) {
                    String pkgName = serverAppInfo.get(i).getAppPackage();
                    if (!TextUtils.isEmpty(pkgName)) {
                        ApkInfo apkInfo = loadApkInfo(pkgName);
                        apkDownloadInfo.url = apkInfo.getDownload();
                        apkDownloadInfo.md5 = apkInfo.getMd5();
                        apkDownloadInfo.setApk(pkgName);
                        downloadInfoList.add(apkDownloadInfo);
                    }
                }
            }
            return downloadInfoList;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取升级应用信息
     *
     * @return List<AppUpdateInfo>
     */
    public List<AppUpdateInfo> loadAppUpdateInfo() {
        List<AppUpdateInfo> appUpdateInfoList = null;
        StringBuilder builder = null;
        List<LocalAppInfo> localAppInfo = mGetLocalAppInfo.getLocalAppInfo();
        if (localAppInfo != null && localAppInfo.size() > 0) {
            builder = new StringBuilder();
            for (LocalAppInfo info : localAppInfo) {
                builder.append(info.packageName)
                        .append(":")
                        .append(info.appVersionCode)
                        .append(";");
            }
        }

        try {
            //获取静默升级数据
            BasicParamsInterceptor paramsInterceptor = new BasicParamsInterceptor.Builder()
                    .addParam(REQUEST_PARAM, "com.coocaa.mall:17238;")
                    .build();
            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(paramsInterceptor)
                    .build();
            Request request = new Request.Builder()
                    .url(BASE_URL)
                    .build();
            final Response response = client.newCall(request).execute();


            //解析成对象
            BaseUpdateData baseUpdateData = JSONObject.parseObject(response.body().byteStream(), BaseUpdateData.class);
            if (baseUpdateData.getRet() == 0) {//获取数据成功
                List<AppUpdateInfo> infos = baseUpdateData.getData();
                if (infos != null && infos.size() > 0) {
                    appUpdateInfoList = filterAppInfo(localAppInfo, infos);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return appUpdateInfoList;
    }

    public ApkInfo loadApkInfo(String pkgName) {
        ApkInfo apkInfo = null;
        try {
            BasicParamsInterceptor paramsInterceptor = new BasicParamsInterceptor.Builder()
                    .addParam(REQUEST_APK_PARAM, pkgName)
                    .build();
            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(paramsInterceptor)
                    .build();
            Request request = new Request.Builder()
                    .url(BASE_APK_URl)
                    .build();
            final Response response = client.newCall(request).execute();
            Log.i(TAG, "loadApkInfo: response: " + response.body().toString());

            apkInfo = JSONObject.parseObject(response.body().byteStream(), ApkInfo.class);
            if (apkInfo == null) return null;
            if (apkInfo.getRet() == 0) {
                return apkInfo;
            } else {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return apkInfo;
    }

    /**
     * 过滤需要静默升级的App信息
     */
    public List<AppUpdateInfo> filterAppInfo(List<LocalAppInfo> localAppInfoList,
                                             List<AppUpdateInfo> appUpdateInfoList) {
        List<AppUpdateInfo> infos = new ArrayList<>();

        if (localAppInfoList != null && localAppInfoList.size() > 0
                && appUpdateInfoList != null && appUpdateInfoList.size() > 0) {
            for (int i = 0; i < appUpdateInfoList.size(); i++) {
                int updateType = appUpdateInfoList.get(i).getUpdateType();
                if (updateType == 1) {//需要静默升级类型
                    for (int j = 0; j < localAppInfoList.size(); j++) {
                        final String localPackageName = localAppInfoList.get(j).packageName;
                        final String serverPackageName = appUpdateInfoList.get(i).getAppPackage();
                        if (!TextUtils.isEmpty(localPackageName) && !TextUtils.isEmpty(serverPackageName)) {
                            if (localPackageName.equals(serverPackageName)) {//包名一样
                                final String localAppVersionCode = localAppInfoList.get(j).appVersionCode;
                                final String serverVerCode = appUpdateInfoList.get(i).getVerCode();
                                if (!TextUtils.isEmpty(localAppVersionCode) && !TextUtils.isEmpty(serverVerCode)) {
                                    final int localVersionCode = Integer.valueOf(localAppVersionCode);
                                    final int serverVersionCode = Integer.valueOf(serverVerCode);
                                    if (serverVersionCode > localVersionCode) {
                                        infos.add(appUpdateInfoList.get(i));
                                    }
                                }
                            }
                        }
                    }
                }
            }
            return infos;
        }
        return null;
    }
}
