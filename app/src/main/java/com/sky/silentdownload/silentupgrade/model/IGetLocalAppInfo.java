package com.sky.silentdownload.silentupgrade.model;


import com.sky.silentdownload.silentupgrade.model.data.LocalAppInfo;

import java.util.List;

/**
 * Created by BaoCheng on 2017/3/9.
 */

public interface IGetLocalAppInfo {

    /**
     * 获取本地已安装应用信息
     *
     * @return List<LocalAppInfo>
     */
    List<LocalAppInfo> getLocalAppInfo();

}
