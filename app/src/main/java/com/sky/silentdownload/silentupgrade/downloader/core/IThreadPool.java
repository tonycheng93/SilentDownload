package com.sky.silentdownload.silentupgrade.downloader.core;


import com.sky.silentdownload.silentupgrade.downloader.core.impl.DownloaderImpl;
import com.sky.silentdownload.silentupgrade.downloader.data.DownloadTaskInfo;

/**
 * Created by BaoCheng on 2017/2/22.
 */

public interface IThreadPool {


    /**
     * 创建一个 DownloaderImpl
     */
    DownloaderImpl create(DownloadTaskInfo downloadTaskInfo);

    /**
     * 删除 DownloaderImpl
     */
    void remove(DownloadTaskInfo downloadTaskInfo);
}
