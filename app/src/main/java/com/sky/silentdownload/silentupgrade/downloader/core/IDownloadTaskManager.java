package com.sky.silentdownload.silentupgrade.downloader.core;


import com.sky.silentdownload.silentupgrade.downloader.data.DownloadInfo;
import com.sky.silentdownload.silentupgrade.downloader.data.DownloadTaskInfo;

import java.util.List;

/**
 * Created by BaoCheng on 2017/2/22.
 */

public interface IDownloadTaskManager {
    class DownloadTaskInfoCreateException extends Exception {
        private int errorCode = 0;

        public DownloadTaskInfoCreateException(int errorCode) {
            super();
            this.errorCode = errorCode;
        }

        public int getErrorCode() {
            return errorCode;
        }
    }

    /**
     * 创建 DownloadTaskInfo
     */
    DownloadTaskInfo create(DownloadInfo downloadInfo) throws DownloadTaskInfoCreateException;

    /**
     * 获取下载任务
     */
    DownloadTaskInfo getDownloadTask(DownloadInfo downloadInfo);

    /**
     * 获取所有下载任务
     */
    List<DownloadTaskInfo> getAllDownloadTask();

    /**
     * 更新下载任务
     */
    void updateDownloadTask(DownloadTaskInfo downloadTaskInfo);

    /**
     * 删除文件
     */
    void remove(DownloadInfo downloadInfo);
}
