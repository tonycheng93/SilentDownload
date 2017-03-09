package com.sky.silentdownload.silentupgrade.downloader.core.impl;

import android.content.Context;
import android.text.TextUtils;

import com.sky.silentdownload.silentupgrade.downloader.core.IDB;
import com.sky.silentdownload.silentupgrade.downloader.core.IDownloadTaskManager;
import com.sky.silentdownload.silentupgrade.downloader.data.DownloadInfo;
import com.sky.silentdownload.silentupgrade.downloader.data.DownloadTaskInfo;
import com.sky.silentdownload.silentupgrade.downloader.data.Status;
import com.sky.silentdownload.silentupgrade.utils.Android;

import java.io.File;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by BaoCheng on 2017/2/22.
 */

public class DownloadTaskManagerImpl implements IDownloadTaskManager {
    private static String createSaveDir(File parent) {
        String path = parent.getAbsolutePath() + File.separator + "downloader";
        File file = new File(path);
        if (!file.exists())
            file.mkdirs();
        return file.getAbsolutePath();
    }

    static String getInternalSaveDir(Context context) {
        return createSaveDir(context.getCacheDir());
    }

    static String getExternalSaveDir(Context context) {
        File cacheDir = context.getExternalCacheDir();
        if (cacheDir == null)
            return null;
        return createSaveDir(cacheDir);
    }

    public interface DownloadTaskManagerUtils {
        long getFileLength(String url);

        File createSaveDir(Context context, long fileLength);
    }

    private List<DownloadTaskInfo> taskInfos = new ArrayList<>();
    private IDB fileDB;
    private Context mContext;
    private DownloadTaskManagerUtils utils = new DownloadTaskManagerUtils() {

        @Override
        public long getFileLength(String url) {
            try {
                URL _url = new URL(url);
                URLConnection uc = _url.openConnection();
                return uc.getContentLength();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return 0;
        }

        long getConfigSize() {
            return 0;
            //long configSize = (long) SkyGeneralProperties.getIntConfig("x_downloader_reserve_space");  //获取配置大小 单位为MB
            //return configSize * 1024 * 1024;
        }

        @Override
        public File createSaveDir(Context context, long fileLength) {
            String saveDir = getExternalSaveDir(context);
            if (!TextUtils.isEmpty(saveDir) && Android.getFreeSpace(saveDir) > fileLength)
                return new File(saveDir);
            saveDir = getInternalSaveDir(context);
            long configSize = getConfigSize();
            if (configSize > 0) {
                if (Android.getFreeSpace(saveDir) > configSize + 2 * fileLength)
                    return new File(saveDir);
            } else {
                if (Android.getFreeSpace(saveDir) > Android.getTotalSpace(saveDir) * 0.1 + 2 * fileLength)
                    return new File(saveDir);
            }
            return null;
        }
    };

    public DownloadTaskManagerImpl(Context context) {
        mContext = context;
        fileDB = new FileDBImpl(mContext);
        taskInfos.addAll(getAllDownloadTask());
    }

    @Override
    public DownloadTaskInfo create(DownloadInfo downloadInfo) throws DownloadTaskInfoCreateException {
        DownloadTaskInfo info = getDownloadTask(downloadInfo);
        if (info != null)
            return info;
        long fileLength = utils.getFileLength(downloadInfo.url);
        File saveDir = utils.createSaveDir(mContext, fileLength);
        if (saveDir == null)
            throw new DownloadTaskInfoCreateException(Status.ERROR_SPACE);
        String fileName = Android.md5s(downloadInfo.url);
        info = new DownloadTaskInfo();
        info.downloadInfo = downloadInfo;
        info.size = fileLength;
        info.savePath = saveDir.getAbsolutePath() + File.separator + fileName;
        synchronized (taskInfos) {
            taskInfos.add(info);
        }
        return info;
    }

    private DownloadTaskInfo getDownloadTaskFromMem(DownloadInfo downloadInfo) {
        synchronized (taskInfos) {
            if (downloadInfo == null || downloadInfo.url == null)
                return null;

            for (DownloadTaskInfo taskInfo : taskInfos) {
                if (taskInfo != null && taskInfo.downloadInfo != null
                        && downloadInfo.url.equals(taskInfo.downloadInfo.url))
                    return taskInfo;
            }
        }
        return null;
    }

    @Override
    public DownloadTaskInfo getDownloadTask(DownloadInfo downloadInfo) {
        return getDownloadTaskFromMem(downloadInfo);
    }

    @Override
    public List<DownloadTaskInfo> getAllDownloadTask() {
        synchronized (fileDB) {
            return fileDB.list();
        }
    }

    @Override
    public void updateDownloadTask(DownloadTaskInfo downloadTaskInfo) {
        DownloadTaskInfo taskInfo = getDownloadTask(downloadTaskInfo.downloadInfo);
        if (taskInfo != null) {
            synchronized (taskInfos) {
                taskInfos.remove(taskInfo);
                taskInfos.add(downloadTaskInfo);
            }
        }

        synchronized (fileDB) {
            fileDB.update(downloadTaskInfo);
        }
    }

    @Override
    public void remove(DownloadInfo downloadInfo) {
        DownloadTaskInfo taskInfo = getDownloadTask(downloadInfo);
        if (taskInfo != null) {
            synchronized (taskInfos) {
                taskInfos.remove(taskInfo);
            }
            synchronized (fileDB) {
                fileDB.remove(taskInfo);
            }
        }
    }
}
