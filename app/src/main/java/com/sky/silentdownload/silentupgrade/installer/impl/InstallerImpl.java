package com.sky.silentdownload.silentupgrade.installer.impl;

import android.content.Context;

import com.sky.silentdownload.silentupgrade.installer.IInstaller;
import com.sky.silentdownload.silentupgrade.installer.IInstallerListener;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Created by BaoCheng on 2017/3/6.
 */

public class InstallerImpl implements IInstaller {

    private Context mContext;
    private Executor executor = Executors.newSingleThreadExecutor();
//    private SilentInstallerHandler sl = null;

    public InstallerImpl(Context context) {
        mContext = context;
//        sl = new SilentInstallerHandler(mContext);

    }

    @Override
    public void install(final String apkPath) {
//        if(sl == null)
//            sl = new SilentInstallerHandler(mContext);
//        executor.execute(new Runnable() {
//            @Override
//            public void run() {
//                sl.install(apkPath);
//            }
//        });
    }

    @Override
    public void setInstallListener(IInstallerListener listener) {
//        if(sl == null)
//            sl = new SilentInstallerHandler(mContext);
//        sl.setInstallerListener(listener);
    }

    @Override
    public void unsetInstallListener(IInstallerListener listener) {
    }
}
