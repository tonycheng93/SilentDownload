package com.sky.silentdownload.silentupgrade.downloader;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.sky.silentdownload.silentupgrade.downloader.core.IDownloadTaskManager;
import com.sky.silentdownload.silentupgrade.downloader.core.impl.DownloadTaskManagerImpl;
import com.sky.silentdownload.silentupgrade.downloader.core.impl.DownloaderImpl;
import com.sky.silentdownload.silentupgrade.downloader.core.impl.ThreadPoolImpl;
import com.sky.silentdownload.silentupgrade.downloader.data.DownloadInfo;
import com.sky.silentdownload.silentupgrade.downloader.data.DownloadTaskInfo;
import com.sky.silentdownload.silentupgrade.downloader.data.Status;
import com.sky.silentdownload.silentupgrade.utils.Verify;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by BaoCheng on 2017/2/18.
 */

public class DownloadManager implements IDownloadManager, IDownloadTaskManagerListener {

    private static final String TAG = "DM";
    private volatile static DownloadManager instance;
    private DownloadTaskManagerImpl mDownloadTaskManagerImpl;
    private Context mContext;
    private ThreadPoolImpl mThreadPoolImpl;
    private List<IDownloadManagerListener> downloadListeners = new ArrayList<>();//维护一个标志每一个不同的downloadManagerListener 列表

    private DownloadManager(Context context) {
        mContext = context;
        mDownloadTaskManagerImpl = new DownloadTaskManagerImpl(context);
        mThreadPoolImpl = ThreadPoolImpl.getInstance(mContext, this, mDownloadTaskManagerImpl.getAllDownloadTask());
    }

    public static DownloadManager getInstance(Context context) {
        if (null == instance) {
            synchronized (DownloadManager.class) {
                instance = new DownloadManager(context);
            }
        }
        return instance;
    }

    @Override
    public synchronized void download(DownloadInfo downloadInfo) {
        if (downloadInfo == null || TextUtils.isEmpty(downloadInfo.url)
                || TextUtils.isEmpty(downloadInfo.md5)) {
            Log.i(TAG, "download params null");

            if (downloadListeners != null && downloadListeners.size()>0) {
                synchronized (downloadListeners) {
                    for (IDownloadManagerListener downloadManagerListener: downloadListeners) {
                        if (downloadManagerListener != null) {
                            downloadManagerListener.onDownloadStatus(Status.ERROR, downloadInfo, "invalid");
                        }
                    }
                }
            }
            return;
        }

        Log.i(TAG, "download: " + downloadInfo.url);

        DownloadTaskInfo downloadTaskInfo = mDownloadTaskManagerImpl.getDownloadTask(downloadInfo);
        if (downloadTaskInfo == null) {
            //未保存有下载任务
            try {
                //创建下载任务
                downloadTaskInfo = mDownloadTaskManagerImpl.create(downloadInfo);
            } catch (IDownloadTaskManager.DownloadTaskInfoCreateException e) {
                e.printStackTrace();

                if (downloadListeners != null && downloadListeners.size()>0) {
                    synchronized (downloadListeners) {
                        if (downloadListeners != null && downloadListeners.size()>0) {
                            for (IDownloadManagerListener downloadManagerListener: downloadListeners) {
                                if (downloadManagerListener != null) {
                                    downloadManagerListener.onDownloadStatus(Status.ERROR_SPACE, downloadInfo, "");
                                }
                            }
                        }
                    }
                }

                return;
            }

            //添加下载线程到线程池
            mThreadPoolImpl.createDownloadTask(downloadTaskInfo);
        } else {
            //如果以创建有任务
            switch (downloadTaskInfo.state) {
                case Status.FINISH:
                    //如果已下载完成且文件有效
                    if (downloadTaskInfo.savePath != null && !new File(downloadTaskInfo.savePath).exists()
                            && downloadTaskInfo.downloadInfo != null
                            && Verify.checkMD5(downloadTaskInfo.downloadInfo.md5, new File(downloadTaskInfo.savePath))) {
                        if (downloadListeners != null && downloadListeners.size()>0) {

                            synchronized (downloadListeners) {
                                for (IDownloadManagerListener downloadManagerListener: downloadListeners) {
                                    if (downloadManagerListener != null) {
                                        downloadManagerListener.onDownloadStatus(Status.FINISH, downloadTaskInfo.downloadInfo, "");
                                    }
                                }
                            }
                        }
                        return;
                    }

                    //apk无效时重新下载
                    mThreadPoolImpl.createDownloadTask(downloadTaskInfo);
                    break;
                default:
                    mThreadPoolImpl.createDownloadTask(downloadTaskInfo);
                    break;
            }
        }
    }

    @Override
    public synchronized void remove(DownloadInfo downloadInfo) {
        if (downloadInfo == null || downloadInfo.url == null)
            return;

        DownloadTaskInfo downloadTaskInfo = mDownloadTaskManagerImpl.getDownloadTask(downloadInfo);
        if (downloadTaskInfo != null) {
            DownloaderImpl downloaderImpl = mThreadPoolImpl.getDownloader(downloadTaskInfo);
            if (downloaderImpl != null) {
                if (mThreadPoolImpl.isDownloadRemove(downloadTaskInfo)) {
                    return;
                } else {
                    mThreadPoolImpl.removeDownloader(downloadTaskInfo);
                }
            }
        }
    }

    @Override
    public synchronized List<DownloadInfo> list() {
        List<DownloadTaskInfo> downloadTaskInfoList = mDownloadTaskManagerImpl.getAllDownloadTask();

        if (downloadTaskInfoList != null) {
            List<DownloadInfo> downloadInfoList = new ArrayList<DownloadInfo>();
            for (DownloadTaskInfo taskInfo : downloadTaskInfoList) {
                if (taskInfo.downloadInfo != null)
                    downloadInfoList.add(taskInfo.downloadInfo);
            }

            return downloadInfoList;
        }

        return null;
    }



    @Override
    public void setDownloadManagerListener(IDownloadManagerListener listener) {
        if (listener != null && !downloadListeners.contains(listener)) {
            synchronized (downloadListeners) {
                downloadListeners.add(listener);
            }
        }

    }

    @Override
    public void unSetDownloadManagerListener(IDownloadManagerListener listener) {
        if (listener != null && downloadListeners.contains(listener)) {
            synchronized (downloadListeners) {
                downloadListeners.remove(listener);
            }
        }
    }

    @Override
    public synchronized String getNativePath(DownloadInfo downloadInfo) {
        if (downloadInfo == null || downloadInfo.url == null)
            return null;

        DownloadTaskInfo downloadTaskInfo = mDownloadTaskManagerImpl.getDownloadTask(downloadInfo);
        if (downloadTaskInfo != null && downloadTaskInfo.state == Status.FINISH
                && downloadTaskInfo.savePath != null && downloadTaskInfo.downloadInfo != null) {
            //判断文件是否存在且有效，否则返回null
            if (Verify.checkMD5(downloadTaskInfo.downloadInfo.md5, new File(downloadTaskInfo.savePath))) {
                return downloadTaskInfo.savePath;
            }
        }

        return null;
    }

    @Override
    public synchronized int getDownloadStatus(DownloadInfo downloadInfo) {
        if (downloadInfo == null || downloadInfo.url == null)
            return Status.NONE;

        DownloadTaskInfo downloadTaskInfo = mDownloadTaskManagerImpl.getDownloadTask(downloadInfo);
        if (downloadTaskInfo != null) {
            return downloadTaskInfo.state;
        }
        return 0;
    }

    @Override
    public synchronized void onDownloadStatus(int status, final DownloadTaskInfo downloadTaskInfo, final String extra) {
        if (downloadTaskInfo == null)
            return;

        Log.i(TAG, "onDownloadStatus: " + status + ", state: " + downloadTaskInfo.state);

        //下载完成，MD5校验失败删除任务，同时回调MD5校验失败
        if (status == Status.FINISH && new File(downloadTaskInfo.savePath).exists()
                && downloadTaskInfo.downloadInfo != null) {
            if (!Verify.checkMD5(downloadTaskInfo.downloadInfo.md5, new File(downloadTaskInfo.savePath))) {
                //更新下载任务
                downloadTaskInfo.state = Status.ERROR_MD5;
                mDownloadTaskManagerImpl.updateDownloadTask(downloadTaskInfo);
                mThreadPoolImpl.removeDownloader(downloadTaskInfo);

                synchronized (downloadListeners) {
                    for (IDownloadManagerListener listener: downloadListeners) {
                        if (listener != null) {
                            listener.onDownloadStatus(Status.ERROR_MD5, downloadTaskInfo.downloadInfo, extra);
                        }
                    }
                }

                return;
            }
        }

        if (status == Status.REMOVE && downloadTaskInfo.downloadInfo != null) {
            mDownloadTaskManagerImpl.remove(downloadTaskInfo.downloadInfo);

            synchronized (downloadListeners) {
                for (IDownloadManagerListener listener: downloadListeners) {
                    listener.onDownloadStatus(status, downloadTaskInfo.downloadInfo, extra);
                }
            }
            return;
        }

        //更新下载任务
        mDownloadTaskManagerImpl.updateDownloadTask(downloadTaskInfo);

        //回调下载信息
        synchronized (downloadListeners) {
            for (IDownloadManagerListener listener: downloadListeners) {
                listener.onDownloadStatus(status, downloadTaskInfo.downloadInfo, extra);
            }
        }
    }
}
