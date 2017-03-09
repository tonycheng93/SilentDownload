package com.sky.silentdownload.silentupgrade.downloader.core;


import com.sky.silentdownload.silentupgrade.downloader.data.DownloadTaskInfo;

import java.util.List;

/**
 * Created by lu on 17-2-22.
 */

public interface IDB {
    boolean update(DownloadTaskInfo info);

    DownloadTaskInfo read(String file);

    List<DownloadTaskInfo> list();

    boolean remove(DownloadTaskInfo info);
}
