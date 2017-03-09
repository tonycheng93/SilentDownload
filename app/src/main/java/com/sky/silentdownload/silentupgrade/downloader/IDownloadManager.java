package com.sky.silentdownload.silentupgrade.downloader;


import com.sky.silentdownload.silentupgrade.downloader.data.DownloadInfo;

import java.util.List;

/**
 * Created by BaoCheng on 2017/2/18.
 */

public interface IDownloadManager {

    /**
     * 下载指定 apk
     */
    void download(DownloadInfo downloadInfo);

    /**
     * 删除指定 apk
     */
    void remove(DownloadInfo downloadInfo);

    /**
     * 取出所有下载任务
     */
    List<DownloadInfo> list();

    /**
     * 设置下载监听
     */
    void setDownloadManagerListener(IDownloadManagerListener listener);

    /**
     * 取消下载监听
     */
    void unSetDownloadManagerListener(IDownloadManagerListener listener);

    /**
     * 获取 下载 apk 本地路径
     */
    String getNativePath(DownloadInfo downloadInfo);

    /**
     * 获取指定下载任务状态
     */
    int getDownloadStatus(DownloadInfo downloadInfo);
}
