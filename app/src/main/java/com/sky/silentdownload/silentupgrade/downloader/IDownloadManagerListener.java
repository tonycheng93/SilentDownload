package com.sky.silentdownload.silentupgrade.downloader;


import com.sky.silentdownload.silentupgrade.downloader.data.DownloadInfo;

/**
 * Created by BaoCheng on 2017/2/18.
 */

public interface IDownloadManagerListener {

    void onDownloadStatus(int status, DownloadInfo downloadInfo, String extra);
}
