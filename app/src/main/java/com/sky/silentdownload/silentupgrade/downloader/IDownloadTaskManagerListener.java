package com.sky.silentdownload.silentupgrade.downloader;


import com.sky.silentdownload.silentupgrade.downloader.data.DownloadTaskInfo;

/**
 * Created by xfk on 2017/2/23.
 */

public interface IDownloadTaskManagerListener {
    void onDownloadStatus(int status, DownloadTaskInfo downloadInfo, String extra);
}
