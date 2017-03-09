package com.sky.silentdownload.silentupgrade.installer;

/**
 * Created by BaoCheng on 2017/3/6.
 */

public interface IInstallerListener {

     int INSTALL_ERROR = 1009;//安装失败
     int INSTALL_SUCCESS = 1010;//安装成功

//    /**
//     * 安装结束的结果：成功
//     */
//    String RESULT_SUCCESS = "SUCCESS";

    /**
     * 安装结束的结果：空间不足
     */
    String RESULT_FAILED_INSUFFICIENT_STORAGE = "INSTALL_FAILED_INSUFFICIENT_STORAGE";

    /**
     * 安装结束的结果：解析包失败
     */
    String RESULT_FAILED_INVALID_URI = "INSTALL_FAILED_INVALID_URI";

    /**
     * 安装结束的结果：解析包失败
     */
    String RESULT_FAILED_INVALID_APK = "INSTALL_FAILED_INVALID_APK";

    /**
     * 安装结束的结果：安装失败，systemuid应用被安装到sdcard
     */
    String RESULT_FAILED_SYSTEMUID_TO_SDCARD = "RESULT_FAILED_SYSTEMUID_TO_SDCARD";


    /**
     * 静默安装回调
     *
     * @param result 安装结果
     * @param extra  附加信息
     */
    void onInstallListener(int result, String pkg, String extra);
}
