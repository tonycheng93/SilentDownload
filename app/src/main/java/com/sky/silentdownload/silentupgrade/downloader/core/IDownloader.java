package com.sky.silentdownload.silentupgrade.downloader.core;


import com.sky.silentdownload.silentupgrade.downloader.IDownloadTaskManagerListener;
import com.sky.silentdownload.silentupgrade.downloader.data.DownloadTaskInfo;

/**
 * Created by tonycheng on 2017/2/22.
 */

public interface IDownloader {

    /**
     * 开始下载
     */
    void onStart();

    /**
     * 暂停
     */
    void onPause();

    /**
     * 取消下载
     */
    void onCancel();

    /**
     * 获取 DownloadTaskInfo
     */
    DownloadTaskInfo getDownloadTaskInfo();

    /**
     * 设置监听
     */
    void setDownloadTaskManagerListener(IDownloadTaskManagerListener listener);

}
