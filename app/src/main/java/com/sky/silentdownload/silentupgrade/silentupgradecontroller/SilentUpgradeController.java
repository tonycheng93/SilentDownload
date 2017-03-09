package com.sky.silentdownload.silentupgrade.silentupgradecontroller;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.text.TextUtils;
import android.util.Log;

import com.sky.silentdownload.silentupgrade.IGetSilentUpgradeData;
import com.sky.silentdownload.silentupgrade.downloader.DownloadManager;
import com.sky.silentdownload.silentupgrade.downloader.IDownloadManager;
import com.sky.silentdownload.silentupgrade.downloader.IDownloadManagerListener;
import com.sky.silentdownload.silentupgrade.downloader.data.ApkDownloadInfo;
import com.sky.silentdownload.silentupgrade.downloader.data.DownloadInfo;
import com.sky.silentdownload.silentupgrade.downloader.data.Status;
import com.sky.silentdownload.silentupgrade.installer.IInstaller;
import com.sky.silentdownload.silentupgrade.installer.IInstallerListener;
import com.sky.silentdownload.silentupgrade.installer.impl.InstallerImpl;
import com.sky.silentdownload.silentupgrade.utils.FirstStart;
import com.sky.silentdownload.silentupgrade.utils.SharePreferencesUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

import static com.sky.silentdownload.silentupgrade.downloader.data.Status.ERROR;
import static com.sky.silentdownload.silentupgrade.downloader.data.Status.FINISH;


/**
 * Created by BaoCheng on 2017/3/6.
 */

public class SilentUpgradeController implements ISilentUpgradeController, IDownloadManagerListener, IInstallerListener, IGetSilentUpgradeData {

    private static final String TAG = "SilentUpgradeController";

    private static final String FLAG_IS_FIRST_START = "isFirstStart";
    private static final String PREF_NAME = "com.coocaa.x.app.appstore3.stub.silentupgrade.silentupgradecontroller.silentupgradecontroller";
    private static final String FLAG_REQUEST_TIME_STAMP = "last_time_request";
    private static final long REQUEST_CYCLE = 8 * 60 * 60 * 1000;

    private IDownloadManager mDownloadManager = null;
    private IInstaller mInstaller = null;
    private Context mContext = null;
    private boolean isFirstStart;
    private boolean isRegisterListener = false;
    public long tempTime = 0;

    /*=================测试用====================*/
    public void setDownloadManager(DownloadManager downloadManager) {
        mDownloadManager = downloadManager;
    }

    public void setInstaller(InstallerImpl installer) {
        mInstaller = installer;
    }

    /*=================测试用====================*/

    /**
     * 网络状态广播监听
     */
    private BroadcastReceiver mNetworkBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            ConnectivityManager connectivityManager =
                    (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            if (networkInfo != null && networkInfo.isAvailable()) {
                onNetworkConnect();
            }
        }
    };

    /**
     * Default constructor
     */
    public SilentUpgradeController() {

    }

    @Override
    public void init(final Context context) {
        mContext = context;
        Observable observable = Observable.create(new Observable.OnSubscribe<Objects>() {
            @Override
            public void call(Subscriber<? super Objects> subscriber) {
                if (mDownloadManager == null) {
                    mDownloadManager = DownloadManager.getInstance(context);
                }

                if (mInstaller == null) {
                    mInstaller = new InstallerImpl(mContext);
                }
                subscriber.onNext(null);
            }
        });

        Action1 action = new Action1() {
            @Override
            public void call(Object o) {
                registerListener();//注册静默下载、静默安装监听
            }
        };
        observable.observeOn(Schedulers.newThread()).subscribe(action);

        //判断当前版本是否第一次启动
        isFirstStart = FirstStart.isFirstStart(mContext, FLAG_IS_FIRST_START, FirstStart.ACTION.FIRST_VERSION);
        if (isFirstStart) {
            requestSilentUpgradeDataFirst();
            FirstStart.markFirstStartFlag(mContext, FLAG_IS_FIRST_START);
        }

        long lastRequestTimeMillis = SharePreferencesUtil.get(mContext, PREF_NAME, FLAG_REQUEST_TIME_STAMP, 0);
        long currentTimeMillis = System.currentTimeMillis();
        long time = calculateTime(lastRequestTimeMillis, currentTimeMillis);

        //距离上次请求静默升级数据，少于8小时
        if (time > 0 && time < REQUEST_CYCLE) {
            tempTime = REQUEST_CYCLE - time;
            try {
                List<DownloadInfo> downloadTasks = requestSilentDownloadTask();
                if (downloadTasks != null && downloadTasks.size() > 0) {
                    for (DownloadInfo downloadInfo : downloadTasks) {
                        if (mDownloadManager != null) {
                            int downloadStatus = mDownloadManager.getDownloadStatus(downloadInfo);
                            if (downloadStatus == Status.PAUSE) {
                                mDownloadManager.download(downloadInfo);
                            } else if (downloadStatus == FINISH) {
                                String apkPath = mDownloadManager.getNativePath(downloadInfo);
                                if (!TextUtils.isEmpty(apkPath)) {
                                    installPackage(apkPath);
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (time >= REQUEST_CYCLE) {//距离上次请求大于等于8小时
            startDownloadAfterGetUpgradeData();
        }

        initTimer();

    }

    /**
     * 计算距上次请求静默升级数据时间差
     *
     * @param lastRequestTimeMillis 上次请求静默升级数据时间
     * @param currentTimeMillis     当前时间
     */
    public long calculateTime(long lastRequestTimeMillis, long currentTimeMillis) {
        return currentTimeMillis - lastRequestTimeMillis;
    }

    /**
     * 初始化计时器
     */
    private void initTimer() {
        Observable.interval(tempTime, TimeUnit.HOURS)
                .observeOn(Schedulers.newThread())
                .subscribe(new Action1<Long>() {
                    @Override
                    public void call(Long aLong) {
                        startDownloadAfterGetUpgradeData();
                    }
                });
    }

    /**
     * 请求静默升级数据之后，开始静默下载
     */
    public void startDownloadAfterGetUpgradeData() {
        try {
            //请求静默升级数据
            List<DownloadInfo> downloadInfos = requestSilentUpgradeData();
            tempTime = REQUEST_CYCLE;
            //记录下此次请求静默升级数据时间
            long currentTimeMillis = System.currentTimeMillis();
            SharePreferencesUtil.put(mContext, PREF_NAME, FLAG_REQUEST_TIME_STAMP, currentTimeMillis);

            if (downloadInfos != null && downloadInfos.size() > 0) {//静默升级数据不为空

                List<DownloadInfo> downloadTasks = requestSilentDownloadTask();
                if (downloadTasks == null) {//静默升级数据不为空,静默下载任务为空，则创建静默下载任务
                    if (mDownloadManager != null) {
                        for (DownloadInfo downloadInfo : downloadInfos) {
                            mDownloadManager.download(downloadInfo);//开始静默下载
                        }
                    }
                } else {//静默升级数据不为空,静默下载任务也不为空
                    for (DownloadInfo downloadInfo : downloadInfos) {
                        String packageName = ApkDownloadInfo.getApkFromDownloadInfo(downloadInfo);//获取应用包名
                        DownloadInfo downloadTask = getDownloadInfoByPackageName(packageName);
                        if (downloadTask != null) {
                            String upgradeUrl = downloadInfo.url;
                            String downloadUrl = downloadTask.url;
                            if (upgradeUrl.equalsIgnoreCase(downloadUrl)) {//对应应用下载url相同
                                if (mDownloadManager != null) {
                                    int downloadStatus = mDownloadManager.getDownloadStatus(downloadInfo);
                                    if (downloadStatus == Status.PAUSE) {
                                        mDownloadManager.download(downloadInfo);
                                    } else if (downloadStatus == FINISH) {
                                        String apkPath = mDownloadManager.getNativePath(downloadInfo);
                                        if (!TextUtils.isEmpty(apkPath)) {
                                            installPackage(apkPath);
                                        }
                                    }
                                }
                            } else {//对应应用下载url不相同
                                removeSilentDownloadTask(downloadTask);//删除该应用原有静默下载
                                mDownloadManager.download(downloadInfo);//创建该应用新的静默下载任务并开始下载
                            }
                        } else {//当前包名对应的下载任务不存在，则创建静默下载任务
                            mDownloadManager.download(downloadInfo);
                        }
                    }
                }
            } else {
                try {
                    List<DownloadInfo> downloadTasks = requestSilentDownloadTask();
                    if (downloadTasks == null) {//静默升级数据为空并且静默下载任务也为空，则直接返回
                        return;
                    } else {//静默升级数据为空并且静默下载任务不为空，则删除所有静默下载任务
                        if (mDownloadManager != null) {
                            for (DownloadInfo downloadInfo : downloadTasks) {
                                mDownloadManager.remove(downloadInfo);
                            }
                        } else {
                            Log.i(TAG, "init: mDownloader == null");
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 该方法只会此版本应用第一次启动的时候调用， 此版本应用安装第一次启动，直接请求静默升级数据
     */
    private void requestSilentUpgradeDataFirst() {
        long currentTimeMillis = System.currentTimeMillis();
        SharePreferencesUtil.put(mContext, PREF_NAME, FLAG_REQUEST_TIME_STAMP, currentTimeMillis);
        //请求静默升级数据
        try {
            List<DownloadInfo> downloadInfos = requestSilentUpgradeData();
            if (downloadInfos != null && downloadInfos.size() <= 0) {
                if (mDownloadManager != null) {
                    for (DownloadInfo downloadInfo : downloadInfos) {
                        mDownloadManager.download(downloadInfo);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 请求静默升级数据
     *
     * @return List<SilentUpgradeData>
     */
    public List<DownloadInfo> requestSilentUpgradeData() {
        List<DownloadInfo> downloadInfoList = null;
        try {
            downloadInfoList = getSilentUpgradeData();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return downloadInfoList;
    }

    /**
     * 获取静默下载任务
     */
    public List<DownloadInfo> requestSilentDownloadTask() {
        List<DownloadInfo> downloadInfoList = null;
        if (mDownloadManager != null) {
            try {
                downloadInfoList = mDownloadManager.list();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return downloadInfoList;
    }

    /**
     * 根据包名获取静默下载任务
     */
    public DownloadInfo getDownloadInfoByPackageName(String packageName) {
        if (mDownloadManager == null || TextUtils.isEmpty(packageName)) {
            return null;
        }

        List<DownloadInfo> downloadInfos = mDownloadManager.list();
        if (downloadInfos == null || downloadInfos.size() <= 0) {
            return null;
        }

        for (DownloadInfo info : downloadInfos) {
            if (ApkDownloadInfo.getApkFromDownloadInfo(info) != null &&
                    ApkDownloadInfo.getApkFromDownloadInfo(info).equals(packageName)) {
                return info;
            }
        }
        return null;
    }

    /**
     * 删除静默下载任务
     */
    public void removeSilentDownloadTask(DownloadInfo downloadInfo) {
        try {
            if (mDownloadManager != null && downloadInfo != null) {
                mDownloadManager.remove(downloadInfo);
            } else {
                Log.i(TAG, "removeSilentDownloadTask: mDownloadManager == null || downloadInfo == null");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 开始静默下载
     */
    public void startSilentDownload() {
        if (mDownloadManager == null) return;

        List<DownloadInfo> downloadInfos = mDownloadManager.list();
        if (downloadInfos == null || downloadInfos.size() <= 0) return;

        for (DownloadInfo downloadInfo : downloadInfos) {
            int status;
            try {
                status = mDownloadManager.getDownloadStatus(downloadInfo);
                if (status != FINISH) {
                    mDownloadManager.download(downloadInfo);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 静默安装
     *
     * @param apkPath String
     */
    public void installPackage(String apkPath) {
        try {
            if (mInstaller != null) {
                mInstaller.install(apkPath);
            } else {
                Log.i(TAG, "installPackage: mInstaller == null || apkPath == null");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 当网络恢复时回调，通知下载器断点续传
     */
    private void onNetworkConnect() {
        startSilentDownload();
    }

    /**
     * 注册监听
     */
    private void registerListener() {
        if (isRegisterListener) return;

        isRegisterListener = true;

        //静默下载广播
        if (mDownloadManager != null) {
            mDownloadManager.setDownloadManagerListener(this);
        } else {
            Log.i(TAG, "registerListener: mDownloader == null");
        }

        //静默安装广播
        if (mInstaller != null) {
            mInstaller.setInstallListener(this);
        } else {
            Log.i(TAG, "registerListener: mInstaller == null");
        }

        //网络监听
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        if (mContext != null) {
            mContext.registerReceiver(mNetworkBroadcastReceiver, intentFilter);
        } else {
            Log.i(TAG, "registerListener: mContext == null");
        }

    }

    /**
     * 从服务器获取静默升级数据
     *
     * @return List<DownloadInfo>
     */
    @Override
    public List<DownloadInfo> getSilentUpgradeData() {
        ApkDownloadInfo info = new ApkDownloadInfo();
        List<DownloadInfo> infoList = new ArrayList<>();
        infoList.add(info);
        return infoList;
    }

    @Override
    public void onDownloadStatus(int status, DownloadInfo downloadInfo, String extra) {
        Log.i(TAG, "onDownloadStatus: " + status);

        if (downloadInfo == null) {
            Log.i(TAG, "onDownloadStatus: downloadInfo == null");
            return;
        }
        switch (status) {
            case ERROR:
                try {
                    if (mDownloadManager != null) {
                        mDownloadManager.remove(downloadInfo);
                    } else {
                        Log.i(TAG, "onDownloadStatus: mDownloadManager == null");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case FINISH:
                try {
                    String path = "";
                    if (mDownloadManager != null) {
                        path = mDownloadManager.getNativePath(downloadInfo);
                    } else {
                        Log.i(TAG, "onDownloadStatus: mDownloadManager == null");
                    }

                    if (!TextUtils.isEmpty(path)) {
                        installPackage(path);
                    } else {
                        Log.i(TAG, "onDownloadStatus: nativePath error");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
        }
    }

    @Override
    public void onInstallListener(int result, String pkg, String extra) {
        DownloadInfo downloadInfo = null;
        if (pkg != null) {
            downloadInfo = getDownloadInfoByPackageName(pkg);
        }

        if (result == IInstallerListener.INSTALL_SUCCESS) {
            if (mDownloadManager != null && downloadInfo != null) {
                mDownloadManager.remove(downloadInfo);
            } else {
                Log.i(TAG, "onInstallListener: mDownloadManager == null");
            }
        }
    }
}
