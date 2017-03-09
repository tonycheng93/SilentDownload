package com.sky.silentdownload.silentupgradecontroller;

import android.content.Context;
import android.test.AndroidTestCase;

import com.sky.silentdownload.silentupgrade.model.data.ApkInfo;
import com.sky.silentdownload.silentupgrade.model.data.AppUpdateInfo;
import com.sky.silentdownload.silentupgrade.model.data.LocalAppInfo;
import com.sky.silentdownload.silentupgrade.model.impl.GetLocalAppInfoImpl;
import com.sky.silentdownload.silentupgrade.model.impl.GetSilentUpgradeDataImpl;

import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by BaoCheng on 2017/3/9.
 */

public class GetSilentUpgradeDataImplTest extends AndroidTestCase {

    private List<LocalAppInfo> mLocalAppInfoList = null;

    private List<AppUpdateInfo> mAppUpdateInfoList = null;

    private GetSilentUpgradeDataImpl mGetSilentUpgradeData = null;

    private GetLocalAppInfoImpl mGetLocalAppInfo = null;

    private Context mContext = null;

    private String pkgName = "mb.gamenet";

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        mContext = getContext();

        mGetSilentUpgradeData = GetSilentUpgradeDataImpl.getInstance(mContext);
        mGetLocalAppInfo = Mockito.mock(GetLocalAppInfoImpl.class);

        mLocalAppInfoList = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            LocalAppInfo localAppInfo = new LocalAppInfo();
            localAppInfo.packageName = "com.example.test" + i;
            localAppInfo.appVersionCode = String.valueOf(i);
            mLocalAppInfoList.add(localAppInfo);
        }

        mAppUpdateInfoList = new ArrayList<>();
        AppUpdateInfo appUpdateInfo1 = new AppUpdateInfo();
        appUpdateInfo1.setAppPackage("com.example.test0");
        appUpdateInfo1.setVerCode("2");
        appUpdateInfo1.setUpdateType(1);
        mAppUpdateInfoList.add(appUpdateInfo1);
        AppUpdateInfo appUpdateInfo2 = new AppUpdateInfo();
        appUpdateInfo2.setAppPackage("com.example.test1");
        appUpdateInfo2.setVerCode("2");
        appUpdateInfo2.setUpdateType(1);
        mAppUpdateInfoList.add(appUpdateInfo2);
        AppUpdateInfo appUpdateInfo3 = new AppUpdateInfo();
        appUpdateInfo3.setAppPackage("com.example.test2");
        appUpdateInfo3.setVerCode("4");
        appUpdateInfo3.setUpdateType(1);
        mAppUpdateInfoList.add(appUpdateInfo3);
        AppUpdateInfo appUpdateInfo4 = new AppUpdateInfo();
        appUpdateInfo4.setAppPackage("com.example.test3");
        appUpdateInfo4.setVerCode("2");
        appUpdateInfo4.setUpdateType(1);
        mAppUpdateInfoList.add(appUpdateInfo4);

    }

    /**
     * 测试过滤 app 是否正确
     */
    @Test
    public void testFilterApp() {
        List<AppUpdateInfo> appUpdateInfos = mGetSilentUpgradeData.filterAppInfo(mLocalAppInfoList, mAppUpdateInfoList);
        assertEquals(appUpdateInfos.size(), 3);
    }

    /**
     * 测试根据包名获取apk信息
     */
    @Test
    public void testLoadApkInfo() throws InterruptedException {
        final ApkInfo apkInfo = mGetSilentUpgradeData.loadApkInfo(pkgName);
        Thread.sleep(60 * 1000);
        assertNotNull(apkInfo);
    }

    /**
     * 测试获取需要升级app列表
     */
    @Test
    public void testLoadAppUpdateInfo() throws InterruptedException {
        List<LocalAppInfo> localAppInfoList = new ArrayList<>();
        LocalAppInfo localAppInfo1 = new LocalAppInfo();
        localAppInfo1.packageName = "cn.jj.tv";
        localAppInfo1.appVersionCode = "40507";
        localAppInfoList.add(localAppInfo1);

        LocalAppInfo localAppInfo2 = new LocalAppInfo();
        localAppInfo2.packageName = "com.trans.mermaid";
        localAppInfo2.appVersionCode = "53";

        LocalAppInfo localAppInfo3 = new LocalAppInfo();
        localAppInfo3.packageName = "com.wandoujia.phoenix2";
        localAppInfo3.appVersionCode = "13150";
        localAppInfoList.add(localAppInfo2);

        GetLocalAppInfoImpl mockGetLocalAppInfo = Mockito.mock(GetLocalAppInfoImpl.class);
        Mockito.when(mockGetLocalAppInfo.getLocalAppInfo()).thenReturn(localAppInfoList);
        final List<AppUpdateInfo> appUpdateInfos = mGetSilentUpgradeData.loadAppUpdateInfo();
        Thread.sleep(60 * 1000);
        assertEquals(1,appUpdateInfos.size());
    }
}
