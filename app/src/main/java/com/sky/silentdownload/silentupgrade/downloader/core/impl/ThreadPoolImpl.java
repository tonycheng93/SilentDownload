package com.sky.silentdownload.silentupgrade.downloader.core.impl;

/**
 * Created by BaoCheng on 2017/2/20.
 */

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.sky.silentdownload.silentupgrade.downloader.IDownloadTaskManagerListener;
import com.sky.silentdownload.silentupgrade.downloader.data.DownloadTaskInfo;
import com.sky.silentdownload.silentupgrade.downloader.data.Status;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 线程池
 */
public class ThreadPoolImpl implements IDownloadTaskManagerListener {

    private static final int TASK_SIZE = 2;
    private static ThreadPoolImpl instance;
    private Context mContext;
    private IDownloadTaskManagerListener downloadManagerListener;
    private ThreadPoolExecutor executor = null;
    private BlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>();
    private Map<String, DownloaderImpl> downloaderMap = new HashMap<String, DownloaderImpl>();
    private Map<String, DownloaderImpl> removeDownloaderMap = new HashMap<String, DownloaderImpl>();
    private Map<String, DownloaderImpl> executorMap = new HashMap<String, DownloaderImpl>();

    public ThreadPoolImpl(Context context, IDownloadTaskManagerListener listener, List<DownloadTaskInfo> taskInfoList) {
        mContext = context;
        downloadManagerListener = listener;
        executor = new ThreadPoolExecutor(TASK_SIZE, TASK_SIZE, 999, TimeUnit.DAYS, queue,
                new ThreadFactory() {
                    @Override
                    public Thread newThread(Runnable r) {
                        // TODO Auto-generated method stub
                        return new Thread(r, "DownloadThread" + System.currentTimeMillis());
                    }
                });
        executor.prestartAllCoreThreads();

        if(taskInfoList != null) {
            Log.i("threadPool", "taskInfoList size: "+taskInfoList.size());
            for (DownloadTaskInfo taskInfo: taskInfoList) {
                getDownloaderFromMap(taskInfo);
            }
        }
    }

    public static ThreadPoolImpl getInstance(Context context, IDownloadTaskManagerListener listener, List<DownloadTaskInfo> taskInfoList) {
        if (instance == null) {
            instance = new ThreadPoolImpl(context, listener, taskInfoList);
        }
        return instance;
    }

    private synchronized DownloaderImpl getDownloaderFromMap(DownloadTaskInfo taskInfo) {
        if (!downloaderMap.containsKey(taskInfo.downloadInfo.url)) {
            taskInfo.state = Status.START;
            DownloaderImpl downloaderImpl = new DownloaderImpl(mContext, taskInfo);
            //设置回调
            downloaderImpl.setDownloadTaskManagerListener(this);
            downloaderMap.put(taskInfo.downloadInfo.url, downloaderImpl);
            return downloaderImpl;
        }
        return downloaderMap.get(taskInfo.downloadInfo.url);
    }

    public synchronized boolean isDownloadRemove(DownloadTaskInfo taskInfo) {
        if (taskInfo == null || taskInfo.downloadInfo == null || taskInfo.downloadInfo.url == null)
            return false;
        return removeDownloaderMap.containsKey(taskInfo.downloadInfo.url)?true:false;
    }

    public synchronized void createDownloadTask(DownloadTaskInfo taskInfo) {
        if (taskInfo == null || taskInfo.downloadInfo == null
                || TextUtils.isEmpty(taskInfo.downloadInfo.url)
                || TextUtils.isEmpty(taskInfo.downloadInfo.md5)) {
            return;
        }

        //重新下载删除中的任务
        if (removeDownloaderMap.containsKey(taskInfo.downloadInfo.url)) {
            return;
        }

        if (executorMap.containsKey(taskInfo.downloadInfo.url)) {
            Log.i("threadPool", "current downloaderRunning: "+taskInfo.downloadInfo.url);
            return;
        }
        DownloaderImpl downloaderImpl = getDownloaderFromMap(taskInfo);

        synchronized (queue) {
            Log.i("threadPool", "queue size: "+queue.size());
            for (Runnable runnable : queue) {
                DownloadTaskInfo downloadTaskInfo = ((DownloaderImpl) runnable).getDownloadTaskInfo();
                if (downloadTaskInfo != null && downloadTaskInfo.downloadInfo != null
                        && downloadTaskInfo.downloadInfo.url.equals(taskInfo.downloadInfo.url)) {
                    return;
                }
            }

            executor.execute(downloaderImpl);
            executorMap.put(taskInfo.downloadInfo.url, downloaderImpl);
        }

        Log.i("threadPool", "create getTaskCount: "+executor.getTaskCount()+", Active: "+executor.getActiveCount());
    }

    public synchronized DownloaderImpl getDownloader(DownloadTaskInfo taskInfo) {
        if (taskInfo == null || taskInfo.downloadInfo == null
                || TextUtils.isEmpty(taskInfo.downloadInfo.url)
                || TextUtils.isEmpty(taskInfo.downloadInfo.md5)) {
            return null;
        }

        if (downloaderMap.containsKey(taskInfo.downloadInfo.url)) {
            return downloaderMap.get(taskInfo.downloadInfo.url);
        } else {
            return getDownloaderFromMap(taskInfo);
        }
    }

    public synchronized void removeDownloader(DownloadTaskInfo taskInfo) {
        Log.i("threadPool", "removeDownloader");
        if (taskInfo == null || taskInfo.downloadInfo == null
                || TextUtils.isEmpty(taskInfo.downloadInfo.url)
                || TextUtils.isEmpty(taskInfo.downloadInfo.md5)) {
            Log.i("threadPool", "removeDownloader null");
            return;
        }

        if (removeDownloaderMap.containsKey(taskInfo.downloadInfo.url)) {
            Log.i("threadPool", "removeDownloader containsKey");
            return;
        }

        if (downloaderMap.containsKey(taskInfo.downloadInfo.url)) {
            DownloaderImpl downloaderImpl = downloaderMap.get(taskInfo.downloadInfo.url);
            removeDownloaderMap.put(taskInfo.downloadInfo.url, downloaderImpl);
            downloaderImpl.onCancel();
            return;
        }
    }

    @Override
    public void onDownloadStatus(int status, DownloadTaskInfo downloadTaskInfo, String extra) {
        if ((status == Status.REMOVE || status == Status.ERROR || status == Status.ERROR_SPACE
                || status == Status.FINISH) && downloadTaskInfo != null) {
            synchronized (this) {
                if (downloaderMap.containsKey(downloadTaskInfo.downloadInfo.url)) {
                    DownloaderImpl downloaderImpl = downloaderMap.get(downloadTaskInfo.downloadInfo.url);
                    queue.remove(downloaderImpl);
                    removeDownloaderMap.remove(downloadTaskInfo.downloadInfo.url);
                    downloaderMap.remove(downloaderImpl);
                    executorMap.remove(downloadTaskInfo.downloadInfo.url);
                }
            }
            Log.i("threadPool", "onDownloadStatus getTaskCount: "+executor.getTaskCount()+", Active: "+executor.getActiveCount());
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(2);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    Log.i("threadPool", "onDownloadStatus thread getTaskCount: "+executor.getTaskCount()+", Active: "+executor.getActiveCount());
                }
            });
        }

        if (downloadManagerListener != null) {
            downloadManagerListener.onDownloadStatus(status, downloadTaskInfo, extra);
        }
    }
}
