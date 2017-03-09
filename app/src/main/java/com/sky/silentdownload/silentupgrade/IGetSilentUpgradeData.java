package com.sky.silentdownload.silentupgrade;


import com.sky.silentdownload.silentupgrade.downloader.data.DownloadInfo;

import java.util.List;

/**
 * Created by BaoCheng on 2017/3/6.
 */

public interface IGetSilentUpgradeData {

    /**
     * 从服务器获取静默下载数据
     *
     * @return List<DownloadInfo>
     */
    List<DownloadInfo> getSilentUpgradeData();
}
