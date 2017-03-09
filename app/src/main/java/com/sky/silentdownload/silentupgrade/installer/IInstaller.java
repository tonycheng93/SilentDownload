package com.sky.silentdownload.silentupgrade.installer;

/**
 * Created by BaoCheng on 2017/3/6.
 */

public interface IInstaller {

    void install(String apkPath);//静默安装

    /**
     * 注册静默安装监听
     */
    void setInstallListener(IInstallerListener listener);

    /**
     * 反注册静默安装监听
     */
    void unsetInstallListener(IInstallerListener listener);
}
